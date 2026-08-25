package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression test to ensure SQLCipher JNI requirements and R8 configuration remain intact:
 * 1. Java SQLCipher SQLiteDatabase class has the JNI field 'mNativeHandle' of type Long (J).
 * 2. ProGuard rules explicitly retain 'net.sqlcipher.**' to prevent R8 stripping/renaming.
 */
class SQLCipherJniRegressionTest {

    @Test
    fun verifySqlCipherSQLiteDatabaseHasJniNativeHandleField() {
        val clazz = net.sqlcipher.database.SQLiteDatabase::class.java
        val field = clazz.getDeclaredField("mNativeHandle")
        assertNotNull("mNativeHandle field must exist in SQLiteDatabase for JNI binding", field)
        assertEquals("mNativeHandle field must be of type Long (JNI signature 'J')", Long::class.javaPrimitiveType, field.type)
    }

    @Test
    fun verifyProguardRulesRetainSqlCipher() {
        val proguardFile = File("proguard-rules.pro")
        val fallbackFile = File("app/proguard-rules.pro")
        val targetFile = if (proguardFile.exists()) proguardFile else fallbackFile

        assertTrue("ProGuard rules file must exist", targetFile.exists())
        val content = targetFile.readText()
        assertTrue("ProGuard rules must keep net.sqlcipher.** to prevent JNI NoSuchFieldError",
            content.contains("-keep class net.sqlcipher.**"))
        assertTrue("ProGuard rules must keep net.sqlcipher.database.**",
            content.contains("-keep class net.sqlcipher.database.**"))
    }
}
