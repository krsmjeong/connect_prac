package com.example.codepractice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.codepractice.adapter.RecyclerviewAdapter
import com.example.codepractice.data.FileListData
import com.example.codepractice.databinding.ActivityMainBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val mBinding : ActivityMainBinding? = null
    private val binding get() = mBinding!!

    private val unZipFileDataClass: MutableList<FileListData> = mutableListOf()
    private var unzipFileNameList: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val httpUrl = binding.putinUrl.text.toString()
        val progressBar = binding.progressBar


        binding.downloadBtn.setOnClickListener {
            progressBar.progress = 0
            downloadFile(httpUrl, progressBar)
            recyclerViewInit()
        }
    }

    private fun downloadFile(httpURL: String, progressBar: ProgressBar) : List<String> {
        thread(start = true) {
            var inputStream: InputStream? = null
            var fileOutputStream: FileOutputStream? = null
            val pathForUnzip = "${Environment.getExternalStorageDirectory()}/${DIRECTORY_DOWNLOADS}"
            val downloadDirectory = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            val responseCode: Int
            try {
                val url = URL(httpURL)
                val httpUrlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                responseCode = httpUrlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val zipFileName = httpURL.substring(httpURL.lastIndexOf("/"))
                    inputStream = httpUrlConnection.inputStream
                    fileOutputStream = FileOutputStream(File(downloadDirectory, zipFileName), false)
                    val bufferSize = 4096
                    var bytesRead: Int
                    val buffer = ByteArray(bufferSize)
                    val fileSize = httpUrlConnection.contentLength
                    progressBar.max = fileSize

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        progressBar.incrementProgressBy(bytesRead)
                        fileOutputStream.write(buffer, 0, bytesRead)
                    }
                    println("file download success")
                    val unzipTargetFileName = zipFileName.substring(0, zipFileName.lastIndexOf("."))
                    val zipFilePath = "${pathForUnzip}/$zipFileName"
                    unZip(zipFilePath, pathForUnzip)
                    unzipFileNameList = getFileNameList(unzipTargetFileName, pathForUnzip)
                } else {
                    println("file downloaded failed. responseCode = $responseCode")
                }
            } catch (e: Exception) {
                println("download failed.")
            }
            fileOutputStream?.close()
            inputStream?.close()
        }
        return unzipFileNameList
    }

    private fun unZip(zipFilePath : String, pathForUnzip: String) {
        val zip = ZipFile(zipFilePath)
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry: ZipEntry = enumeration.nextElement()
            val destinationFilePath = File(pathForUnzip, entry.name)
            destinationFilePath.parentFile?.mkdirs()

            if(entry.isDirectory) {
                continue
            }

            val bufferedInputStream = BufferedInputStream(zip.getInputStream(entry))

            bufferedInputStream.use {
                destinationFilePath.outputStream().buffered(1024).use {
                        bufferdOutputStream -> bufferedInputStream.copyTo(bufferdOutputStream)
                }
            }
        }
    }

    private fun getFileNameList(unzipTargetFileName: String, pathForUnzip: String): List<String> {
        val path = "${pathForUnzip}/$unzipTargetFileName"
        val directory = File(path)
        val files = directory.listFiles()
        val filesNameList: MutableList<String> = ArrayList()

        if (files != null) {
            for (i in files.indices) {
                filesNameList.add(files[i].name)
            }
        }
        println("filesNameList == $filesNameList")
        return filesNameList
    }

    private fun recyclerViewInit() {
        runOnUiThread {
            val recyclerviewAdapter = RecyclerviewAdapter()
            recyclerviewAdapter.listData = unZipFileDataClass
            binding.filenameRv.adapter = recyclerviewAdapter
            binding.filenameRv.layoutManager = LinearLayoutManager(this)
        }
    }
}











