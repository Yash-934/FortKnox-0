package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DecoyNoteEntity
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntrusionLogEntity
import com.example.security.KeystoreManager
import com.example.security.NativeCore
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        EncryptedVaultEntity::class,
        IntrusionLogEntity::class,
        DecoyNoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao
    abstract fun intrusionLogDao(): IntrusionLogDao
    abstract fun decoyNoteDao(): DecoyNoteDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = KeystoreManager.getOrCreateDatabasePassphrase(context.applicationContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "fortknox_encrypted_vault.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                // Zeroize passphrase in memory after initialization
                NativeCore.wipe(passphrase)
                NativeCore.munlock(passphrase)

                INSTANCE = instance
                instance
            }
        }
    }
}
