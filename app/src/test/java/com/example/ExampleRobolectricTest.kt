package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fort Knox", appName)
  }

  @Test
  fun `crypto engine encrypts and decrypts AES-GCM correctly`() {
    val key = CryptoEngine.generateDEK()
    val secretText = "SuperSecretPassword123!#"
    val plaintext = secretText.toByteArray(StandardCharsets.UTF_8)

    val encrypted = CryptoEngine.encryptAesGcm(plaintext, key)
    assertNotNull(encrypted.iv)
    assertEquals(12, encrypted.iv.size)
    assertTrue(encrypted.ciphertext.isNotEmpty())

    val decryptedBytes = CryptoEngine.decryptAesGcm(encrypted, key)
    val decryptedText = String(decryptedBytes, StandardCharsets.UTF_8)
    assertEquals(secretText, decryptedText)
  }
}
