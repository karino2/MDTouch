package io.github.karino2.mdtouch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
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
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.*
import org.intellij.markdown.ast.impl.ListCompositeNode


data class MarkdownRenderer(
    val renderBlock:@Composable (ctx: RenderContext, block: ASTNode, isTopLevel: Boolean) -> Unit,
    val renderHeading:@Composable (ctx: RenderContext, block: CompositeASTNode, style: TextStyle) -> Unit
)

// src is topLevelBlock.
data class RenderContext(val src: String, val renderer: MarkdownRenderer)

fun defaultRenderer() = MarkdownRenderer(
    renderBlock = { ctx, block, isTopLevel ->
        DefaultRenderBlock(
            ctx,
            block,
            isTopLevel
        )
    },
    renderHeading = { ctx, block, style ->
        DefaultRenderHeading(
            ctx,
            block,
            style
        )
    }
)

@Composable
fun RenderTopLevelBlocks(blocks: List<String>, parseFun: (block:String)->ASTNode, renderer: MarkdownRenderer){
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        blocks.forEach {
            RenderTopLevelBlock(it, parseFun, renderer)
        }
    }
}

@Composable
fun RenderTopLevelBlock(block: String, parseFun: (block:String)->ASTNode, renderer: MarkdownRenderer) {
    val node = parseFun(block)
    val ctx = RenderContext(block, renderer)
    ctx.renderer.renderBlock(ctx, node, true)
}

@Composable
fun RenderBlocks(ctx: RenderContext, blocks: CompositeASTNode, isTopLevel: Boolean = false) {
    blocks.children.forEach { ctx.renderer.renderBlock(ctx, it, isTopLevel) }
}


@Composable
fun RenderBox(content: AnnotatedString, paddingBottom: Dp, style: TextStyle = LocalTextStyle.current) {
    Box(Modifier.padding(bottom=paddingBottom)) { Text(content,  style=style) }
}


@Composable
fun DefaultRenderHeading(ctx: RenderContext, block: CompositeASTNode, style: TextStyle) {
    RenderBox(buildAnnotatedString {
        block.children.forEach { appendHeadingContent(ctx.src, it) }
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
                    pushStyle(SpanStyle(color= Color.Red, background = bgcolor))
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
fun DefaultRenderBlock(ctx: RenderContext, block: ASTNode, isTopLevel: Boolean) {
    when(block.type) {
        MarkdownElementTypes.ATX_1 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h1)
        }
        MarkdownElementTypes.ATX_2 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h2)
        }
        MarkdownElementTypes.ATX_3 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h3)
        }
        MarkdownElementTypes.ATX_4 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h4)
        }
        MarkdownElementTypes.ATX_5 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h5)
        }
        MarkdownElementTypes.ATX_6 -> {
            ctx.renderer.renderHeading(ctx, block as CompositeASTNode, MaterialTheme.typography.h6)
        }
        MarkdownElementTypes.PARAGRAPH -> {
            RenderBox(buildAnnotatedString {
                appendTrimmingInline(ctx.src, block)
            }, if (isTopLevel) 8.dp else 0.dp)
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            RenderUnorderedList(ctx, block as ListCompositeNode, isTopLevel)
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            RenderOrderedList(ctx, block as ListCompositeNode, isTopLevel)
        }

        MarkdownElementTypes.CODE_FENCE -> {
            RenderCodeFence(ctx.src, block)
        }
    }
    println(block.type.name)
}

// similar to CodeFenceGeneratingProvider at GeneratingProviders.kt
@Composable
fun RenderCodeFence(md: String, node: ASTNode) {
    Column (modifier = Modifier
        .background(Color.LightGray)
        .padding(8.dp)) {
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
    Column (
        Modifier
            .offset(x = if (isTopLevel) 5.dp else 10.dp)
            .padding(bottom = if (isTopLevel) 5.dp else 0.dp)) { content() }
}

@Composable
fun RenderUnorderedList(ctx:RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    RenderListColumn(isTopLevel) {
        list.children.forEach { item->
            if (item.type == MarkdownElementTypes.LIST_ITEM) {
                Row {
                    Canvas(modifier = Modifier
                        .size(10.dp)
                        .offset(y = 7.dp)
                        .padding(end = 5.dp)) {
                        drawCircle(radius=size.width/2, center=center, color= Color.Black) }
                    Box {
                        Column {
                            RenderBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }

        }

    }
}

@Composable
fun RenderOrderedList(ctx:RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    RenderListColumn(isTopLevel){
        val items = list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }

        val heads = items.runningFold(0) { aggr, item ->
            if (aggr == 0)
            {
                item.findChildOfType(MarkdownTokenTypes.LIST_NUMBER)
                    ?.getTextInNode(ctx.src)?.toString()?.trim()?.let {
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
                            RenderBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }
    }

}
