package io.github.karino2.mdtouch

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ParserTest {
    val testContent = """
    # H1 title
    
    brabra
    
    - hogehoge
    - ikaika
       - fugafuga`tako`gyogyo
    
    after        
    """.trimIndent()

    @Test
    fun splitBlock_recoverOriginal() {
        val parser = Parser()

        val texts = parser.splitBlocks(testContent)

        assertEquals(testContent, texts.joinToString(""))
    }

    @Test
    fun splitBlock_topLevelBlockAlwaysSplitToOneBlock() {
        val parser = Parser()

        val texts = parser.splitBlocks(testContent)
        texts.forEach {
            assertEquals(1, parser.splitBlocks(it).size)
        }
    }

    @Test
    fun parseBlock() {
        val parser = Parser()

        val texts = parser.splitBlocks(testContent)

        val first = parser.parseBlock(texts[0])
        assertEquals(MarkdownElementTypes.ATX_1, first.type)

        val second = parser.parseBlock(texts[1])
        assertEquals(MarkdownTokenTypes.EOL, second.type)
    }
}