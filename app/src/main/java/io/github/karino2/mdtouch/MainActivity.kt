package io.github.karino2.mdtouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import io.github.karino2.mdtouch.ui.MdEditor
import io.github.karino2.mdtouch.ui.theme.MDTouchTheme
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    companion object {
        const val URL_KEY = "urlkey"
    }

    val Bundle.url : Uri?
        get() = this.getString(URL_KEY)?.let { Uri.parse(it) }


    fun getUrl(intent: Intent?, state: Bundle?) : Uri? {
        return intent?.data ?: state?.url
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _url?.let { outState.putString(URL_KEY, it.toString()) }
        super.onSaveInstanceState(outState)
    }

    private var _url : Uri? = null


    val viewModel: MdViewModel by viewModels()

    fun tryOpenUrl(url: Uri?) {
        url ?: return
        _url = url

        val text = contentResolver.openFileDescriptor(url, "r")!!.use {desc->
            val fis = FileInputStream(desc.fileDescriptor)
            fis.bufferedReader().use { it.readText() }
        }
        viewModel.openMd(text)
    }

    fun saveMd(text: String) {
        _url?.let { url->
            try {
                contentResolver.openFileDescriptor(url, "wt")!!.use {desc->
                    writeText(desc, text)
                }
            } catch(_: FileNotFoundException) {
                // GoogleDrive throw FileNotFoundException for wt mode.
                // But content provider of local file treat "w" as not truncate.
                // So first try wt, then use w to ensure allcase with truncate.
                contentResolver.openFileDescriptor(url, "w")!!.use {desc->
                    writeText(desc, text)
                }

            }
        }
    }

    private fun writeText(desc: ParcelFileDescriptor, text: String) {
        val fos = FileOutputStream(desc.fileDescriptor)
        fos.use {
            it.write(text.toByteArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _url = getUrl(intent, savedInstanceState)

        _url?.let { tryOpenUrl(it) }

        viewModel.notifySaveState.observe(this) {_ ->
            saveMd(viewModel.fullSrc)
        }

        setContent {
            MDTouchTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MdEditor(viewModel, onClose = { finish() }, onFullSrcSave = {
                        saveMd(it)
                        viewModel.openMd(it)
                        viewModel.isBlockEdit.value = true
                    })
                }
            }
        }
    }
}

