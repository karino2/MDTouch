package io.github.karino2.mdtouch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.karino2.mdtouch.ui.theme.MDTouchTheme
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.*
import org.intellij.markdown.ast.impl.ListCompositeNode
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

@Composable
fun RenderMd(md: String){
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour)

    val tree = parser.buildMarkdownTreeFromString(md)
    RenderMarkdown(md, tree)
}

@Composable
fun RenderMarkdown(md: String, root: ASTNode) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (root is CompositeASTNode) {
            RenderBlocks(md, root, true)
        }
    }
}

@Composable
fun RenderBlocks(md: String, blocks: CompositeASTNode, isTopLevel: Boolean = false) {
    blocks.children.forEach { RenderBlock(md, it, isTopLevel) }
}

@Composable
fun RenderBox(content: AnnotatedString, paddingBottom: Dp, style: TextStyle=LocalTextStyle.current) {
    Box(Modifier.padding(bottom=paddingBottom)) { Text(content,  style=style) }
}

@Composable
fun RenderHeading(md: String, block: CompositeASTNode, style: TextStyle) {
    RenderBox(buildAnnotatedString {
               block.children.forEach { appendHeadingContent(md, it) }
            }, 0.dp, style)
}

fun AnnotatedString.Builder.appendHeadingContent(md: String, node : ASTNode){
    when(node.type) {
        MarkdownTokenTypes.ATX_CONTENT -> {
            appendTrimmingInline(md, node)
            return
        }
    }
    if (node is CompositeASTNode) {
        node.children.forEach { appendHeadingContent(md, it) }
        return
    }
}

fun selectTrimmingInline(node: ASTNode) : List<ASTNode> {
    val children = node.children
    var from = 0
    while (from < children.size && children[from].type == MarkdownTokenTypes.WHITE_SPACE) {
        from++
    }
    var to = children.size
    while (to > from && children[to - 1].type == MarkdownTokenTypes.WHITE_SPACE) {
        to--
    }

    return children.subList(from, to)
}


fun AnnotatedString.Builder.appendInline(md: String, node : ASTNode, childrenSelector : (ASTNode)->List<ASTNode>){
    val targets = childrenSelector(node)
    targets.forEach {
        if(it is LeafASTNode) {
            append(it.getTextInNode(md).toString())
        } else {
            when(it.type) {
                MarkdownElementTypes.CODE_SPAN -> {
                    // val bgcolor = Color(0xFFF5F5F5)
                    val bgcolor =  Color.LightGray
                    pushStyle(SpanStyle(color=Color.Red, background = bgcolor))
                    it.children.subList(1, it.children.size-1).forEach { item->
                        append(item.getTextInNode(md).toString())
                    }
                    pop()
                }

            }
        }
    }
}

fun AnnotatedString.Builder.appendTrimmingInline(md: String, node : ASTNode){
    appendInline(md, node, ::selectTrimmingInline)
}

@Composable
fun RenderBlock(md: String, block: ASTNode, isTopLevel: Boolean = false) {
    when(block.type) {
        MarkdownElementTypes.ATX_1 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h1)
        }
        MarkdownElementTypes.ATX_2 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h2)
        }
        MarkdownElementTypes.ATX_3 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h3)
        }
        MarkdownElementTypes.ATX_4 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h4)
        }
        MarkdownElementTypes.ATX_5 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h5)
        }
        MarkdownElementTypes.ATX_6 -> {
            RenderHeading(md, block as CompositeASTNode, MaterialTheme.typography.h6)
        }
        MarkdownElementTypes.PARAGRAPH -> {
            RenderBox(buildAnnotatedString {
                appendTrimmingInline(md, block)
            }, if (isTopLevel) 8.dp else 0.dp)
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            RenderUnorderedList(md, block as ListCompositeNode, isTopLevel)
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            RenderOrderedList(md, block as ListCompositeNode, isTopLevel)
        }

        MarkdownElementTypes.CODE_FENCE -> {
            RenderCodeFence(md, block)
        }
    }
    println(block.type.name)
}

// similar to CodeFenceGeneratingProvider at GeneratingProviders.kt
@Composable
fun RenderCodeFence(md: String, node: ASTNode) {
    Column (modifier = Modifier.background(Color.LightGray).padding(8.dp)) {
        val codeStyle = TextStyle(fontFamily = FontFamily.Monospace)
        val builder = StringBuilder()

        var childrenToConsider = node.children
        if (childrenToConsider.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size - 1)
        }

        var lastChildWasContent = false

        var renderStart = false
        for (child in childrenToConsider) {
            if (!renderStart && child.type == MarkdownTokenTypes.EOL) {
                renderStart = true
            }
            else
            {
                when(child.type) {
                    MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                        builder.append(child.getTextInNode(md))
                        lastChildWasContent = true
                    }
                    MarkdownTokenTypes.EOL -> {
                        Text(style=codeStyle,
                            text = builder.toString())
                        builder.clear()
                        lastChildWasContent = false
                    }
                }

            }
        }
        if (lastChildWasContent) {
            Text(style=codeStyle,
                text = builder.toString())
        }

    }
}


@Composable
inline fun RenderListColumn(
    isTopLevel: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column ( Modifier.offset(x = if (isTopLevel) 5.dp else 10.dp)
        .padding(bottom = if (isTopLevel) 5.dp else 0.dp)) { content() }
}

@Composable
fun RenderUnorderedList(md: String, list: ListCompositeNode, isTopLevel: Boolean) {
    RenderListColumn(isTopLevel) {
        list.children.forEach { item->
            if (item.type == MarkdownElementTypes.LIST_ITEM) {
                Row {
                    Canvas(modifier = Modifier.size(10.dp)
                        .offset(y=7.dp)
                        .padding(end=5.dp)) {
                        drawCircle(radius=size.width/2, center=center, color= Color.Black) }
                    Box {
                        Column {
                            RenderBlocks(md, item as CompositeASTNode)
                        }
                    }
                }
            }

        }

    }
}

@Composable
fun RenderOrderedList(md: String, list: ListCompositeNode, isTopLevel: Boolean) {
    RenderListColumn(isTopLevel){
        val items = list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }

        val heads = items.runningFold(0) { aggr, item ->
                if (aggr == 0)
                {
                    item.findChildOfType(MarkdownTokenTypes.LIST_NUMBER)
                        ?.getTextInNode(md)?.toString()?.trim()?.let {
                            val number = it.substring(0, it.length - 1).trimStart('0')
                            if (number.isEmpty()) 0 else number.toInt()
                        } ?: 1
                }
                else
                {
                    aggr+1
                }
            }.drop(1)

        heads.zip(items)
            .forEach {(head, item) ->
                val mark = "${head}."
                Row {
                    Box(Modifier.padding(end=5.dp)) {
                        Text(mark)
                    }
                    Box {
                        Column {
                            RenderBlocks(md, item as CompositeASTNode)
                        }
                    }
                }
            }
    }

}
