package io.github.karino2.mdtouch

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.karino2.mdtouch.ui.theme.MDTouchTheme
import java.io.FileOutputStream

class FileChooserActivity : ComponentActivity() {

    val getNewFile = registerForActivityResult(ActivityResultContracts.CreateDocument()) {url->
        contentResolver.openFileDescriptor(url, "w")!!.use {desc->
            val fos = FileOutputStream(desc.fileDescriptor)
            fos.use {
                it.write("# Hello\n\n".toByteArray())
            }
        }

        setResult(RESULT_OK, Intent().apply { data = url} )
        finish()
    }

    val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) {url ->
        setResult(RESULT_OK, Intent().apply { data = url} )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MDTouchTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)) {
                        Button(onClick={
                            getNewFile.launch("text/*")
                        }) {
                            Text("New File")
                        }
                        Spacer(modifier = Modifier.size(5.dp))
                        Button(onClick={
                            openFile.launch(arrayOf("text/*"))
                        }) {
                            Text("Open File")
                        }
                    }
                }
            }
        }
    }
}