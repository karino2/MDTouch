package io.github.karino2.mdtouch

import org.junit.Test
import org.junit.Assert.*

class BlockListTest {
    val parser = Parser()
    fun splitter(src: String) = parser.splitBlocks(src)

    val testMd = """
    # H1 title
    
    brabra
    
    fugafuga
    hegahega
    
    - hogehoge
    - ikaika
       - fugafuga`tako`gyogyo
    
    after        
    """.trimIndent()

    val testBlocks = BlockList.toBlocks(splitter(testMd))

    /*
        For append last block, newly added block will be separated by \n\n.
     */
    @Test
    fun update_appendOneBlockToTail() {
        val actual = testBlocks.update(::splitter, -1, "newBlock")
        assertNotEquals(testBlocks, actual)
        testBlocks.forEachIndexed { index, block ->
            assertEquals(block, actual[index])
        }
        assertEquals(actual.size, testBlocks.size+3)

        val thirdLast = actual[actual.size-3]
        assertTrue( thirdLast.id > testBlocks.last().id)
        assertEquals( "\n", thirdLast.src)

        val secondLast = actual[actual.size-2]
        assertTrue( secondLast.id > testBlocks.last().id)
        assertEquals( "\n", secondLast.src)

        val newb = actual.last()
        assertEquals("newBlock", newb.src)
        assertTrue( newb.id > 1)
    }

    @Test
    fun update_replaceBraBra_OneBlock() {
        val brabra = testBlocks[3]
        assertEquals("brabra", brabra.src)

        val actual = testBlocks.update(::splitter, brabra.id, "hogehoge")
        assertNotEquals(testBlocks, actual)
        assertEquals(testBlocks.size, actual.size)
        testBlocks.forEachIndexed { index, block ->
            if (block.id != brabra.id)
                assertEquals(block, actual[index])
        }

        val actualBrabra = actual[3]
        assertEquals(brabra.id, actualBrabra.id)
        assertEquals("hogehoge", actualBrabra.src)
    }

    @Test
    fun update_replaceBraBra_SeveralBlocks() {
        val brabra = testBlocks[3]
        assertEquals("brabra", brabra.src)

        val actual = testBlocks.update(::splitter, brabra.id, "hogehoge\n\nikaika")
        assertNotEquals(testBlocks, actual)
        assertEquals(testBlocks.size+3, actual.size)
        for(i in 0..2) {
            assertEquals(testBlocks[i], actual[i])
        }
        for(i in 4 until testBlocks.size) {
            assertEquals(testBlocks[i], actual[i+3])
        }

        val newBlocks = actual.slice(3..6)
        newBlocks.forEach { assertTrue( it.id > testBlocks.last().id ) }
        assertEquals("hogehoge", newBlocks[0].src)
        assertEquals("\n", newBlocks[1].src)
        assertEquals("\n", newBlocks[2].src)
        assertEquals("ikaika", newBlocks[3].src)
    }

    @Test
    fun update_deleteBrabra() {
        val brabra = testBlocks[3]
        assertEquals("brabra", brabra.src)

        val actual = testBlocks.update(::splitter, brabra.id, "")
        assertNotEquals(testBlocks, actual)
        assertEquals(testBlocks.size-1, actual.size)

        for(i in 0..2) {
            assertEquals(testBlocks[i], actual[i])
        }
        for(i in 4 until testBlocks.size) {
            assertEquals(testBlocks[i], actual[i-1])
        }

    }

}