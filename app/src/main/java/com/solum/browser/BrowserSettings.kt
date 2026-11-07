package com.solum.browser

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BrowserSettings(context: Context) {
    private val preferences = context.getSharedPreferences("browser_settings", Context.MODE_PRIVATE)

    val searchEngine: SearchEngine
        get() = SearchEngine.entries.firstOrNull { it.name == preferences.getString("search_engine", null) }
            ?: SearchEngine.GOOGLE

    fun apiKey(): String {
        val stored = preferences.getString("api_key", null) ?: return ""
        val pieces = stored.split(":")
        require(pieces.size == 2) { "저장된 API 키를 읽을 수 없습니다. 설정에서 다시 저장해 주세요." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            encryptionKey(),
            GCMParameterSpec(128, Base64.decode(pieces[0], Base64.NO_WRAP))
        )
        return String(cipher.doFinal(Base64.decode(pieces[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun save(apiKey: String, searchEngine: SearchEngine) {
        val editor = preferences.edit().putString("search_engine", searchEngine.name)
        if (apiKey.isBlank()) {
            editor.remove("api_key")
        } else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
            val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            editor.putString(
                "api_key",
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encrypted, Base64.NO_WRAP)
            )
        }
        check(editor.commit()) { "설정을 저장하지 못했습니다. 다시 시도해 주세요." }
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing != null) return existing as SecretKey
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "solum_api_key"
    }
}

enum class SearchEngine(val label: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    NAVER("Naver", "https://search.naver.com/search.naver?query=")
}
