package apw.android.notes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import apw.android.notes.data.EditViewModel
import apw.android.notes.data.EditViewModelFactory
import apw.android.notes.data.NoteBlock
import apw.android.notes.data.NotesDatabase
import apw.android.notes.ui.theme.APWNotesTheme
import kotlinx.coroutines.launch

fun Activity.launchEditNotes(
    noteId: Long? = null
) {
    val intent = Intent(this, EditNoteActivity::class.java)
    noteId?.let {
        intent.putExtra("note_id", it)
    }
    startActivity(intent)
}

class EditNoteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val noteId = intent.getLongExtra("note_id", -1L)
        setContent {
            APWNotesTheme {
                EditNotes(noteId)
            }
        }
    }
}

@Composable
fun EditNotes(noteId: Long) {
    val context: Context = LocalContext.current
    val db: NotesDatabase = NotesDatabase.getDatabase(context)
    val viewModel: EditViewModel = viewModel(
        factory = EditViewModelFactory(dao = db.notesDao())
    )
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    val characterCount = uiState.blocks.sumOf {
        when (it) {
            is NoteBlock.Text -> it.txt.length
            is NoteBlock.CheckBox -> it.txt.length
            is NoteBlock.Image -> 0
        }
    }
    val initText = if (characterCount == 0) "Write something" else "wanna write more ?"

    LaunchedEffect(noteId) {
        if (noteId == null) return@LaunchedEffect
        val note = db.notesDao().getNoteById(noteId) ?: return@LaunchedEffect
        val blocks = db.notesDao().getBlocksForNote(noteId)

        viewModel.loadNote(
            note = note,
            blocks = blocks
        )
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    textStyle = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = {
                        if (uiState.title.isEmpty()) {
                            Text(
                                "Title",
                                fontSize = 26.sp,
                                color = Color.Gray
                            )
                        }
                        it()
                    }
                )
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.viewModelScope.launch { 
                                viewModel.saveNote()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        color = colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            BottomEditBar(
                characterCount = characterCount,
                modifier = Modifier,
                onCheckBoxClick = {
                    viewModel.addCheckBoxBlockAfterFocused()
                }
            ) {
                viewModel.addImageBlock("test")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .padding(start = 5.dp, end = 5.dp)
        ) {
            items(
                items = uiState.blocks,
                key = { it.id }
            ) { block ->
                when (block) {

                    is NoteBlock.Text -> {
                        BasicTextField(
                            value = block.txt,
                            onValueChange = {
                                viewModel.updateTextBlock(block.id, it)
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = colorScheme.onBackground
                            ),
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        viewModel.updateFocusedBlock(id = block.id)
                                    }
                                },
                            decorationBox = {
                                if (block.txt.isEmpty()) {
                                    Text(
                                        initText,
                                        color = Color.LightGray.copy(0.7f),
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .fillMaxWidth()
                                    )
                                }
                                it()
                            }
                        )
                    }

                    is NoteBlock.CheckBox -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (block.isChecked)
                                    Icons.Outlined.CheckCircle
                                else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (block.isChecked)
                                    colorScheme.primary
                                else Color.Gray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        viewModel.updateCheckBoxCheck(block.id, !block.isChecked)
                                    }
                            )

                            Spacer(Modifier.size(10.dp))

                            BasicTextField(
                                value = block.txt,
                                onValueChange = {
                                    if ("\n" in it) {
                                        val cleaned = it.replace(oldValue = "\n", newValue = "")
                                        viewModel.updateCheckBoxTitle(block.id, txt = cleaned)
                                        viewModel.insertTextBlockAfter(id = block.id)
                                    } else {
                                        viewModel.updateCheckBoxTitle(block.id, txt = it)
                                    }
                                },
                                modifier = Modifier
                                    .padding(all = 5.dp)
                                    .fillMaxWidth()
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            viewModel.updateFocusedBlock(id = block.id)
                                        }
                                    },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = colorScheme.onBackground
                                ),
                                decorationBox = {
                                    if (block.txt.isEmpty()) {
                                        Text(
                                            "List item...",
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .padding(all = 5.dp)
                                                .fillMaxWidth()
                                        )
                                    }
                                    it()
                                }
                            )
                        }
                    }

                    is NoteBlock.Image -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.Gray, shape = RoundedCornerShape(12.dp))
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        viewModel.updateFocusedBlock(id = block.id)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image Preview")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BottomEditBar(
    characterCount: Int,
    modifier: Modifier = Modifier,
    onCheckBoxClick: () -> Unit,
    onAddImageClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(vertical = 20.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconButton(
                onClick = {
                    onCheckBoxClick()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Add Todo",
                    modifier = Modifier
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = {
                    onAddImageClick()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                    contentDescription = "Add Todo",
                    modifier = Modifier
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = {
                    onAddImageClick()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Summarize,
                    contentDescription = "Add Todo",
                    modifier = Modifier
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            "$characterCount chars",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewEdit() {
    APWNotesTheme(darkTheme = true) {
        EditNotes(1L)
    }
}