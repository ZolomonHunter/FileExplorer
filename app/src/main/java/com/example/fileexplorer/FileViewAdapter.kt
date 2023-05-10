package com.example.fileexplorer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView.*
import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.readAttributes

class FileViewAdapter(private val fileArray : Array<File>) : Adapter<FileViewAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.fileImage)
        val name = itemView.findViewById<TextView>(R.id.fileNameText)
        val size = itemView.findViewById<TextView>(R.id.fileSizeText)
        val date = itemView.findViewById<TextView>(R.id.fileDateText)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.file_card_view, parent, false)
        return FileViewHolder(view)
    }

    override fun getItemCount(): Int {
        return fileArray.size
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val curFile = fileArray[position]
        holder.name.text = curFile.name
        holder.image.setImageResource(getFileSourceImage(curFile))
        holder.size.text = getFileSize(curFile)
        holder.date.text =
            curFile.toPath().readAttributes<BasicFileAttributes>().creationTime().toString()
        holder.itemView.setOnClickListener { onClickListenerFun(holder.image.context, holder, curFile) }
        holder.itemView.setOnLongClickListener { onLongClickListenerFun(holder.image.context, curFile) }
    }
}

// Open file or directory on click
private fun onClickListenerFun(context: Context, holder: FileViewAdapter.FileViewHolder, curFile: File) {
    if (curFile.isDirectory) {
        val intent = Intent(holder.itemView.context, MainActivity::class.java)
        intent.putExtra("path", curFile.absolutePath)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(holder.itemView.context, intent, null)
    } else if (curFile.isFile) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(context, "com.example.fileexplorer.MyFileProvider", curFile)
        if (curFile.name.endsWith(".jpg") || curFile.name.endsWith(".jpeg") || curFile.name.endsWith(".png"))
            intent.setDataAndType(uri, "image/jpeg")
        else if (curFile.name.endsWith(".mp3"))
            intent.setDataAndType(uri, "audio/x-wav")
        else if (curFile.name.endsWith(".pdf"))
            intent.setDataAndType(uri, "application/pdf")
        else if (curFile.name.endsWith(".txt"))
            intent.setDataAndType(uri, "text/plain")
        else if (curFile.name.endsWith(".zip"))
            intent.setDataAndType(uri, "application/zip")
        else
            intent.setDataAndType(uri, "*/*")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(context, intent, null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No application found which can open the file", Toast.LENGTH_SHORT).show();
        }
    }
}

// Share file on long click
private fun onLongClickListenerFun(context: Context, curFile: File): Boolean {
    return if (curFile.isFile) {
        val intent = Intent(Intent.ACTION_SEND)
        val uri =
            FileProvider.getUriForFile(context, "com.example.fileexplorer.MyFileProvider", curFile)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(context, intent, null)
        true
    }
    else false
}

    private fun getFileSize(curFile: File): CharSequence? {
        if (curFile.isFile) {
            var bytes = curFile.length()
            var measure = " bytes"
            if (bytes > 1024) {
                bytes /= 1024
                measure = " kb"
                if (bytes > 1024) {
                    bytes /= 1024
                    measure = " mb"
                }
            }
            return bytes.toString() + measure
        }
        if (curFile.isDirectory)
            return (curFile.listFiles() ?: arrayOf()).size.toString() + " files"
        return null
    }

    private fun getFileSourceImage(curFile : File) : Int {
        return if (curFile.isFile) {
            if (curFile.name.endsWith(".jpg") || curFile.name.endsWith(".jpeg") )
                R.drawable.jpg
            else if (curFile.name.endsWith(".mp3"))
                R.drawable.mp3
            else if (curFile.name.endsWith(".pdf"))
                R.drawable.pdf
            else if (curFile.name.endsWith(".png"))
                R.drawable.png
            else if (curFile.name.endsWith(".txt"))
                R.drawable.txt
            else if (curFile.name.endsWith(".zip"))
                R.drawable.zip
            else if (curFile.name.endsWith(".apk"))
                R.drawable.apk
            else
                R.drawable.file
        }
        else if (curFile.isDirectory)
            R.drawable.folder
        else
            R.drawable.file
    }
