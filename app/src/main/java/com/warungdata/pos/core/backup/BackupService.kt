package com.warungdata.pos.core.backup

import android.content.Context
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.BackupHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResult(val file: File, val size: Long, val checksum: String)

class BackupService(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dbPath: String
        get() = context.getDatabasePath("warungdata.db").absolutePath

    suspend fun createBackup(type: String = "manual"): BackupResult = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "backups")
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(dir, "warungdata_backup_$ts.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val dbFile = File(dbPath)
            if (dbFile.exists()) {
                zos.putNextEntry(ZipEntry("warungdata.db"))
                dbFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("info.txt"))
                val info = buildString {
                    appendLine("Backup Date: ${Date()}")
                    appendLine("App: WarungData POS")
                    appendLine("DB Size: ${dbFile.length()} bytes")
                    appendLine("Type: $type")
                }
                zos.write(info.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { f ->
                    zos.putNextEntry(ZipEntry("exports/${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        val checksum = sha256(zipFile)
        val size = zipFile.length()

        db.backupHistoryDao().insertBackup(
            BackupHistoryEntity(
                filePath = zipFile.absolutePath,
                fileSize = size,
                type = type,
                checksum = checksum
            )
        )

        BackupResult(zipFile, size, checksum)
    }

    suspend fun restoreBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(backupFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "warungdata.db") {
                        val dbFile = File(dbPath)
                        dbFile.parentFile?.mkdirs()
                        FileOutputStream(dbFile).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getBackupHistory(): List<BackupHistoryEntity> = withContext(Dispatchers.IO) {
        db.backupHistoryDao().getAllBackups().first()
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buf = ByteArray(8192)
            var len: Int
            while (fis.read(buf).also { len = it } != -1) {
                md.update(buf, 0, len)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
