package apw.android.notes.data

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

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
                imageUri = null,
                isBold = style.isBold,
                isItalic = style.isItalic,
                isHeading = style.isHeading,
                isUnderline = style.isUnderline,
                isStrikeThrough = style.isStrikeThrough,
                alignment = when(style.alignment) {
                    TextAlign.Center -> "center"
                    TextAlign.End -> "end"
                    TextAlign.Justify -> "justify"
                    else -> "start"
                }
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

data class TextStyleState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikeThrough: Boolean = false,
    val alignment: TextAlign = TextAlign.Start,
    val isHeading: Boolean = false
)

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
        val txt: String,
        val style: TextStyleState = TextStyleState()
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