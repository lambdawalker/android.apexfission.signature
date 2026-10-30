package io.apexfission.signature.protocol

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.Base64

/** The enum name is the protocol wire value. Suite selection belongs to the server. */
public enum class Suite { ES256_STRONGBOX, ES256_TEE }
public enum class Operation(public val wire: String) { REGISTER("register"), SIGN("sign") }

/** Immutable protocol input. nonce is copied; identities are encoded without normalization. */
public class Statement(
    public val operation: Operation,
    public val suite: Suite,
    public val challengeId: String,
    nonce: ByteArray,
    public val issuer: String,
    public val subject: String,
    public val keyId: String,
    public val documentSHA256: String = "",
) {
    private val nonceBytes = nonce.copyOf()
    init {
        require(nonceBytes.size == 32) { "Nonce must contain exactly 32 bytes" }
        require(challengeId.isNotEmpty() && issuer.isNotEmpty() && subject.isNotEmpty()) { "Challenge and identity must not be empty" }
        require(keyId.matches(DIGEST)) { "keyId must be lowercase SHA-256 hex of SPKI DER" }
        require(if (operation == Operation.REGISTER) documentSHA256.isEmpty() else documentSHA256.matches(DIGEST)) {
            "Registration has no document digest; signing requires lowercase SHA-256 hex"
        }
    }
    /** Fresh bytes: nine UTF-8 fields, each prefixed by its unsigned big-endian uint32 byte length. */
    public fun encode(): ByteArray {
        val fields = listOf("sworn-ink/v1", operation.wire, suite.name, challengeId,
            wireBase64(nonceBytes), issuer, subject, keyId, documentSHA256)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                fields.forEach { field ->
                    val value = field.toByteArray(Charsets.UTF_8)
                    output.writeInt(value.size)
                    output.write(value)
                }
            }
            bytes.toByteArray()
        }
    }
    private companion object { val DIGEST = Regex("[0-9a-f]{64}") }
}

public fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
public fun wireBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
