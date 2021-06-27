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


    private val _notifySaveState = MutableLiveData<Int>()
    val notifySaveState : LiveData<Int> = _notifySaveState

    private fun onBlocksChange(newBlocks: List<Block>, notifySave : Boolean = true) {
        _blocks.value = newBlocks
        _openState.value = newBlocks.map { false }
        if(notifySave) {
            _notifySaveState.value = _notifySaveState.value?.let { it +1 } ?: 0
        }
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

    val isBlockOpen : Boolean
    get() {
        return _openState.value!!.any { it }
    }

    fun closeOpenState() {
        _openState.value = _openState.value!!.map { false }
    }

    fun openMd(newMd: String) {
        onBlocksChange(BlockList.toBlocks(parser.splitBlocks(newMd)), false)
    }

    fun parseBlock(src: String) = parser.parseBlock(src)
}
