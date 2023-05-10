package com.example.fileexplorer

import android.app.Notification
import android.content.Context
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

// CoroutineWorker for updating hash in Room
class CoroutineUpdateHashWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = FileDatabase.getDatabase(applicationContext)

        val oldFiles = db.dao.getAllFiles()
        for (file in oldFiles) {
            db.dao.replaceOldHashWithNewHash(file.name)
        }

        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED)
            return Result.failure()
        val initialFiles =
            Environment.getExternalStorageDirectory().listFiles() ?: return Result.failure()

        // Scanning file storage BFS
        // Ignoring files in hidden directories, directories paths and hidden files
        // On app startup newHash becomes oldHash and newHash recalculated
        val queue = ArrayDeque<File>()
        for (file in initialFiles)
            queue.addFirst(file)
        while (!queue.isEmpty()) {
            val curFile = queue.removeLast()
            if (curFile.isDirectory && !curFile.isHidden) {
                val fileArray = curFile.listFiles() ?: arrayOf<File>()
                for (file in fileArray)
                    queue.addFirst(file)
            } else if (curFile.isFile && !curFile.isHidden) {
                val dbFile = db.dao.getFile(curFile.name)
                if (dbFile != null)
                    db.dao.updateNewHash(dbFile.name, calculateHash(curFile))
                else
                    db.dao.upsertFile(FileModel(curFile.name, "", calculateHash(curFile)))
            }
        }
        return Result.success()
    }

    // Overriding for setExpedited request
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, "M_CH_ID")

        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Hash updating")
            .setContentText("Updating hash in foreground")
            .setContentInfo("Info")
        val notification = notificationBuilder.build()
        return ForegroundInfo(1, notification)
    }

    private fun calculateHash(curFile: File): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(md.digest(curFile.readBytes())).toString(16).padStart(32, '0')
    }


}
