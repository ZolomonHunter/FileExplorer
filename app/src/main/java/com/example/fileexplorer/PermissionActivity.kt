package com.example.fileexplorer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE

        // If permission already granted -> open app
        if (isPermissionGranted(permission))
            openExplorer()
        setContentView(R.layout.permission_view)
        val btn = findViewById<Button>(R.id.permissionButton)

        btn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                askForPermissionR()
            else
                askForPermissionLegacy(permission)
        }

    }

    // Asking for READ_EXTERNAL_STORAGE permission (from android developers docs)
    private fun askForPermissionLegacy(permission: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED)
            openExplorer()
        else {
            val requestPermissionLauncher =
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        openExplorer()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "App can't work without permission",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openExplorer()
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    Toast.makeText(
                        applicationContext,
                        "App need access to directories in order to work",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }

    // From SDK 30 need to ask MANAGE_EXTERNAL_STORAGE for shared files
    @RequiresApi(Build.VERSION_CODES.R)
    private fun askForPermissionR() {
        if (Environment.isExternalStorageManager())
            openExplorer()
        else {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else
            ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Setting up WorkManager and opening main activity
    private fun openExplorer() {
        // Notification channel for Expedited request (overriding GetForegroundInfo())
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("M_CH_ID", "hashUpdate", NotificationManager.IMPORTANCE_DEFAULT))

        // Starting Work of updating hash in Room
        val request = OneTimeWorkRequestBuilder<CoroutineUpdateHashWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("hashUpdate")
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "updateHash",
            ExistingWorkPolicy.KEEP,
            request
        )

        // Closing PermissionActivity and opening MainActivity
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        ContextCompat.startActivity(applicationContext, intent, null)
    }

}