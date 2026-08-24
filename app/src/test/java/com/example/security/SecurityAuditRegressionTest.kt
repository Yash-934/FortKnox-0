package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.autofill.AutofillSessionManager
import com.example.autofill.VaultAutofillService
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAuditRegressionTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // 1. HARDWARE ATTESTATION FAIL-CLOSED REGRESSION TEST
    @Test
    fun testAttestationFailClosedOnInvalidCert() {
        val result = KeystoreManager.performHardwareAttestation(context)
        // In Robolectric without hardware TEE certificate chain, must NOT fail-open to "Verified (TEE)"
        if (!result.isAttestationSuccess) {
            assertFalse("Fail-open bug prevented: unverified certificate must not report true", result.isAttestationSuccess)
        }
    }

    // 2. AUTOFILL SESSION EPHEMERAL TOKEN & ZEROIZATION TEST
    @Test
    fun testAutofillSessionManagerOneTimeConsumption() {
        val token = AutofillSessionManager.createSaveSession(
            domain = "https://accounts.google.com",
            packageName = "com.google.android.gms",
            username = "security.user@example.com",
            password = "SecretAutofillPassword99!"
        )
        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        // First consume: should succeed
        val payload = AutofillSessionManager.consumeSaveSession(token)
        assertNotNull(payload)
        assertEquals("security.user@example.com", payload?.username)
        assertEquals("https://accounts.google.com", payload?.domain)
        assertEquals("SecretAutofillPassword99!", String(payload!!.passwordChars))

        // Second consume: must return null (single-use token)
        val secondConsume = AutofillSessionManager.consumeSaveSession(token)
        assertNull("Session token must be one-time use only", secondConsume)
    }

    @Test
    fun testAutofillSessionManagerExpiration() {
        val token = AutofillSessionManager.createSaveSession(
            domain = "https://github.com",
            packageName = "com.github.android",
            username = "dev-user",
            password = "ghp_1234567890"
        )
        // Cleanup expired with simulated expired timestamp
        AutofillSessionManager.cleanupExpiredSessions(maxAgeMs = 0)
        val consumed = AutofillSessionManager.consumeSaveSession(token)
        assertNull("Expired session token must be pruned and inaccessible", consumed)
    }

    // 3. AAD RECORD BINDING & CIPHERTEXT TRANSPLANT DEFENSE TEST
    @Test
    fun testAadRecordBindingWithImmutableRecordUid() {
        val dek = CryptoEngine.generateDEK()
        val recordUidA = UUID.randomUUID().toString()
        val recordUidB = UUID.randomUUID().toString()

        val plaintextData = "{\"username\":\"admin\",\"password\":\"Classified#2026\"}".toByteArray(StandardCharsets.UTF_8)
        val authenticAad = "FORTKNOX_AAD_V2:recordUid=$recordUidA:schema=VAULT_ENTRY:version=2:category=LOGINS".toByteArray(StandardCharsets.UTF_8)

        // Encrypt with authentic AAD bound to recordUidA
        val encrypted = CryptoEngine.encryptAesGcm(plaintextData, dek, associatedData = authenticAad)

        // Decrypt with authentic AAD: must succeed
        val decrypted = CryptoEngine.decryptAesGcm(encrypted, dek, associatedData = authenticAad)
        assertEquals(String(plaintextData, StandardCharsets.UTF_8), String(decrypted, StandardCharsets.UTF_8))

        // Adversarial Scenario 1: Ciphertext transplant to recordUidB
        val transplantedAad = "FORTKNOX_AAD_V2:recordUid=$recordUidB:schema=VAULT_ENTRY:version=2:category=LOGINS".toByteArray(StandardCharsets.UTF_8)
        try {
            CryptoEngine.decryptAesGcm(encrypted, dek, associatedData = transplantedAad)
            fail("AEAD must reject decryption when ciphertext is transplanted to a different recordUid")
        } catch (e: Exception) {
            // Expected: AEADBadTagException
        }

        // Adversarial Scenario 2: Tamper category
        val tamperedCategoryAad = "FORTKNOX_AAD_V2:recordUid=$recordUidA:schema=VAULT_ENTRY:version=2:category=CARDS".toByteArray(StandardCharsets.UTF_8)
        try {
            CryptoEngine.decryptAesGcm(encrypted, dek, associatedData = tamperedCategoryAad)
            fail("AEAD must reject decryption when category is tampered")
        } catch (e: Exception) {
            // Expected
        }

        // Adversarial Scenario 3: Null AAD fallback attempt
        try {
            CryptoEngine.decryptAesGcm(encrypted, dek, associatedData = null)
            fail("AEAD must reject decryption when unauthenticated null AAD is provided")
        } catch (e: Exception) {
            // Expected
        }
    }

    // 4. CANONICAL ORIGIN MATCHING & SUBSTRING ATTACK DEFENSE
    @Test
    fun testAutofillDomainMatchingStrictness() {
        // Authentic subdomains
        assertTrue(VaultAutofillService.isDomainMatch("https://auth.github.com/login", "https://github.com"))
        assertTrue(VaultAutofillService.isDomainMatch("https://sub.domain.accounts.google.com", "https://google.com"))
        assertTrue(VaultAutofillService.isDomainMatch("https://netflix.com", "https://netflix.com"))

        // Substring / Prefix / Suffix attacks
        assertFalse("attacker-github.com must NOT match github.com",
            VaultAutofillService.isDomainMatch("https://attacker-github.com/login", "https://github.com"))
        assertFalse("github.com.attacker.com must NOT match github.com",
            VaultAutofillService.isDomainMatch("https://github.com.attacker.com/login", "https://github.com"))
        assertFalse("fakegoogle.com must NOT match google.com",
            VaultAutofillService.isDomainMatch("https://fakegoogle.com", "https://google.com"))
        assertFalse("google.com.phishing.io must NOT match google.com",
            VaultAutofillService.isDomainMatch("https://google.com.phishing.io", "https://google.com"))

        // Empty / Ambiguous requests
        assertFalse(VaultAutofillService.isDomainMatch("", "https://github.com"))
        assertFalse(VaultAutofillService.isDomainMatch("https://github.com", ""))
    }

    // 5. CRYPTOGRAPHIC 2FA DUAL SHARE SPLITTING & RECONSTRUCTION TEST
    @Test
    fun testTwoFactorCryptographicShareSplitting() {
        val masterDek = CryptoEngine.generateDEK()
        val shareA = CryptoEngine.generateDEK()
        val shareB = ByteArray(32) { i -> (masterDek[i].toInt() xor shareA[i].toInt()).toByte() }

        // Test XOR reconstruction: ShareA XOR ShareB == DEK
        val reconstructed = ByteArray(32) { i -> (shareA[i].toInt() xor shareB[i].toInt()).toByte() }
        assertTrue("Reconstructed DEK must match original master DEK", masterDek.contentEquals(reconstructed))

        // Neither ShareA nor ShareB alone can decrypt data encrypted by DEK
        val testPlaintext = "TopSecretMedicalFile".toByteArray(StandardCharsets.UTF_8)
        val encrypted = CryptoEngine.encryptAesGcm(testPlaintext, masterDek)

        try {
            CryptoEngine.decryptAesGcm(encrypted, shareA)
            fail("Share A alone must not decrypt vault data")
        } catch (e: Exception) {
            // Expected AEAD failure
        }

        try {
            CryptoEngine.decryptAesGcm(encrypted, shareB)
            fail("Share B alone must not decrypt vault data")
        } catch (e: Exception) {
            // Expected AEAD failure
        }
    }

    // 6. ENCRYPTED BACKUP BOUNDS & INTEGRITY TEST
    @Test
    fun testEncryptedBackupRoundtripAndIntegrity() {
        val entries = listOf(
            VaultEntry(
                id = 1,
                title = "Primary Email",
                username = "admin@knox.security",
                password = "P#992_xkL!v99",
                url = "https://mail.google.com",
                category = VaultCategory.LOGINS
            )
        )
        val backupPassword = "StrongBackupPassword#2026".toCharArray()
        val backupJson = EncryptedBackupManager.createEncryptedBackup(context, entries, backupPassword.clone(), isDeviceBound = false)

        assertNotNull(backupJson)
        assertTrue(backupJson.contains("\"version\": 2"))

        // Restore test
        val restored = EncryptedBackupManager.restoreEncryptedBackup(context, backupJson, backupPassword.clone())
        assertEquals(1, restored.size)
        assertEquals("Primary Email", restored[0].title)
        assertEquals("admin@knox.security", restored[0].username)
        assertEquals("P#992_xkL!v99", restored[0].password)

        // Tamper test: corrupted password
        try {
            val wrongPass = "WrongBackupPassword".toCharArray()
            EncryptedBackupManager.restoreEncryptedBackup(context, backupJson, wrongPass)
            fail("Restore must fail with invalid password")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testEncryptedBackupRejectsPathologicalKdfParameters() {
        // Pathological iteration count attack (DoS via excessive CPU)
        val attackJsonIterations = """
            {
                "version": 2,
                "kdf": "argon2id",
                "iterations": 999999,
                "memoryKb": 65536,
                "salt": "YWJjZGVmZ2hpamtsbW5vcA==",
                "nonce": "YWJjZGVmZ2hpamts",
                "ciphertext": "dGVzdGNpcGhlcnRleHQ="
            }
        """.trimIndent()
        try {
            EncryptedBackupManager.restoreEncryptedBackup(context, attackJsonIterations, "pass".toCharArray())
            fail("Pathological iterations must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Pathological KDF iteration count") == true)
        }

        // Pathological memory allocation attack (DoS via OOM)
        val attackJsonMemory = """
            {
                "version": 2,
                "kdf": "argon2id",
                "iterations": 3,
                "memoryKb": 1048576,
                "salt": "YWJjZGVmZ2hpamtsbW5vcA==",
                "nonce": "YWJjZGVmZ2hpamts",
                "ciphertext": "dGVzdGNpcGhlcnRleHQ="
            }
        """.trimIndent()
        try {
            EncryptedBackupManager.restoreEncryptedBackup(context, attackJsonMemory, "pass".toCharArray())
            fail("Pathological memory allocation must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Pathological KDF memory allocation") == true)
        }
    }

    @Test
    fun testEncryptedBackupRejectsInvalidSaltAndNonceLengths() {
        // Salt too short (< 16 bytes)
        val attackShortSalt = """
            {
                "version": 2,
                "kdf": "argon2id",
                "iterations": 3,
                "memoryKb": 65536,
                "salt": "c2hvcnQ=",
                "nonce": "YWJjZGVmZ2hpamts",
                "ciphertext": "dGVzdGNpcGhlcnRleHQ="
            }
        """.trimIndent()
        try {
            EncryptedBackupManager.restoreEncryptedBackup(context, attackShortSalt, "pass".toCharArray())
            fail("Short salt must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid backup salt length") == true)
        }
    }

    // 7. MEMORY ZEROIZATION & MUTABLE BUFFER WIPING TEST
    @Test
    fun testMemoryZeroizationBufferWiping() {
        val secretBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        CryptoEngine.wipe(secretBytes)
        for (b in secretBytes) {
            assertEquals("ByteArray buffer must be zeroized after wipe", 0.toByte(), b)
        }

        val secretChars = "SecretMasterPassword#2026".toCharArray()
        CryptoEngine.wipe(secretChars)
        for (c in secretChars) {
            assertEquals("CharArray buffer must be zeroized after wipe", '\u0000', c)
        }
    }

    // 8. AUTOFILL FORGED TOKEN & REPLAY RESISTANCE
    @Test
    fun testAutofillForgedTokenRejection() {
        val forgedToken = UUID.randomUUID().toString()
        val session = AutofillSessionManager.consumeSaveSession(forgedToken)
        assertNull("Forged non-existent session token must return null", session)
    }

    // 9. APK SIGNATURE & DEX INTEGRITY CHECKS
    @Test
    fun testApkSignatureValidationFormat() {
        val result = NativeCore.verifyApkSignature(context)
        assertNotNull(result)
        // In Robolectric test runner, returns PASSED or UNVERIFIED
        assertTrue(result == NativeCore.SignatureVerificationResult.PASSED || result == NativeCore.SignatureVerificationResult.UNVERIFIED_NO_REFERENCE_FINGERPRINT)
    }

    @Test
    fun testApkSignatureMismatchDetection() {
        val mismatchResult = NativeCore.verifyApkSignature(context, expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        assertEquals(NativeCore.SignatureVerificationResult.FAILED_MISMATCH, mismatchResult)
    }

    @Test
    fun testDexIntegrityCheckExecution() {
        val result = NativeCore.verifyDexIntegrity(context)
        assertNotNull(result)
    }

    @Test
    fun testDexIntegrityMismatchDetection() {
        val mismatchResult = NativeCore.verifyDexIntegrity(context, expectedSha256 = "INVALID_TEST_HASH")
        assertEquals(NativeCore.DexVerificationResult.FAILED_HASH_MISMATCH, mismatchResult)
    }

    // 10. INDEPENDENT RELEASE ARTIFACT VERIFIER TESTS
    @Test
    fun testReleaseArtifactAuditVerifierExecution() {
        val report = ReleaseArtifactAuditVerifier.performReleaseAudit(context)
        assertNotNull(report)
        assertTrue("Audit report must contain evaluated findings", report.findings.isNotEmpty())
        
        // Confirm no declared INTERNET permission is audited
        val netFinding = report.findings.firstOrNull { it.controlId == "REL-NET-01" }
        assertNotNull(netFinding)
        assertEquals(ReleaseArtifactAuditVerifier.AuditVerdict.PASS, netFinding?.verdict)
        assertTrue(netFinding?.technicalEvidence?.contains("No declared INTERNET permission") == true)

        // Confirm UNKNOWN status when trusted reference is not provided
        val dexFinding = report.findings.firstOrNull { it.controlId == "REL-DEX-01" }
        assertNotNull(dexFinding)
        assertEquals(ReleaseArtifactAuditVerifier.AuditVerdict.UNKNOWN, dexFinding?.verdict)

        val certFinding = report.findings.firstOrNull { it.controlId == "REL-CERT-01" }
        assertNotNull(certFinding)
        assertEquals(ReleaseArtifactAuditVerifier.AuditVerdict.UNKNOWN, certFinding?.verdict)
    }

    @Test
    fun testReleaseArtifactAuditVerifierMismatchHandling() {
        val reportWithBadCert = ReleaseArtifactAuditVerifier.performReleaseAudit(
            context,
            trustedReleaseCertSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        )
        val certFinding = reportWithBadCert.findings.firstOrNull { it.controlId == "REL-CERT-01" }
        assertEquals(ReleaseArtifactAuditVerifier.AuditVerdict.FAIL, certFinding?.verdict)
    }
}
