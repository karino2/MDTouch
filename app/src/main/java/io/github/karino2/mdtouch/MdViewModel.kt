package io.github.karino2.mdtouch

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MdViewModel : ViewModel() {
    private val parser = Parser()

    // block edit or full text area edit.
    val isBlockEdit = mutableStateOf(true)

    val blocks = mutableStateOf(emptyList<Block>())

    val fullSrc: String
        get() = blocks.value.joinToString("") { it.src }


    private val _notifySaveState = MutableLiveData<Int>()
    val notifySaveState : LiveData<Int> = _notifySaveState

    private val emptyBlock = Block(-1, "")
    val selectedBlock = mutableStateOf(emptyBlock)

    private fun onBlocksChange(newBlocks: List<Block>, notifySave : Boolean = true) {
        blocks.value = newBlocks
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

    fun updateSelectionState(idx: Int, isOpen: Boolean) {
        selectedBlock.value = if(isOpen) blocks.value[idx] else emptyBlock
    }

    fun openMd(newMd: String) {
        onBlocksChange(BlockList.toBlocks(parser.splitBlocks(newMd)), false)
    }

    fun parseBlock(src: String) = parser.parseBlock(src)
}
