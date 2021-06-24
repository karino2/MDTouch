package io.github.karino2.mdtouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import io.github.karino2.mdtouch.ui.RenderMd
import io.github.karino2.mdtouch.ui.defaultRenderer
import io.github.karino2.mdtouch.ui.theme.MDTouchTheme
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    companion object {
        const val URL_KEY = "urlkey"
    }

    val Bundle.url : Uri?
        get() = this.getString(URL_KEY)?.let { Uri.parse(it) }


    fun getUrl(intent: Intent?, state: Bundle?) : Uri? {
        return intent?.let {
            if (it.action == Intent.ACTION_VIEW ||
                    it.action == Intent.ACTION_EDIT) {
                it.data
            }
            else
                null
        } ?: state?.url
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _url?.let { outState.putString(URL_KEY, it.toString()) }
        super.onSaveInstanceState(outState)
    }

    private var _url : Uri? = null

    val getFileUrl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if(result.resultCode == RESULT_OK) {
            tryOpenUrl(result.data?.data)
        }
    }

    fun gotoFileChooser() {
        Intent(this, FileChooserActivity::class.java).also { getFileUrl.launch(it) }
    }

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
            contentResolver.openFileDescriptor(url, "w")!!.use {desc->
                val fos = FileOutputStream(desc.fileDescriptor)
                fos.use {
                    it.write(text.toByteArray())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _url = getUrl(intent, savedInstanceState)

        val renderer = defaultRenderer()

        _url?.let { tryOpenUrl(it) }

        viewModel.blocks.observe(this) {newBlockList ->
            if(!viewModel.duringOpen) {
                newBlockList.joinToString("") { it.src }
                    .also { saveMd(it) }
            }
        }

        setContent {
            MDTouchTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    RenderMd(viewModel, renderer)
                }
            }
        }

        if(_url == null) gotoFileChooser()
    }
}

