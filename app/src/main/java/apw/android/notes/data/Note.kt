package apw.android.notes.data

fun NoteBlock.toRoomEntity(
    noteId: Long,
    position: Int
): BlockEntity {
    return when(this) {
        is NoteBlock.Text -> {
            BlockEntity(
                id = id,
                noteId = noteId,
                position = position,
                type = "text",
                text = txt,
                isChecked = null,
                imageUri = null
            )
        }
        is NoteBlock.CheckBox -> {
            BlockEntity(
                id = id,
                noteId = noteId,
                position = position,
                type = "checkbox",
                text = txt,
                isChecked = isChecked,
                imageUri = null
            )
        }
        is NoteBlock.Image -> {
            BlockEntity(
                id = id,
                noteId = noteId,
                position = position,
                type = "image",
                text = null,
                isChecked = null,
                imageUri = uri
            )
        }
    }
}

data class Note(
    val id: String,
    val title: String,
    val blocks: List<NoteBlock>,
    val createdAt: Long,
    val updatedAt: Long,
)

sealed class NoteBlock {
    abstract val id: Long
    data class Text(
        override val id: Long,
        val txt: String
    ): NoteBlock()
    data class CheckBox(
        override val id: Long,
        val txt: String,
        val isChecked: Boolean
    ): NoteBlock()
    data class Image(
        override val id: Long
        ,val uri: String
    ): NoteBlock()
}