package io.github.karino2.mdtouch.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsWithImePadding
import io.github.karino2.mdtouch.Block
import io.github.karino2.mdtouch.GFMWithWikiFlavourDescriptor
import io.github.karino2.mdtouch.MdViewModel
import io.github.karino2.mdtouch.R
import io.github.karino2.mdtouch.ui.theme.Teal200
import kotlinx.coroutines.launch
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.*
import org.intellij.markdown.ast.impl.ListCompositeNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import kotlin.math.roundToInt


// src is topLevelBlock.
data class RenderContext(val block: Block) {
    val src: String
        get() = block.src
}



@Composable
fun MdEditor(viewModel: MdViewModel, onClose: ()->Unit, onFullSrcSave: (String)->Unit) {
    if (viewModel.isBlockEdit.value) {
        MdBlockEdit(viewModel, onClose)
    } else {
        MdFullSrcEdit(viewModel, onFullSrcSave)
    }

}

@Composable
private fun MdFullSrcEdit(viewModel: MdViewModel, onSave: (String)->Unit) {
    var text by remember { mutableStateOf(viewModel.fullSrc) }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onSave(text) }) {
                    Icon(painter = painterResource(id = R.drawable.outline_save), contentDescription = "Save")
                }
            }
        },
            navigationIcon = {
                IconButton(onClick = { viewModel.isBlockEdit.value = true }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        TextField(modifier = Modifier.fillMaxSize(), value = text, onValueChange = { text = it })
    }
}

@Composable
private fun MdBlockEdit(
    viewModel: MdViewModel,
    onClose: () -> Unit
) {
    // https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary
    // val toolbarHeight = 48.dp
    val toolbarHeight = 56.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    var textState by remember { mutableStateOf(viewModel.selectedBlock.value.src) }
    val cscope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(nestedScrollConnection)) {
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier
                .verticalScroll(scrollState)
                .padding(top = toolbarHeight)) {
                viewModel.blocks.value.forEachIndexed { index, block ->
                    key(block.id) {
                        TopLevelBlock(block,
                            block == viewModel.selectedBlock.value,
                            { viewModel.parseBlock(it) },
                            onSelect = { newSelect ->
                                viewModel.updateSelectionState(index, newSelect)
                                textState = viewModel.selectedBlock.value.src
                            }
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                    }
                }
            }

            TopAppBar(
                modifier = Modifier
                    .height(toolbarHeight)
                    .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
                title = {
                    Text("MDTouch")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { viewModel.isBlockEdit.value = false }) {
                            Icon(painter = painterResource(id = R.drawable.outline_source), contentDescription = "Source")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

        }
        EditBlockBox(
            viewModel.selectedBlock.value,
            textState,
            onEditing = { newText -> textState = newText },
            onSubmitNewBlock = {
                viewModel.appendTailBlocks(textState)
                cscope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                textState = ""
            },
            onSubmitEditBlock = { blockId ->
                viewModel.updateBlock(blockId, textState)
                textState = ""
            },
            onCancelEdit = { blockId ->
                viewModel.updateSelectionState(blockId, false)
                textState = ""
            }
        )
    }
}


@Composable
fun ColumnScope.EditBlockBox(
    block: Block,
    editingText: String,
    onEditing: (String) -> Unit,
    onSubmitNewBlock: () -> Unit,
    onSubmitEditBlock: (blockId: Int) -> Unit,
    onCancelEdit: (blockId: Int) -> Unit
) {
    val forAppend = block.isEmpty
    val submitLabel = if (forAppend) "Add" else "Submit"
    TextField(
        value = editingText,
        onValueChange = onEditing,
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.align(Alignment.End).navigationBarsWithImePadding()) {
        if (!forAppend) {
            Button(onClick = {
                onCancelEdit(block.id)
            }) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.size(10.dp))
        }
        Button(onClick = {
            if (forAppend) {
                onSubmitNewBlock()
            } else {
                onSubmitEditBlock(block.id)
            }
        }) {
            Text(submitLabel)
        }
    }
}

@Composable
fun TopLevelBlock(
    block: Block, isSelected: Boolean,
    parseFun: (block: String) -> ASTNode,
    onSelect: (isSelect: Boolean) -> Unit
) {
    if (block.src == "\n")
        return
    val node = parseFun(block.src)
    val ctx = RenderContext(block)

    // draw bounding box and call onSelect
    val boxModifier = if (isSelected) Modifier.background(Teal200)
        .fillMaxWidth() else Modifier.clickable { onSelect(true) }
    Box(modifier = boxModifier) {
        MdBlock(ctx, node, true)
    }
}

@Composable
fun MdBlocks(ctx: RenderContext, blocks: CompositeASTNode, isTopLevel: Boolean = false) {
    blocks.children.forEach { MdBlock(ctx, it, isTopLevel) }
}


@Composable
fun AnnotatedBox(
    content: AnnotatedString,
    paddingBottom: Dp,
    style: TextStyle = LocalTextStyle.current
) {
    Box(Modifier.padding(bottom = paddingBottom)) { Text(content, style = style) }
}


@Composable
fun Heading(ctx: RenderContext, block: CompositeASTNode, style: TextStyle) {
    AnnotatedBox(buildAnnotatedString {
        block.children.forEach { appendHeadingContent(ctx.src, it, MaterialTheme.colors) }
    }, 0.dp, style)
}

fun AnnotatedString.Builder.appendHeadingContent(md: String, node: ASTNode, colors: Colors) {
    when (node.type) {
        MarkdownTokenTypes.ATX_CONTENT -> {
            appendTrimmingInline(md, node, colors)
            return
        }
    }
    if (node is CompositeASTNode) {
        node.children.forEach { appendHeadingContent(md, it, colors) }
        return
    }
}

fun selectTrimmingInline(node: ASTNode): List<ASTNode> {
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


fun AnnotatedString.Builder.withStyle(
    style: SpanStyle,
    builder: AnnotatedString.Builder.() -> Unit
) {
    pushStyle(style)
    this.builder()
    pop()
}

fun AnnotatedString.Builder.appendInline(
    md: String,
    node: ASTNode,
    childrenSelector: (ASTNode) -> List<ASTNode>,
    colors: Colors
) {
    val targets = childrenSelector(node)
    targets.forEachIndexed { index, child ->
        if (child is LeafASTNode) {
            when (child.type) {
                MarkdownTokenTypes.EOL -> {
                    // treat as space, except the case of BR EOL
                    if (index != 0 && targets[index - 1].type != MarkdownTokenTypes.HARD_LINE_BREAK)
                        append(" ")
                }
                MarkdownTokenTypes.HARD_LINE_BREAK -> append("\n")
                else -> append(child.getTextInNode(md).toString())
            }

        } else {
            when (child.type) {
                MarkdownElementTypes.CODE_SPAN -> {
                    // val bgcolor = Color(0xFFF5F5F5)
                    val bgcolor = Color.LightGray
                    pushStyle(SpanStyle(color = Color.Red, background = bgcolor))
                    child.children.subList(1, child.children.size - 1).forEach { item ->
                        append(item.getTextInNode(md).toString())
                    }
                    pop()
                }
                MarkdownElementTypes.STRONG -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(2, parent.children.size - 2) },
                            colors
                        )
                    }
                }
                MarkdownElementTypes.EMPH -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(1, parent.children.size - 1) },
                            colors
                        )
                    }
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    withStyle(
                        SpanStyle(
                            colors.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        child.children.filter { it.type == MarkdownElementTypes.LINK_TEXT }
                            .forEach { linktext ->
                                linktext.children.subList(1, linktext.children.size - 1).forEach {
                                    append(it.getTextInNode(md).toString())
                                }
                            }
                    }
                }
                GFMElementTypes.STRIKETHROUGH -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(2, parent.children.size - 2) },
                            colors
                        )
                    }
                }
                GFMWithWikiFlavourDescriptor.WIKI_LINK -> {
                    // [[WikiName]]
                    assert(child.children.size == 5)

                    // Render [[WikiName]] as [[WikiName]] with link like decoration.
                    withStyle(
                        SpanStyle(
                            colors.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(child.getTextInNode(md).toString())
                    }

                }

            }
        }
    }
}

fun AnnotatedString.Builder.appendTrimmingInline(md: String, node: ASTNode, colors: Colors) {
    appendInline(md, node, ::selectTrimmingInline, colors)
}


@Composable
fun MdBlock(ctx: RenderContext, block: ASTNode, isTopLevel: Boolean) {
    when (block.type) {
        MarkdownElementTypes.ATX_1 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.h3)
        }
        MarkdownElementTypes.ATX_2 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.h4)
        }
        MarkdownElementTypes.ATX_3 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.h5)
        }
        MarkdownElementTypes.ATX_4 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.h6)
        }
        MarkdownElementTypes.ATX_5 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.subtitle1)
        }
        MarkdownElementTypes.ATX_6 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.subtitle2)
        }
        MarkdownElementTypes.PARAGRAPH -> {
            AnnotatedBox(buildAnnotatedString {
                appendTrimmingInline(ctx.src, block, MaterialTheme.colors)
            }, if (isTopLevel) 8.dp else 0.dp)
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            MdUnorderedList(ctx, block as ListCompositeNode, isTopLevel)
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            MdOrderedList(ctx, block as ListCompositeNode, isTopLevel)
        }

        MarkdownElementTypes.CODE_FENCE -> {
            CodeFence(ctx.src, block)
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            // for click target.
            Box(modifier = Modifier.height(10.dp)) {
                Divider(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.DarkGray,
                    thickness = 2.dp
                )
            }
        }
    }
    // println(block.type.name)
}

// similar to CodeFenceGeneratingProvider at GeneratingProviders.kt
@Composable
fun CodeFence(md: String, node: ASTNode) {
    Column(
        modifier = Modifier
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
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
            } else {
                when (child.type) {
                    MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                        builder.append(child.getTextInNode(md))
                        lastChildWasContent = true
                    }
                    MarkdownTokenTypes.EOL -> {
                        Text(
                            style = codeStyle,
                            text = builder.toString()
                        )
                        builder.clear()
                        lastChildWasContent = false
                    }
                }

            }
        }
        if (lastChildWasContent) {
            Text(
                style = codeStyle,
                text = builder.toString()
            )
        }

    }
}


@Composable
inline fun MdListColumn(
    isTopLevel: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .offset(x = if (isTopLevel) 5.dp else 10.dp)
            .padding(bottom = if (isTopLevel) 5.dp else 0.dp)
    ) { content() }
}

@Composable
fun MdUnorderedList(ctx: RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    MdListColumn(isTopLevel) {
        list.children.forEach { item ->
            if (item.type == MarkdownElementTypes.LIST_ITEM) {
                Row {
                    Canvas(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(y = 7.dp)
                            .padding(end = 5.dp)
                    ) {
                        drawCircle(radius = size.width / 2, center = center, color = Color.Black)
                    }
                    Box {
                        Column {
                            MdBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }

        }

    }
}

@Composable
fun MdOrderedList(ctx: RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    MdListColumn(isTopLevel) {
        val items = list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }

        val heads = items.runningFold(0) { aggr, item ->
            if (aggr == 0) {
                item.findChildOfType(MarkdownTokenTypes.LIST_NUMBER)
                    ?.getTextInNode(ctx.src)?.toString()?.trim()?.let {
                        val number = it.substring(0, it.length - 1).trimStart('0')
                        if (number.isEmpty()) 0 else number.toInt()
                    } ?: 1
            } else {
                aggr + 1
            }
        }.drop(1)

        heads.zip(items)
            .forEach { (head, item) ->
                val mark = "${head}."
                Row {
                    Box(Modifier.padding(end = 5.dp)) {
                        Text(mark)
                    }
                    Box {
                        Column {
                            MdBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }
    }
}
