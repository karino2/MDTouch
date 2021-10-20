package io.github.karino2.mdtouch

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MdViewModel : ViewModel() {
    private val parser = Parser()

    val blocks = mutableStateOf(emptyList<Block>())
    val openState = mutableStateOf(emptyList<Boolean>())

    private val _notifySaveState = MutableLiveData<Int>()
    val notifySaveState : LiveData<Int> = _notifySaveState

    private val emptyBlock = Block(-1, "")
    val selectedBlock = mutableStateOf(emptyBlock)

    private fun onBlocksChange(newBlocks: List<Block>, notifySave : Boolean = true) {
        blocks.value = newBlocks
        openState.value = newBlocks.map { false }
        selectedBlock.value = emptyBlock
        if(notifySave) {
            _notifySaveState.value = _notifySaveState.value?.let { it +1 } ?: 0
        }
    }

    private val _splitter = { src:String -> parser.splitBlocks(src) }

    fun updateBlock(id: Int, blockSrc: String) {
        val newBlocks = blocks.value.update(_splitter, id, blockSrc)
        onBlocksChange(newBlocks)
    }

    fun appendTailBlocks(blockSrc: String) {
        if(blockSrc != "") {
            val newBlocks = blocks.value.appendTail(_splitter, blockSrc)
            onBlocksChange(newBlocks)
        }
    }

    fun updateOpenState(idx: Int, isOpen: Boolean) {
        openState.value = openState.value.mapIndexed { index2, _ -> if(idx==index2) isOpen else false }
        selectedBlock.value = if(isOpen) blocks.value[idx] else emptyBlock
    }

    val isBlockOpen : Boolean
    get() {
        return !selectedBlock.value.isEmpty
    }

    fun closeOpenState() {
        openState.value = openState.value.map { false }
        selectedBlock.value = emptyBlock
    }

    fun openMd(newMd: String) {
        onBlocksChange(BlockList.toBlocks(parser.splitBlocks(newMd)), false)
    }

    fun parseBlock(src: String) = parser.parseBlock(src)
}
