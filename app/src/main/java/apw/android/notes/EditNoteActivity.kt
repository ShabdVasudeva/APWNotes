package apw.android.notes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import kotlin.collections.mutableMapOf

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
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
    val editorTextStyle = TextStyle(
        fontSize = 16.sp,
        color = colorScheme.onBackground,
        lineHeight = 22.sp
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val focusRequesters = remember { mutableMapOf<Long, FocusRequester>() }

    LaunchedEffect(noteId) {
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
            TopAppBar(
                title = uiState.title,
                onTitleChange = { viewModel.updateTitle(it) },
                onSaveClick = { viewModel.viewModelScope.launch { viewModel.saveNote() } },
                onBackClick = {(context as Activity).finish()},
                modifier = Modifier.statusBarsPadding(),
                undo = { viewModel.undo() },
                redo = { viewModel.redo() }
            )
        },
        bottomBar = {
            BottomEditBar(
                characterCount = characterCount,
                modifier = Modifier.imePadding(),
                isBold = (viewModel.currentTextStyle?.isBold == true),
                isHeading = (viewModel.currentTextStyle?.isHeading == true),
                isUnderline = (viewModel.currentTextStyle?.isUnderline == true),
                isStrikeThrough = (viewModel.currentTextStyle?.isStrikeThrough == true),
                onToggleBold = { viewModel.toggleBold() },
                onCheckBoxClick = { viewModel.addCheckBoxBlockAfterFocused() },
                onAddImageClick = { viewModel.addImageBlock("") },
                onTagClick = {},
                onVoiceClick = {},
                isItalic = (viewModel.currentTextStyle?.isItalic == true),
                onToggleItalic = { viewModel.toggleItalic() },
                isCenter = (viewModel.currentTextStyle?.alignment == TextAlign.Center),
                toggleAlignment = { viewModel.updateAlignment(it) },
                toggleHeading = { viewModel.toggleHeading() },
                toggleUnderline = { viewModel.toggleUnderline() },
                toggleStrikeThrough = { viewModel.toggleStrikeThrough() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        val focusedId =
                            viewModel.currentBlockId.value ?: uiState.blocks.lastOrNull()?.id

                        focusedId?.let {
                            focusRequesters[it]?.requestFocus()
                        }
                        keyboardController?.show()
                    }
                }
                .padding(paddingValues = innerPadding)
                .padding(start = 5.dp, end = 5.dp)
        ) {
            items(
                items = uiState.blocks,
                key = { it.id }
            ) { block ->
                when (block) {

                    is NoteBlock.Text -> {

                        val isFirstBlock =
                            uiState.blocks.firstOrNull()?.id == block.id

                        val focusRequester: FocusRequester = remember(block.id) {
                            FocusRequester()
                        }
                        val fontWeight = when {
                            block.style.isHeading -> FontWeight.SemiBold
                            block.style.isBold -> FontWeight.Bold
                            else -> FontWeight.Normal
                        }
                        val textDecoration = when {
                            block.style.isUnderline -> TextDecoration.Underline
                            block.style.isStrikeThrough -> TextDecoration.LineThrough
                            else -> TextDecoration.None
                        }

                        val editorTextStyle = editorTextStyle.copy(
                            fontWeight = fontWeight,
                            fontStyle = if (block.style.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textAlign = block.style.alignment,
                            fontSize = if (block.style.isHeading) 30.sp else 16.sp,
                            lineHeight = if (block.style.isHeading) 38.sp else 28.sp,
                            textDecoration = textDecoration
                        )

                        focusRequesters[block.id] = focusRequester
                        BasicTextField(
                            value = block.txt,
                            onValueChange = {
                                if ("\n" in it) {
                                    val cleaned = it.replace("\n", "")
                                    viewModel.updateTextBlock(
                                        block.id,
                                        cleaned
                                    )
                                    viewModel.insertTextBlockAfter(block.id)
                                } else {
                                    viewModel.updateTextBlock(
                                        block.id,
                                        it
                                    )
                                }
                            },
                            textStyle = editorTextStyle,
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        viewModel.updateFocusedBlock(id = block.id)
                                        keyboardController?.show()
                                    }
                                },
                            decorationBox = {
                                if (block.txt.isEmpty()) {
                                    Text(
                                        if(block.txt.isEmpty() && isFirstBlock) "write something" else "",
                                        color = Color.LightGray.copy(0.7f),
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        style = editorTextStyle
                                    )
                                }
                                it()
                            }
                        )
                    }

                    is NoteBlock.CheckBox -> {
                        val focusRequester: FocusRequester = remember(block.id) {
                            FocusRequester()
                        }

                        focusRequesters[block.id] = focusRequester
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = if (block.isChecked)
                                    painterResource(R.drawable.checkbox_filled)
                                else painterResource(R.drawable.checkbox),
                                contentDescription = null,
                                tint = if (block.isChecked)
                                    colorScheme.primary
                                else Color.Gray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.updateCheckBoxCheck(block.id, !block.isChecked)
                                    }
                            )

                            Spacer(Modifier.size(3.dp))

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
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            viewModel.updateFocusedBlock(id = block.id)
                                            keyboardController?.show()
                                        }
                                    },
                                textStyle = editorTextStyle,
                                decorationBox = {
                                    if (block.txt.isEmpty()) {
                                        Text(
                                            "List item...",
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .padding(all = 5.dp)
                                                .fillMaxWidth(),
                                            style = editorTextStyle
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
                                        keyboardController?.hide()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image Preview")
                        }
                        viewModel.insertTextBlockAfter(id = block.id)
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

    isBold: Boolean,
    isItalic: Boolean,

    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,

    onCheckBoxClick: () -> Unit,
    onAddImageClick: () -> Unit,

    onTagClick: () -> Unit,
    onVoiceClick: () -> Unit,

    isCenter: Boolean,
    toggleAlignment: (TextAlign) -> Unit,
    toggleHeading: () -> Unit,
    toggleUnderline: () -> Unit,
    toggleStrikeThrough: () -> Unit,
    isUnderline: Boolean,
    isStrikeThrough: Boolean,
    isHeading: Boolean
) {

    val colorScheme = MaterialTheme.colorScheme

    var isFormattingExpanded by remember {
        mutableStateOf(false)
    }

    val items = listOf(
        FormatItem(
            icon = R.drawable.bold,
            isSelected = isBold,
            contentDescription = "Bold",
            onClick = onToggleBold
        ),
        FormatItem(
            icon = R.drawable.italics,
            isSelected = isItalic,
            contentDescription = "Italic",
            onClick = onToggleItalic
        ),
        FormatItem(
            icon = R.drawable.align_center,
            isSelected = isCenter,
            contentDescription = "Align center",
            onClick = {
                toggleAlignment(if (isCenter) TextAlign.Start else TextAlign.Center)
            }
        ),
        FormatItem(
            icon = R.drawable.heading,
            isSelected = isHeading,
            contentDescription = "Heading",
            onClick = toggleHeading
        ),
        FormatItem(
            icon = R.drawable.underline,
            isSelected = isUnderline,
            contentDescription = "Underline",
            onClick = toggleUnderline
        ),
        FormatItem(
            icon = R.drawable.strike_through,
            isSelected = isStrikeThrough,
            contentDescription = "Stricked Text",
            onClick = toggleStrikeThrough
        ),
        FormatItem(
            icon = R.drawable.list,
            contentDescription = "Formated list",
            onClick = {}
        ),
        FormatItem(
            icon = R.drawable.number_list,
            contentDescription = "Number list",
            onClick = {}
        ),
        FormatItem(
            icon = R.drawable.quote_block,
            contentDescription = "Quote",
            onClick = {}
        ),
        FormatItem(
            icon = R.drawable.code_block,
            contentDescription = "Code block",
            onClick = {}
        ),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
        ) {
            AnimatedVisibility(
                visible = isFormattingExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items) { item ->
                        if (item.isSelected != null) {
                            BottomBarToggleButton(
                                icon = item.icon,
                                contentDescription = item.contentDescription,
                                isSelected = item.isSelected,
                                onClick = item.onClick
                            )
                        } else {
                            BottomBarActionButton(
                                icon = item.icon,
                                contentDescription = item.contentDescription,
                                onClick = item.onClick
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        BottomBarActionButton(
                            icon = R.drawable.checkbox,
                            contentDescription = "Checklist",
                            onClick = onCheckBoxClick
                        )
                    }

                    item {

                        BottomBarActionButton(
                            icon = R.drawable.image,
                            contentDescription = "Image",
                            onClick = onAddImageClick
                        )
                    }

                    item {

                        BottomBarActionButton(
                            icon = R.drawable.summarize_alt,
                            contentDescription = "Ai Summary",
                            onClick = onTagClick
                        )
                    }

                    item {

                        BottomBarActionButton(
                            icon = R.drawable.writer,
                            contentDescription = "Ai Writer",
                            onClick = onVoiceClick
                        )
                    }

                    item {

                        BottomBarToggleButton(
                            icon = R.drawable.formatting,
                            contentDescription = "Formatting",
                            isSelected = isFormattingExpanded
                        ) {

                            isFormattingExpanded =
                                !isFormattingExpanded
                        }
                    }
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(18.dp)
                        .padding(horizontal = 10.dp),
                    color = colorScheme.outlineVariant
                )

                Text(
                    text = "$characterCount chars",
                    fontSize = 12.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@Composable
private fun BottomBarActionButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = colorScheme.onSurface,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun BottomBarToggleButton(
    icon: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor =
        if (isSelected)
            colorScheme.primary.copy(alpha = 0.18f)
        else
            colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)

    val iconTint =
        if (isSelected)
            colorScheme.primary
        else
            colorScheme.onSurface

    val scale by animateFloatAsState(
        if (isSelected) 1.08f else 1f,
        label = ""
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .scale(scale)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun TopAppBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    undo: () -> Unit,
    redo: () -> Unit,
    modifier: Modifier = Modifier
) {

    val tagsList = listOf(
        "#ideas", "#android"
    )

    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp, vertical = 12.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                        )
                        .clickable {
                            onBackClick()
                        }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onBackground,
                        letterSpacing = (-0.3).sp
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->

                        if (title.isEmpty()) {
                            Text(
                                text = "Untitled",
                                color = colorScheme.onSurface.copy(alpha = 0.35f),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        innerTextField()
                    })

                Spacer(Modifier.width(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                            )
                            .clickable {

                            }, contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pin),
                            contentDescription = "Pin",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(0.6f)
                                    )
                                )
                            )
                            .clickable {
                                onSaveClick()
                            }
                            .padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Icon(
                                painter = painterResource(R.drawable.save),
                                contentDescription = "Save",
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = "Save",
                                color = colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tagsList) { item ->
                        SuggestionChip(onClick = {}, label = {
                            Text(text = item)
                        })
                    }
                    item {
                        SuggestionChip(onClick = {}, label = {
                            Text(text = "+ Add")
                        })
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 10.dp),
                    color = colorScheme.outlineVariant
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            undo()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.undo),
                            contentDescription = "undo"
                        )
                    }
                    IconButton(
                        onClick = {
                            redo()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.redo),
                            contentDescription = "redo"
                        )
                    }
                }
            }
        }
    }
}

data class FormatItem(
    val icon: Int,
    val isSelected: Boolean? = null,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Preview(showSystemUi = true)
@Composable
fun PreviewEdit() {
    APWNotesTheme(darkTheme = true) {
        EditNotes(1L)
    }
}