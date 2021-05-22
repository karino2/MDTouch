package io.github.karino2.mdtouch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import io.github.karino2.mdtouch.ui.RenderMd
import io.github.karino2.mdtouch.ui.theme.MDTouchTheme
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrintHtml(md)
        setContent {
            MDTouchTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    RenderMd(md)
                }
            }
        }
    }
}

val md2 = """
3. hogehoge
4. ikaika
5. fugafuga
"""

val md = """
# H1 title

brabra

- hogehoge
- ikaika
   - fugafuga`tako`gyogyo

after

5. five
6. six
7. seven

aaa

1. hoge
1. ika
1. fuga

```
code
block
```

## H2 title

abc

"""

fun PrintHtml(md: String) {
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour)

    val tree = parser.buildMarkdownTreeFromString(md)
    val html = HtmlGenerator(md, tree, flavour).generateHtml()
    println(html)
    /*
            return HtmlGenerator(md, tree, flavour).generateHtml(

     */
}

