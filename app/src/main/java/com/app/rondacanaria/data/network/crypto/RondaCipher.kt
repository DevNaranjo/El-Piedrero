package com.app.rondacanaria.data.network.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Componente criptográfico para asegurar la comunicación entre el Host y los Clientes
 * mediante AES-256-GCM (Authenticated Encryption with Associated Data).
 *
 * Cada trama cuenta con un IV aleatorio de 12 bytes y una etiqueta de autenticación de 128 bits
 * que previene tanto la interceptación como la manipulación o inyección de tramas en la red local.
 */
object RondaCipher {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val KEY_SIZE_BITS = 256
    private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private val secureRandom = SecureRandom()

    /**
     * Genera una clave simétrica aleatoria de 256 bits codificada en Base64.
     */
    fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_SIZE_BITS, secureRandom)
        val secretKey = keyGen.generateKey()
        return base64Encode(secretKey.encoded)
    }

    /**
     * Reconstruye una [SecretKey] a partir de su representación en Base64.
     */
    fun parseKey(base64Key: String): SecretKey {
        val decoded = base64Decode(base64Key.trim())
        require(decoded.size == 32) { "La clave de seguridad debe ser de 256 bits (32 bytes)" }
        return SecretKeySpec(decoded, ALGORITHM)
    }

    /**
     * Cifra un texto plano en UTF-8 y devuelve una cadena Base64 que contiene [IV (12B) + Ciphertext + Tag (16B)].
     */
    fun encrypt(plainText: String, secretKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return base64Encode(combined)
    }

    /**
     * Descifra una cadena Base64 generada por [encrypt].
     * Lanza excepción si la trama fue manipulada o si la clave es incorrecta.
     */
    fun decrypt(cipherTextBase64: String, secretKey: SecretKey): String {
        val combined = base64Decode(cipherTextBase64.trim())
        require(combined.size >= GCM_IV_LENGTH_BYTES + 16) { "Longitud de trama cifrada insuficiente" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun base64Encode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            out.append(BASE64_CHARS[b0 shr 2])
            if (b1 != -1) {
                out.append(BASE64_CHARS[((b0 and 0x03) shl 4) or (b1 shr 4)])
                if (b2 != -1) {
                    out.append(BASE64_CHARS[((b1 and 0x0F) shl 2) or (b2 shr 6)])
                    out.append(BASE64_CHARS[b2 and 0x3F])
                } else {
                    out.append(BASE64_CHARS[(b1 and 0x0F) shl 2])
                    out.append('=')
                }
            } else {
                out.append(BASE64_CHARS[(b0 and 0x03) shl 4])
                out.append("==")
            }
        }
        return out.toString()
    }

    private fun base64Decode(str: String): ByteArray {
        val clean = str.filter { it in BASE64_CHARS || it == '=' }
        val len = clean.length
        if (len == 0 || len % 4 != 0) return ByteArray(0)
        var pad = 0
        if (len > 0 && clean[len - 1] == '=') pad++
        if (len > 1 && clean[len - 2] == '=') pad++

        val byteCount = (len * 3) / 4 - pad
        val bytes = ByteArray(byteCount)
        var byteIdx = 0
        var i = 0
        while (i < len) {
            val c0 = BASE64_CHARS.indexOf(clean[i++])
            val c1 = BASE64_CHARS.indexOf(clean[i++])
            val c2 = if (clean[i] != '=') BASE64_CHARS.indexOf(clean[i++]) else { i++; -1 }
            val c3 = if (clean[i] != '=') BASE64_CHARS.indexOf(clean[i++]) else { i++; -1 }

            if (c0 < 0 || c1 < 0) break
            bytes[byteIdx++] = ((c0 shl 2) or (c1 shr 4)).toByte()
            if (c2 != -1 && byteIdx < byteCount) {
                bytes[byteIdx++] = (((c1 and 0x0F) shl 4) or (c2 shr 2)).toByte()
                if (c3 != -1 && byteIdx < byteCount) {
                    bytes[byteIdx++] = (((c2 and 0x03) shl 6) or c3).toByte()
                }
            }
        }
        return bytes
    }
}
