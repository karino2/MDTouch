package io.github.karino2.mdtouch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MdViewModel : ViewModel() {
    private val parser = Parser()

    private val _blocks = MutableLiveData(emptyList<Block>())
    val blocks: LiveData<List<Block>> = _blocks

    private val _openState = MutableLiveData(emptyList<Boolean>())
    val openState: LiveData<List<Boolean>> = _openState

    private fun onBlocksChange(newBlocks: List<Block>) {
        _blocks.value = newBlocks
        _openState.value = newBlocks.map { false }
    }

    private val _splitter = { src:String -> parser.splitBlocks(src) }

    fun updateBlock(id: Int, blockSrc: String) {
        val newBlocks = _blocks.value!!.update(_splitter, id, blockSrc)
        onBlocksChange(newBlocks)
    }

    fun appendTailBlocks(blockSrc: String) {
        if(blockSrc != "") {
            val newBlocks = blocks.value!!.appendTail(_splitter, blockSrc)
            onBlocksChange(newBlocks)
        }
    }

    fun updateOpenState(idx: Int, isOpen: Boolean) {
        _openState.value = _openState.value!!.mapIndexed { index2, _ -> if(idx==index2) isOpen else false }
    }

    var duringOpen = false

    fun openMd(newMd: String) {
        duringOpen = true
        onBlocksChange(BlockList.toBlocks(parser.splitBlocks(newMd)))
        duringOpen = false
    }

    fun parseBlock(src: String) = parser.parseBlock(src)
}
