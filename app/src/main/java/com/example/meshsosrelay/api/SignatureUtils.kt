package com.example.meshsosrelay.api

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignatureUtils {
    // In a production app, this would be injected or stored securely in EncryptedSharedPreferences/Keystore.
    // For this demo, it must match the backend's .env SECRET_KEY
    private const val SECRET_KEY = "beacon_hmac_secret_key_2026_iic3"

    fun computeSignature(msgId: String, originId: String, createdAt: Long, payload: String): String {
        val canonicalString = "$msgId:$originId:$createdAt:$payload"
        
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        
        val hmacBytes = mac.doFinal(canonicalString.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
}
