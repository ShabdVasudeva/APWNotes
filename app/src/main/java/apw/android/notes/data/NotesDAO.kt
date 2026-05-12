package apw.android.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRef(crossRef: TagCrossRef)

    @Query(value = "DELETE FROM blocks WHERE noteId = :noteId")
    suspend fun deleteBlockForNote(noteId: Long)

    @Query(value = "DELETE FROM tag_cross_ref WHERE tagId = :noteId")
    suspend fun deleteTagForNote(noteId: Long)

//    @Query(value = "SELECT * FROM notes ORDER BY updatedAt DESC")
//    fun getAllNotes(): Flow<List<NoteEntity>>

    @Transaction
    @Query(value = "SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteWithTags>>

    @Query(value = "SELECT * FROM blocks WHERE noteId = :noteId ORDER BY position ASC")
    suspend fun getBlocksForNote(noteId: Long): List<BlockEntity>

    @Query(value = "SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query(value = "SELECT * FROM tags WHERE name = :tagName LIMIT 1")
    suspend fun getTagByName(tagName: String): TagEntity?
}