package io.github.karino2.mdtouch

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.StrikeThroughParser
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.parser.sequentialparsers.*
import org.intellij.markdown.parser.sequentialparsers.impl.*


open class GFMWithWikiFlavourDescriptor : GFMFlavourDescriptor() {
    companion object {
        @JvmField
        val WIKI_LINK: IElementType = MarkdownElementType("WIKI_LINK")
    }

    class WikiLinkParser : SequentialParser {
        fun parseWikiLink(iterator: TokensCache.Iterator): LocalParsingResult? {
            val startIndex = iterator.index
            var it = iterator
            val delegate = RangesListBuilder()

            assert( it.type == MarkdownTokenTypes.LBRACKET)

            it = it.advance()
            if (it.type != MarkdownTokenTypes.LBRACKET) {
                return null
            }
            it = it.advance()
            while(it.type != null) {
                if (it.type == MarkdownTokenTypes.RBRACKET) {
                    it = it.advance()
                    if (it.type == MarkdownTokenTypes.RBRACKET) {
                        // success
                        return LocalParsingResult(it,
                            listOf(SequentialParser.Node(startIndex..it.index + 1, WIKI_LINK)),
                            delegate.get())
                    }
                    return null
                }
                delegate.put(it.index)
                it = it.advance()
            }
            return null
        }

        override fun parse(tokens: TokensCache, rangesToGlue: List<IntRange>): SequentialParser.ParsingResult {
            var result = SequentialParser.ParsingResultBuilder()
            val delegateIndices = RangesListBuilder()
            var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

            while (iterator.type != null) {
                if (iterator.type == MarkdownTokenTypes.LBRACKET) {
                    val wikiLink = parseWikiLink(iterator)
                    if (wikiLink != null) {
                        iterator = wikiLink.iteratorPosition.advance()
                        result = result.withOtherParsingResult(wikiLink)
                        continue
                    }
                }

                delegateIndices.put(iterator.index)
                iterator = iterator.advance()
            }

            return result.withFurtherProcessing(delegateIndices.get())

        }

    }

    override val sequentialParserManager = object : SequentialParserManager() {
        override fun getParserSequence(): List<SequentialParser> {
            return listOf(
                AutolinkParser(listOf(MarkdownTokenTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK)),
                BacktickParser(),
                WikiLinkParser(),
                ImageParser(),
                InlineLinkParser(),
                ReferenceLinkParser(),
                StrikeThroughParser(),
                EmphStrongParser())
        }
    }

}

class Parser {
    val flavour = GFMWithWikiFlavourDescriptor()
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