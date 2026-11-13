package io.apexfission.signature.example

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import io.apexfission.signature.*
import io.apexfission.signature.protocol.*
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

/** Host must obtain these from its validated external issuer/token integration, never from email. */
public data class Account(val issuer: String, val subject: String)
public fun interface AccessTokens { public fun current(): String }
public data class ChallengeRequest(val operation: String, val suites: List<String>, val keyId: String? = null, val documentSHA256: String? = null)
public data class Challenge(val challengeId: String, val nonce: String, val suite: String, val expiresAt: Long, val statement: String? = null)
public data class RegistrationRequest(val challengeId: String, val certificateChain: List<String>, val signature: String)
public data class SignatureRequest(val challengeId: String, val document: String, val signature: String)

/** Implement with HTTPS and Authorization: Bearer <token>; JSON names match these property names. */
public interface SwornInkApi {
    public fun challenge(accessToken: String, body: ChallengeRequest): Challenge
    public fun register(accessToken: String, body: RegistrationRequest)
    public fun submitSignature(accessToken: String, body: SignatureRequest)
}

/** Source integration fixture. No fake HTTP, email flow or JWT issuance is shipped. */
public class SigningIntegration(
    private val keys: HardwareKeys,
    private val signer: BiometricSigner,
    private val api: SwornInkApi,
    private val tokens: AccessTokens,
    private val account: Account,
) {
    public class Registration internal constructor(val key: HardwareKey, internal val challenge: Challenge,
        internal val statement: Statement, internal val chain: List<String>)
    public class Signing internal constructor(val key: HardwareKey, internal val challenge: Challenge,
        internal val statement: Statement, internal val document: ByteArray)

    @WorkerThread
    public fun prepareRegistration(freshAlias: String): Registration {
        val offered = keys.capabilities()
        val challenge = api.challenge(tokens.current(), ChallengeRequest("register", offered.map { it.name }))
        val suite = selected(challenge, offered)
        val key = keys.generate(freshAlias, suite, decodeNonce(challenge))
        // If later steps fail, host decides whether to retry exact bytes or abandon/delete this alias.
        val statement = Statement(Operation.REGISTER, suite, challenge.challengeId, decodeNonce(challenge), account.issuer, account.subject, key.keyId)
        return Registration(key, challenge, statement, keys.certificateChain(key).map(::wireBase64))
    }

    @MainThread
    public fun authenticateRegistration(prepared: Registration, text: PromptText, done: (Result<RegistrationRequest>) -> Unit) {
        checkFresh(prepared.challenge)
        signer.sign(prepared.key, prepared.statement, text) { signed ->
            done(signed.map { RegistrationRequest(prepared.challenge.challengeId, prepared.chain, wireBase64(it)) })
        }
    }

    @WorkerThread
    public fun submitRegistration(request: RegistrationRequest) { api.register(tokens.current(), request) }

    @WorkerThread
    public fun prepareSigning(alias: String, suite: Suite, document: ByteArray): Signing {
        require(document.size <= 512 * 1024) { "Document exceeds protocol limit" }
        val snapshot = document.copyOf()
        val key = keys.open(alias, suite)
        val challenge = api.challenge(tokens.current(), ChallengeRequest("sign", listOf(suite.name), key.keyId, sha256(snapshot)))
        selected(challenge, listOf(suite))
        val expected = Statement(Operation.SIGN, suite, challenge.challengeId, decodeNonce(challenge), account.issuer, account.subject, key.keyId, sha256(snapshot))
        val supplied = Base64.getDecoder().decode(requireNotNull(challenge.statement) { "Server statement missing" })
        require(MessageDigest.isEqual(expected.encode(), supplied)) { "Server statement differs from locally bound document/account/key" }
        return Signing(key, challenge, expected, snapshot)
    }

    @MainThread
    public fun authenticateSigning(prepared: Signing, text: PromptText, done: (Result<SignatureRequest>) -> Unit) {
        checkFresh(prepared.challenge)
        signer.sign(prepared.key, prepared.statement, text) { signed ->
            done(signed.map { SignatureRequest(prepared.challenge.challengeId, wireBase64(prepared.document), wireBase64(it)) })
        }
    }

    @WorkerThread
    public fun submitSignature(request: SignatureRequest) { api.submitSignature(tokens.current(), request) }

    private fun selected(challenge: Challenge, offered: List<Suite>): Suite {
        checkFresh(challenge)
        val selected = Suite.valueOf(challenge.suite)
        require(selected in offered) { "Server selected an unoffered suite" }
        return selected
    }
    private fun checkFresh(challenge: Challenge) { require(challenge.expiresAt > Instant.now().epochSecond) { "Challenge expired; server remains authoritative" } }
    private fun decodeNonce(challenge: Challenge): ByteArray = Base64.getDecoder().decode(challenge.nonce).also {
        require(it.size == 32 && wireBase64(it) == challenge.nonce) { "Nonce must be canonical padded base64 of 32 bytes" }
    }
}
