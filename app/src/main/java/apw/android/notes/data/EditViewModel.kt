package apw.android.notes.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditViewModelFactory(
    private val dao: NotesDAO
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(dao) as T
    }
}


data class EditNotesUIState(
    val noteId: Long? = null, val title: String = "", val blocks: List<NoteBlock> = listOf(
        NoteBlock.Text(
            id = System.nanoTime(), txt = ""
        )
    )
)

class EditViewModel(private val dao: NotesDAO) : ViewModel() {

    private val _uiState = MutableStateFlow(EditNotesUIState())
    val uiState = _uiState.asStateFlow()
    private val _currentBlockId = MutableStateFlow<Long?>(null)
    val currentBlockId = _currentBlockId.asStateFlow()

    private fun generateId(): Long {
        return System.nanoTime()
    }

    fun updateFocusedBlock(id: Long) {
        _currentBlockId.value = id
    }

    private fun updateBlocks(
        transform: MutableList<NoteBlock>.() -> Unit
    ) {
        val updated = _uiState.value.blocks.toMutableList()
        updated.transform()

        _uiState.value = _uiState.value.copy(
            blocks = updated
        )
    }

    private fun findBlockIndex(id: Long): Int {
        return _uiState.value.blocks.indexOfFirst { it.id == id }
    }

    fun addCheckBoxBlock() {
        updateBlocks {
            add(
                NoteBlock.CheckBox(
                    id = generateId(), txt = "", isChecked = false
                )
            )
        }
    }

    fun addCheckBoxBlockAfterFocused() {
        val focusedId = _currentBlockId.value
        if (focusedId == null) {
            addCheckBoxBlock()
            return
        }

        val index = findBlockIndex(focusedId)
        if (index == -1) return

        updateBlocks {
            add(
                index + 1, NoteBlock.CheckBox(
                    id = generateId(), txt = "", isChecked = false
                )
            )
        }
    }

    fun insertTextBlockAfter(id: Long) {
        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {
            add(
                index + 1, NoteBlock.Text(
                    id = generateId(), txt = ""
                )
            )
        }
    }

    fun addImageBlock(uri: String) {
        updateBlocks {
            add(
                NoteBlock.Image(
                    id = generateId(), uri = uri
                )
            )
        }
    }

    fun updateTextBlock(id: Long, txt: String) {
        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {
            val old = this[index] as NoteBlock.Text

            this[index] = old.copy(
                txt = txt
            )
        }
    }

    fun updateCheckBoxCheck(id: Long, isChecked: Boolean) {
        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {
            val old = this[index] as NoteBlock.CheckBox

            this[index] = old.copy(
                isChecked = isChecked
            )
        }
    }

    fun updateCheckBoxTitle(id: Long, txt: String) {
        val index = findBlockIndex(id)
        if (index == -1) return
        updateBlocks {
            val old = this[index] as NoteBlock.CheckBox

            this[index] = old.copy(
                txt = txt
            )
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle
        )
    }

    suspend fun saveNote() {
        val current = _uiState.value
        val noteId = current.noteId ?: System.currentTimeMillis()
        val noteEntity: NoteEntity = NoteEntity(
            id = noteId,
            title = current.title,
            createdAt = if(current.noteId == null) System.currentTimeMillis() else dao.getNoteById(noteId)?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertNote(noteEntity)
        dao.deleteBlockForNote(noteId)

        val blockEntity: List<BlockEntity> = current.blocks.mapIndexed { index, block ->
            block.toRoomEntity(
                noteId = noteId,
                position = index
            )
        }

        dao.insertBlocks(blockEntity)

        _uiState.value = current.copy(
            noteId = noteId
        )
    }

    fun loadNote(
        note: NoteEntity,
        blocks: List<BlockEntity>
    ) {
        _uiState.value = EditNotesUIState(
            noteId = note.id,
            title = note.title,
            blocks = blocks
                .sortedBy { it.position }
                .map { it.toDomain() }
        )
    }
}