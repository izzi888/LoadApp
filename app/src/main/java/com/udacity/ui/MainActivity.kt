package com.udacity.ui

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Patterns
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.udacity.Const.defaultFiles
import com.udacity.R
import com.udacity.data.FileItem
import com.udacity.ui.custom.LoadingButton
import com.udacity.utils.download
import com.udacity.utils.sendNotification
import com.udacity.utils.showShortToast

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }

    private val customButton by lazy {
        findViewById<LoadingButton>(R.id.custom_button)
    }

    private val radioGroup by lazy {
        findViewById<RadioGroup>(R.id.rg_download_options)
    }

    private val editText by lazy {
        findViewById<EditText>(R.id.et_custom)
    }

    private val downloadManager by lazy {
        getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    }

    private val notificationManager by lazy {
        ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadID == id) {
                Toast.makeText(this@MainActivity, "Download Completed", Toast.LENGTH_SHORT).show()
            }

            val query = DownloadManager.Query().setFilterById(id)

            val cursor = downloadManager.query(query)

            customButton.buttonState = ButtonState.Completed

            if (cursor.moveToFirst()) {
                val status =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val title =
                    cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                val description =
                    cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))


                println(status)
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        notificationManager.sendNotification(
                            "The $title file is downloaded successfully",
                            applicationContext,
                            FileItem(
                                fileName = title,
                                fileDescription = description,
                                fileStatus = "Success"))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        notificationManager.sendNotification(
                            "The $title file is not downloaded properly",
                            applicationContext,
                            FileItem(
                                fileName = title,
                                fileDescription = description,
                                fileStatus = "Fail"))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        customButton.setOnClickListener {
            customButton.buttonState = ButtonState.Completed
            checkDownloadOption()
        }

    }

    private fun checkDownloadOption() {
        val chosenOption = radioGroup.checkedRadioButtonId
        if (chosenOption == -1) {
            showShortToast(getString(R.string.no_option_chosen))
            return
        }

        customButton.buttonState = ButtonState.Loading

        when (chosenOption) {
            R.id.rb_glide -> {
                downloadID = download(downloadManager, defaultFiles[0])
            }
            R.id.rb_load_app -> {
                downloadID = download(downloadManager, defaultFiles[1])
            }
            R.id.rb_retrofit -> {
                downloadID = download(downloadManager, defaultFiles[2])
            }
            R.id.rb_custom -> {
                performCustomLoading()
            }
        }
    }

    private fun performCustomLoading() {
        val content = editText.text.toString()
        if (URLUtil.isValidUrl(content)) {
            downloadID = download(downloadManager,
                FileItem(fileName = "Custom",
                    fileDescription = "Your custom URL",
                    fileUrl = content))
        } else {
            showShortToast("Not valid URL")
            customButton.buttonState = ButtonState.Completed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }
}
