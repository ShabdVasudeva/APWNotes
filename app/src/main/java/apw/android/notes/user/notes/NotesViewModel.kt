package apw.android.notes.user.notes

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import apw.android.notes.data.NotesDAO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class NotesViewModelFactory(
    private val dao: NotesDAO
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotesViewModel(dao) as T
    }
}

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

class NotesViewModel(
    private val dao: NotesDAO
) : ViewModel() {
    private val _state = MutableStateFlow(NotesUiState())
    val state = _state

    val notes = dao.getAllNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(500),
        initialValue = emptyList()
    )

    fun selectTab(tab: NoteTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }
}