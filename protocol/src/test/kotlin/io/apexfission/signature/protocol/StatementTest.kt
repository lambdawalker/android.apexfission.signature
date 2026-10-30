package io.apexfission.signature.protocol

import java.io.File
import java.util.Base64
import kotlin.test.*
import org.json.JSONObject

class StatementTest {
    @Test fun matchesSharedUtf8Vector() {
        val vector = JSONObject(File(System.getProperty("vectorPath")).readText())
        val fields = vector.getJSONArray("fields")
        val statement = Statement(
            Operation.REGISTER, Suite.ES256_STRONGBOX, fields.getString(3),
            Base64.getDecoder().decode(fields.getString(4)), fields.getString(5),
            fields.getString(6), fields.getString(7)
        )
        assertEquals(vector.getString("encodedHex"), statement.encode().toHex())
    }
    @Test fun signStatementBindsDocument() {
        val a = Statement(Operation.SIGN, Suite.ES256_TEE, "c", ByteArray(32), "i", "s", "a".repeat(64), "b".repeat(64))
        assertFalse(a.encode().contentEquals(Statement(Operation.SIGN, Suite.ES256_TEE, "c", ByteArray(32), "i", "s", "a".repeat(64), "c".repeat(64)).encode()))
    }
    @Test fun rejectsInvalidDigestAndNonce() {
        assertFailsWith<IllegalArgumentException> { Statement(Operation.REGISTER, Suite.ES256_TEE, "c", ByteArray(31), "i", "s", "a".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { Statement(Operation.SIGN, Suite.ES256_TEE, "c", ByteArray(32), "i", "s", "A".repeat(64), "b".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { Statement(Operation.REGISTER, Suite.ES256_TEE, "c", ByteArray(32), "i", "s", "a".repeat(64), "b".repeat(64)) }
    }
    @Test fun defensivelyCopiesNonce() {
        val nonce = ByteArray(32)
        val statement = Statement(Operation.REGISTER, Suite.ES256_TEE, "c", nonce, "i", "s", "a".repeat(64))
        val encoded = statement.encode()
        nonce.fill(1)
        assertContentEquals(encoded, statement.encode())
    }
    @Test fun fingerprintUsesSpkiAndStandardBase64() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".toByteArray()))
        assertEquals("+/8=", wireBase64(byteArrayOf(0xfb.toByte(), 0xff.toByte())))
    }
}
