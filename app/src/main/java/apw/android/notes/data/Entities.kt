package apw.android.notes.data

import androidx.compose.ui.text.style.TextAlign
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

fun BlockEntity.toDomain(): NoteBlock {
    return when(type) {
        "text" -> {
            NoteBlock.Text(
                id = id,
                txt = text ?: "",
                style = TextStyleState(
                    isBold = isBold,
                    isItalic = isItalic,
                    isHeading = isHeading,
                    isUnderline = isUnderline,
                    isStrikeThrough = isStrikeThrough,
                    alignment = when(alignment) {
                        "center" -> TextAlign.Center
                        "end" -> TextAlign.End
                        "justify" -> TextAlign.Justify
                        else -> TextAlign.Start
                    }
                )
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

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey (autoGenerate = true)
    val tagId: Long = 0,
    val name: String
)

@Entity(tableName = "tag_cross_ref", primaryKeys = ["noteId", "tagId"])
data class TagCrossRef(
    val noteId: Long,
    val tagId: Long
)

data class NoteWithTags(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(
            value = TagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

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
    val imageUri: String?,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isHeading: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikeThrough: Boolean = false,
    val alignment: String = "Start"
)