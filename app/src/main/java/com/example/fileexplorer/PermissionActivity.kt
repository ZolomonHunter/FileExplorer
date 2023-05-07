package com.example.fileexplorer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.util.*

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE

        setContentView(R.layout.permission_view)
        val btn = findViewById<Button>(R.id.permissionButton)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
            ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED)
            openExplorer()

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    openExplorer()
                } else {
                    Toast.makeText(applicationContext, "App can't work without permission", Toast.LENGTH_LONG)
                        .show()
                }
            }

        btn.setOnClickListener {
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

    private fun openExplorer() {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("M_CH_ID", "hashUpdate", NotificationManager.IMPORTANCE_DEFAULT))

        val request = OneTimeWorkRequestBuilder<CoroutineUpdateHashWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("hashUpdate")
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "updateHash",
            ExistingWorkPolicy.KEEP,
            request
        )

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        ContextCompat.startActivity(applicationContext, intent, null)
    }

}