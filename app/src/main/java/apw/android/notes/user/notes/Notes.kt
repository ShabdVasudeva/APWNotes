package apw.android.notes.user.notes

data class Note(
    val id: String,
    val blocks: List<NoteBlock>,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false
)

sealed class NoteBlock {
    data class Text(val text: String) : NoteBlock()
    data class Checklist(val items: List<ChecklistItem>) : NoteBlock()
    data class Image(val uri: String) : NoteBlock()
    data class Audio(val uri: String, val duration: Long) : NoteBlock()
}

data class ChecklistItem(
    val text: String,
    val isChecked: Boolean
)