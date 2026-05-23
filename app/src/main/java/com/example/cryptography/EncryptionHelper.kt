package com.example.cryptography

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"

    /**
     * Derives a 256-bit AES key from a passphrase using SHA-256 for optimal, reliable cross-platform execution.
     */
    fun deriveKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hashedBytes, KEY_ALGORITHM)
    }

    /**
     * Encrypts plaintext string using AES-256-CBC with a random IV.
     * Returns a single string containing Base64(IV + Ciphertext) to make storage compact and robust.
     */
    fun encrypt(plainText: String, secretKey: SecretKeySpec): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext before Base64 encoding
            val combined = ByteArray(iv.size + cipherTextBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherTextBytes, 0, combined, iv.size, cipherTextBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts a Base64 encoded string containing Base64(IV + Ciphertext) back to a standard string.
     */
    fun decrypt(encryptedText: String, secretKey: SecretKeySpec): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size < 16) return "[Decryption Error: Malformed Payload]"

            // Extract IV (first 16 bytes)
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, 16)
            val ivSpec = IvParameterSpec(iv)

            // Extract ciphertext bytes
            val cipherTextSize = combined.size - 16
            val cipherTextBytes = ByteArray(cipherTextSize)
            System.arraycopy(combined, 16, cipherTextBytes, 0, cipherTextSize)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedBytes = cipher.doFinal(cipherTextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Error: Invalid Encryption Key/Key Mismatch]"
        }
    }

    /**
     * Convert SecretKey to a presentable hex string representation.
     */
    fun keyToHex(secretKey: SecretKeySpec): String {
        return secretKey.encoded.joinToString("") { "%02x".format(it) }
    }
}
