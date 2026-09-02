package com.app.rondacanaria.data.network.crypto

import com.app.rondacanaria.domain.model.ConnectionInfo
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class RondaCipherTest {

    @Test
    fun testEncryptionDecryptionRoundTrip() {
        val base64Key = RondaCipher.generateKey()
        val secretKey = RondaCipher.parseKey(base64Key)

        val samplePayload = """{"type":"SCORE_UPDATE","senderId":"test-player","scoreUpdate":{"teamId":"TEAM_A","piedras":1}}"""
        val encrypted = RondaCipher.encrypt(samplePayload, secretKey)

        assertNotEquals(samplePayload, encrypted)
        assertTrue(encrypted.isNotBlank())

        val decrypted = RondaCipher.decrypt(encrypted, secretKey)
        assertEquals(samplePayload, decrypted)
    }

    @Test(expected = Exception::class)
    fun testDecryptionFailsWithWrongKey() {
        val key1 = RondaCipher.parseKey(RondaCipher.generateKey())
        val key2 = RondaCipher.parseKey(RondaCipher.generateKey())

        val plainText = "Mensaje confidencial de juego"
        val encrypted = RondaCipher.encrypt(plainText, key1)

        // Descifrar con una clave diferente debe fallar por tag de autenticación inválido
        RondaCipher.decrypt(encrypted, key2)
    }

    @Test(expected = Exception::class)
    fun testDecryptionFailsWhenCiphertextIsTampered() {
        val secretKey = RondaCipher.parseKey(RondaCipher.generateKey())
        val plainText = "{\"piedras\":21}"
        val encrypted = RondaCipher.encrypt(plainText, secretKey)

        // Alterar un carácter de la trama cifrada
        val tampered = if (encrypted.endsWith("A")) encrypted.dropLast(1) + "B" else encrypted.dropLast(1) + "A"

        RondaCipher.decrypt(tampered, secretKey)
    }

    @Test
    fun testConnectionInfoWithSecurityTokens() {
        val key = RondaCipher.generateKey()
        val info = ConnectionInfo(
            ip = "192.168.1.50",
            port = 8888,
            gameId = "game-123",
            hostName = "Anfitrión",
            roomToken = "tokenXYZ",
            secretKey = key
        )

        val jsonString = info.toJson()
        val restored = ConnectionInfo.fromJson(jsonString)

        assertNotNull(restored)
        assertEquals("192.168.1.50", restored?.ip)
        assertEquals("tokenXYZ", restored?.roomToken)
        assertEquals(key, restored?.secretKey)
    }
}
