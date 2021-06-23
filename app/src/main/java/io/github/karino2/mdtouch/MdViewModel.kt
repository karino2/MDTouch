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

    fun updateBlocks(newBlocks: List<Block>) {
        _blocks.value = newBlocks
        _openState.value = newBlocks.map { false }
    }

    fun updateOpenState(idx: Int, isOpen: Boolean) {
        _openState.value = _openState.value!!.mapIndexed { index2, _ -> if(idx==index2) isOpen else false }
    }

    fun updateMd(newMd: String) {
        updateBlocks(BlockList.toBlocks(parser.splitBlocks(newMd)))
    }
}
