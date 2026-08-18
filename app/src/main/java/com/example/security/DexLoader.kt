package com.example.security

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

/**
 * In-Memory Encrypted DEX Loader.
 * Decrypts secondary / protected bytecode payloads entirely in RAM
 * and loads them via Dalvik InMemoryDexClassLoader (API 26+) without writing
 * unencrypted bytecode to flash/disk. Memory is immediately wiped.
 */
object DexLoader {

    private const val TAG = "DexLoader"

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadEncryptedDexInMemory(
        encryptedPayload: CryptoEngine.EncryptedPayload,
        key: ByteArray,
        parentClassLoader: ClassLoader
    ): ClassLoader? {
        var decryptedBytes: ByteArray? = null
        return try {
            decryptedBytes = CryptoEngine.decryptAesGcm(encryptedPayload, key)
            NativeCore.mlock(decryptedBytes)

            val byteBuffer = ByteBuffer.wrap(decryptedBytes)
            val inMemoryLoader = InMemoryDexClassLoader(
                arrayOf(byteBuffer),
                parentClassLoader
            )
            inMemoryLoader
        } catch (e: Exception) {
            null
        } finally {
            decryptedBytes?.let {
                NativeCore.wipe(it)
                NativeCore.munlock(it)
            }
        }
    }

    /**
     * Helper to encrypt bytecode buffer with a transient DEK.
     */
    fun encryptDexPayload(
        dexBytes: ByteArray,
        key: ByteArray
    ): CryptoEngine.EncryptedPayload {
        return CryptoEngine.encryptAesGcm(dexBytes, key)
    }
}
