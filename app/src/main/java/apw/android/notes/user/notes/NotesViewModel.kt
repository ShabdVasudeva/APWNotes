package apw.android.notes.user.notes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

enum class NoteTab { NOTES, TODO, PRIVATE }
data class TodoItem(
    val id: Int,
    val text: String,
    val isDone: Boolean
)
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val selectedTab: NoteTab = NoteTab.NOTES,
    val username: String? = null
)

class NotesViewModel : ViewModel() {
    private val _state = MutableStateFlow(NotesUiState())
    val state = _state

    fun selectTab(tab: NoteTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }
}