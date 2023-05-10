package com.example.fileexplorer

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.readAttributes

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.file_explorer)

        val homeBtn = findViewById<ImageButton>(R.id.homeButton)
        homeBtn.setOnClickListener { openExplorer() }

        val sortSpinner: Spinner = findViewById(R.id.sortSpinner)
        setSpinnerAdapter(sortSpinner, R.array.sort_options)

        val filterSpinner: Spinner = findViewById(R.id.filterSpinner)
        setSpinnerAdapter(filterSpinner, R.array.filter_options)

        val recycleView = findViewById<RecyclerView>(R.id.recyclerView)
        recycleView.layoutManager = LinearLayoutManager(this)
        val currentDir = getDir()
        val currentDirText = findViewById<TextView>(R.id.currentDir)
        currentDirText.text = currentDir.absolutePath
        val files = currentDir.listFiles() ?: arrayOf()
        val recentFiles = ArrayList<File>()
        files.sortBy { file: File -> file.name}
        findViewById<TextView>(R.id.noFilesText).visibility = if (files.isEmpty()) TextView.VISIBLE else TextView.GONE
        recycleView.adapter = FileViewAdapter(files)


        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {sortSpinnerListenerOnSelect(position, if (filterSpinner.selectedItemPosition == 0) files else recentFiles.toTypedArray(), recycleView) }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {filterSpinnerListenerOnSelect(position, files, recentFiles, recycleView) }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }

    // Getting recently updated files (oldHash != newHash)
    // If Worker is still running result may be wrong
    private fun filterSpinnerListenerOnSelect(
        position: Int,
        files: Array<File>,
        recentFiles: ArrayList<File>,
        recycleView: RecyclerView
    ) {
        if (position == 1) {
            try {
                if (WorkManager.getInstance(applicationContext).getWorkInfosByTag("hashUpdate")
                        .get()[0].state != WorkInfo.State.SUCCEEDED
                )
                    Toast.makeText(
                        applicationContext,
                        "Hash update is in progress, result can be wrong",
                        Toast.LENGTH_LONG
                    ).show()
            }  catch (_: java.lang.Exception) { }
            val db = FileDatabase.getDatabase(applicationContext)
            recentFiles.clear()
             this.lifecycleScope.launch {
                 for (file in files) {
                     if (!file.isDirectory && !file.isHidden) {
                         val dbFile = db.dao.getFile(file.name)
                         if (dbFile != null) {
                             if (dbFile.newHash != dbFile.oldHash)
                                 recentFiles.add(file)
                         } else
                             recentFiles.add(file)
                     }
                 }
                withContext(Dispatchers.Main) {
                    recycleView.swapAdapter(FileViewAdapter(recentFiles.toTypedArray()), true)
                }
             }
        }
        else
            recycleView.swapAdapter(FileViewAdapter(files), true)
    }

    private fun sortSpinnerListenerOnSelect(position: Int, files: Array<File>, recycleView: RecyclerView) {
        when (position) {
            0 -> files.sortBy { file: File -> file.name.lowercase() }
            1 -> files.sortByDescending { file: File -> file.name.lowercase() }
            2 -> files.sortBy { file: File -> file.length() }
            3 -> files.sortByDescending { file: File -> file.length() }
            4 -> files.sortBy { file: File -> file.toPath().readAttributes<BasicFileAttributes>().creationTime() }
            5 -> files.sortByDescending { file: File -> file.toPath().readAttributes<BasicFileAttributes>().creationTime() }
            6 -> files.sortBy { file: File -> file.extension }
            7 -> files.sortByDescending { file: File -> file.extension }
        }
        recycleView.swapAdapter(FileViewAdapter(files), true)
    }

    private fun setSpinnerAdapter(spinner: Spinner, arrayId: Int) {
        ArrayAdapter.createFromResource(
            this,
            arrayId,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    // Getting current directory (Default is storage/emulated/0)
    private fun getDir() : File {
        val path: File = if (intent.getStringExtra("path") != null) {
            File(intent.getStringExtra("path")!!)
        } else {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                Environment.getExternalStorageDirectory()
            else
                Environment.getDataDirectory()
        }
        return path
    }

    // Returning to home directory (storage/emulated/0)
    private fun openExplorer() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        ContextCompat.startActivity(applicationContext, intent, null)
    }
}