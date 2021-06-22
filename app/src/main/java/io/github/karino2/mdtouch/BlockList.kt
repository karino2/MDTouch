package io.github.karino2.mdtouch


data class Block(val id: Int, val src: String)

class BlockList {
    companion object {
        private var _lastId = 1

        private fun genId() = _lastId++

        fun resetLastId() { _lastId = 1 }

        fun toBlocks(srces: List<String>) : List<Block> {
            return srces.map { Block(genId(), it) }
        }
    }
}

fun List<Block>.update(splitter: (src:String)-> List<String>, id: Int, newText:String) : List<Block>
{
    assert(id != -1)
    if(newText == "")
    {
        if(id == -1)
            return this
        return this.deleteBlock(id)
    }

    val newSrcs = splitter(newText)
    return if (newSrcs.size == 1)
    {
        this.replace(id, newText)
    }
    else
    {
        val newBlocks = BlockList.toBlocks(newSrcs)
        this.replaceBlocks(id, newBlocks)
    }
}

fun List<Block>.appendTail(splitter: (src:String)->List<String>, newText:String) : List<Block> {
    val tailBlocks = BlockList.toBlocks(listOf("\n", "\n") + splitter(newText))
    return this.appendBlocksToTail(tailBlocks)
}

private fun List<Block>.replaceBlocks(id: Int, newBlocks: List<Block>): List<Block> {
    return this.flatMap {
        if (it.id == id)
        {
            newBlocks
        }
        else
        {
            listOf(it)
        }
    }
}

private fun List<Block>.replace(id: Int, newText: String): List<Block> {
    return this.map { if(it.id == id) { Block(id, newText) } else { it } }
}

private fun List<Block>.appendBlocksToTail(tailBlocks: List<Block>): List<Block> {
    val newBlocks = ArrayList<Block>()
    newBlocks.addAll(this)
    newBlocks.addAll(newBlocks.size, tailBlocks)
    return newBlocks
}

private fun List<Block>.deleteBlock(id: Int): List<Block> {
    return this.filter { it.id != id }
}
