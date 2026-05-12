package apw.android.notes.data

import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class EditViewModelFactory(
    private val dao: NotesDAO
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(dao) as T
    }
}

data class EditNotesUIState(
    val noteId: Long? = null,
    val title: String = "",
    val blocks: List<NoteBlock> = listOf(
        NoteBlock.Text(
            id = System.nanoTime(),
            txt = ""
        )
    ),
    val tags: List<String> = emptyList()
)

class EditViewModel(
    private val dao: NotesDAO
) : ViewModel() {

    companion object {
        private const val MAX_UNDO_STACK_SIZE = 100
        private const val SNAPSHOT_THRESHOLD = 5
    }

    private val _uiState = MutableStateFlow(EditNotesUIState())
    val uiState = _uiState.asStateFlow()

    private val _currentBlockId = MutableStateFlow<Long?>(null)
    val currentBlockId = _currentBlockId.asStateFlow()

    private val undoStack = mutableListOf<EditNotesUIState>()
    private val redoStack = mutableListOf<EditNotesUIState>()

    private var lastSnapshotText = ""

    val currentTextStyle: TextStyleState?
        get() {
            val focusedId = _currentBlockId.value ?: return null

            val block = _uiState.value.blocks.find {
                it.id == focusedId
            }

            return (block as? NoteBlock.Text)?.style
        }

    private fun generateId(): Long {
        return System.nanoTime()
    }

    private fun findBlockIndex(id: Long): Int {
        return _uiState.value.blocks.indexOfFirst {
            it.id == id
        }
    }

    private fun pushToUndoStack() {

        undoStack.add(
            _uiState.value.copy(
                blocks = _uiState.value.blocks.toMutableList()
            )
        )

        if (undoStack.size > MAX_UNDO_STACK_SIZE) {
            undoStack.removeAt(0)
        }

        redoStack.clear()
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

    private fun updateBlocksWithoutHistory(
        transform: MutableList<NoteBlock>.() -> Unit
    ) {

        val updated = _uiState.value.blocks.toMutableList()

        updated.transform()

        _uiState.value = _uiState.value.copy(
            blocks = updated
        )
    }

    private fun updateFormat(
        transform: TextStyleState.() -> TextStyleState
    ) {

        val focusedId = _currentBlockId.value ?: return

        val index = findBlockIndex(focusedId)

        if (index == -1) return

        updateBlocks {

            val old = this[index] as? NoteBlock.Text
                ?: return@updateBlocks

            this[index] = old.copy(
                style = old.style.transform()
            )
        }
    }

    fun updateFocusedBlock(id: Long) {
        _currentBlockId.value = id
    }

    fun undo() {

        if (undoStack.isEmpty()) return

        redoStack.add(
            _uiState.value.copy(
                blocks = _uiState.value.blocks.toList()
            )
        )

        _uiState.value = undoStack.removeAt(
            undoStack.lastIndex
        )
    }

    fun redo() {

        if (redoStack.isEmpty()) return

        undoStack.add(
            _uiState.value.copy(
                blocks = _uiState.value.blocks.toList()
            )
        )

        _uiState.value = redoStack.removeAt(
            redoStack.lastIndex
        )
    }

    fun updateTitle(newTitle: String) {

        _uiState.value = _uiState.value.copy(
            title = newTitle
        )
    }

    fun updateTextBlock(
        id: Long,
        txt: String
    ) {

        if (abs(txt.length - lastSnapshotText.length) >= SNAPSHOT_THRESHOLD) {

            pushToUndoStack()

            lastSnapshotText = txt
        }

        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocksWithoutHistory {

            val old = this[index] as NoteBlock.Text

            this[index] = old.copy(
                txt = txt
            )
        }
    }

    fun updateCheckBoxTitle(
        id: Long,
        txt: String
    ) {

        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {

            val old = this[index] as NoteBlock.CheckBox

            this[index] = old.copy(
                txt = txt
            )
        }
    }

    fun updateCheckBoxCheck(
        id: Long,
        isChecked: Boolean
    ) {

        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {

            val old = this[index] as NoteBlock.CheckBox

            this[index] = old.copy(
                isChecked = isChecked
            )
        }
    }

    fun addCheckBoxBlock() {

        updateBlocks {

            add(
                NoteBlock.CheckBox(
                    id = generateId(),
                    txt = "",
                    isChecked = false
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
                index + 1,
                NoteBlock.CheckBox(
                    id = generateId(),
                    txt = "",
                    isChecked = false
                )
            )
        }
    }

    fun insertTextBlockAfter(id: Long) {

        val index = findBlockIndex(id)

        if (index == -1) return

        updateBlocks {

            add(
                index + 1,
                NoteBlock.Text(
                    id = generateId(),
                    txt = ""
                )
            )
        }
    }

    fun addImageBlock(uri: String) {

        updateBlocks {

            add(
                NoteBlock.Image(
                    id = generateId(),
                    uri = uri
                )
            )
        }
    }

    suspend fun saveNote() {

        val current = _uiState.value

        val noteId = current.noteId
            ?: System.currentTimeMillis()

        val noteEntity = NoteEntity(
            id = noteId,
            title = current.title,

            createdAt = if (current.noteId == null) {
                System.currentTimeMillis()
            } else {
                dao.getNoteById(noteId)?.createdAt
                    ?: System.currentTimeMillis()
            },

            updatedAt = System.currentTimeMillis()
        )

        dao.insertNote(noteEntity)

        dao.deleteBlockForNote(noteId)

        val blockEntities = current.blocks.mapIndexed { index, block ->

            block.toRoomEntity(
                noteId = noteId,
                position = index
            )
        }

        dao.insertBlocks(blockEntities)

        dao.deleteBlockForNote(noteId)

        current.tags.forEach { tagName ->
            val cleanTag = tagName.trim().lowercase()
            val currentTag = dao.getTagByName(cleanTag)
            val tagId = if (currentTag != null) {
                currentTag.tagId
            } else {
                val newId = System.currentTimeMillis()
                dao.insertTag(TagEntity(
                    tagId = newId,
                    name = cleanTag
                ))
                newId
            }
            dao.insertTagCrossRef(
                TagCrossRef(
                    noteId = noteId,
                    tagId = tagId
                )
            )
        }

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

    fun toggleBold() {
        updateFormat {
            copy(isBold = !isBold)
        }
    }

    fun toggleItalic() {
        updateFormat {
            copy(isItalic = !isItalic)
        }
    }

    fun toggleUnderline() {
        updateFormat {
            copy(isUnderline = !isUnderline)
        }
    }

    fun toggleStrikeThrough() {
        updateFormat {
            copy(isStrikeThrough = !isStrikeThrough)
        }
    }

    fun toggleHeading() {
        updateFormat {
            copy(isHeading = !isHeading)
        }
    }

    fun updateAlignment(
        align: TextAlign
    ) {
        updateFormat {
            copy(alignment = align)
        }
    }

    fun addTag(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return
        if (cleaned in _uiState.value.tags) return
        _uiState.value = _uiState.value.copy(tags = _uiState.value.tags + cleaned)
    }

    fun removeTag(tag: String) {
        _uiState.value = _uiState.value.copy(tags = _uiState.value.tags - tag)
    }
}