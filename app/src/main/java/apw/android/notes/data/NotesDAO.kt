package apw.android.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Query(value = "DELETE FROM blocks WHERE noteId = :noteId")
    suspend fun deleteBlockForNote(noteId: Long)

    @Query(value = "SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query(value = "SELECT * FROM blocks WHERE noteId = :noteId ORDER BY position ASC")
    suspend fun getBlocksForNote(noteId: Long): List<BlockEntity>

    @Query(value = "SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?
}