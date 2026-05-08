package apw.android.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        BlockEntity::class
    ],
    version = 1
)
abstract class NotesDatabase: RoomDatabase() {
    abstract fun notesDao(): NotesDAO

    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null

        fun getDatabase(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context = context,
                    NotesDatabase::class.java,
                    name = "notes_db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}