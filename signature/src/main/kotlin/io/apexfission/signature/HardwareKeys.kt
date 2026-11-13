package io.apexfission.signature

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.WorkerThread
import androidx.biometric.BiometricManager
import io.apexfission.signature.protocol.Suite
import io.apexfission.signature.protocol.sha256
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

public class HardwareUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
public class InvalidatedKeyException(cause: Throwable) : Exception("Key permanently invalidated; register a fresh key", cause)
public class AuthenticationException(public val code: Int, message: String) : Exception(message)

public class HardwareKey internal constructor(
    public val alias: String,
    public val suite: Suite,
    public val keyId: String,
)

/** Blocking Keystore operations. Never silently changes a server-selected suite. */
public class HardwareKeys(context: Context) {
    private val context = context.applicationContext

    /** Advisory only: hardware generation and server attestation validation remain mandatory. */
    public fun capabilities(): List<Suite> {
        if (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) return emptyList()
        return buildList {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) add(Suite.ES256_STRONGBOX)
            add(Suite.ES256_TEE)
        }
    }

    @WorkerThread
    public fun generate(alias: String, suite: Suite, attestationNonce: ByteArray): HardwareKey = synchronized(lock) {
        val storage = storageAlias(alias)
        require(attestationNonce.size == 32) { "Attestation nonce must contain exactly 32 bytes" }
        val store = store()
        require(!store.containsAlias(storage)) { "Alias already exists; keys must never be replaced implicitly" }
        if (suite !in capabilities()) throw HardwareUnavailableException("Selected suite or strong biometric unavailable: $suite")
        try {
            val spec = KeyGenParameterSpec.Builder(storage, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .setAttestationChallenge(attestationNonce.copyOf())
                .setIsStrongBoxBacked(suite == Suite.ES256_STRONGBOX)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply { initialize(spec) }.generateKeyPair()
            open(alias, suite).also { certificateChain(it) }
        } catch (failure: Exception) {
            // Only this fresh alias can be removed: containsAlias was checked while holding the process lock.
            try { store.deleteEntry(storage) } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
            if (failure is StrongBoxUnavailableException) throw HardwareUnavailableException("Selected StrongBox suite unavailable; no fallback attempted", failure)
            throw failure
        }
    }

    @WorkerThread
    public fun open(alias: String, suite: Suite): HardwareKey = synchronized(lock) {
        val storage = storageAlias(alias)
        val store = store()
        val privateKey = store.getKey(storage, null) as? PrivateKey
            ?: throw HardwareUnavailableException("Signing key not found")
        validatePolicy(privateKey, suite)
        val leaf = store.getCertificate(storage) ?: throw HardwareUnavailableException("Key certificate missing")
        HardwareKey(alias, suite, sha256(leaf.publicKey.encoded))
    }

    @WorkerThread
    public fun certificateChain(key: HardwareKey): List<ByteArray> = synchronized(lock) {
        checkBinding(key)
        val chain = store().getCertificateChain(storageAlias(key.alias))
            ?: throw HardwareUnavailableException("Attestation chain missing")
        if (chain.size < 2) throw HardwareUnavailableException("Hardware attestation chain is incomplete")
        chain.map { it.encoded.copyOf() }
    }

    @WorkerThread
    public fun delete(alias: String): Unit = synchronized(lock) { store().deleteEntry(storageAlias(alias)) }

    internal fun signingOperation(key: HardwareKey): Signature = synchronized(lock) { try {
        checkBinding(key)
        val privateKey = store().getKey(storageAlias(key.alias), null) as PrivateKey
        Signature.getInstance("SHA256withECDSA").apply { initSign(privateKey) }
    } catch (failure: KeyPermanentlyInvalidatedException) { throw InvalidatedKeyException(failure) } }

    private fun checkBinding(key: HardwareKey) {
        val actual = open(key.alias, key.suite)
        require(actual.keyId == key.keyId) { "Key binding changed" }
    }

    private fun validatePolicy(key: PrivateKey, suite: Suite) {
        val info = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java)
        val expected = if (suite == Suite.ES256_STRONGBOX) KeyProperties.SECURITY_LEVEL_STRONGBOX else KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
        if (info.securityLevel != expected || info.keySize != 256 || info.origin != KeyProperties.ORIGIN_GENERATED ||
            info.purposes != KeyProperties.PURPOSE_SIGN || !info.digests.contentEquals(arrayOf(KeyProperties.DIGEST_SHA256)) ||
            !info.isUserAuthenticationRequired || !isPerUseAuthenticationDuration(info.userAuthenticationValidityDurationSeconds) ||
            info.userAuthenticationType != KeyProperties.AUTH_BIOMETRIC_STRONG ||
            !info.isUserAuthenticationRequirementEnforcedBySecureHardware || !info.isInvalidatedByBiometricEnrollment) {
            throw HardwareUnavailableException("Key does not satisfy selected hardware and per-use biometric policy")
        }
    }

    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    internal companion object {
        private val lock = Any()
        fun storageAlias(alias: String): String {
            require(alias.matches(Regex("[A-Za-z0-9._-]{1,80}"))) { "Alias must contain 1..80 ASCII letters, numbers, dot, underscore or hyphen" }
            return "sworn-ink.v1.$alias"
        }
    }
}

// KeyInfo documents -1 for per-use; Keystore2 reports 0 when KM_TAG_AUTH_TIMEOUT is absent.
// Both mean no positive authentication window. Server attestation validation rejects any authTimeout tag.
internal fun isPerUseAuthenticationDuration(seconds: Int): Boolean = seconds == -1 || seconds == 0
