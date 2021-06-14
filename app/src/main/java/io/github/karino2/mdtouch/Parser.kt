package io.github.karino2.mdtouch

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class Parser {
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour)

    fun splitBlocks(md: String) : List<String> {
        val tree = parser.buildMarkdownTreeFromString(md)

        return tree.children.map {
            it.getTextInNode(md).toString()
        }
    }

    fun parseBlock(block: String) : ASTNode {
        val root = parser.buildMarkdownTreeFromString(block)
        return root.children[0]
    }


}