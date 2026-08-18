package com.example
import android.net.Uri
import java.io.File
fun getFallbackUri(context: android.content.Context): Uri {
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
    val file = File(dir, "fortknox_backup_${System.currentTimeMillis()}.fortknox")
    return Uri.fromFile(file)
}
