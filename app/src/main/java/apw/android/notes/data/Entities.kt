package apw.android.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

fun BlockEntity.toDomain(): NoteBlock {
    return when(type) {
        "text" -> {
            NoteBlock.Text(
                id = id,
                txt = text ?: ""
            )
        }
        "checkbox" -> {
            NoteBlock.CheckBox(
                id = id,
                txt = text ?: "",
                isChecked = isChecked ?: false
            )
        }
        "image" -> {
            NoteBlock.Image(
                id = id,
                uri = imageUri ?: ""
            )
        }
        else -> {
            NoteBlock.Text(
                id = id,
                txt = ""
            )
        }
    }
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey
    val id: Long,
    val noteId: Long,
    val position: Int,
    val type: String,
    val text: String?,
    val isChecked: Boolean?,
    val imageUri: String?
)