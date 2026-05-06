package apw.android.notes.ui.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import apw.android.notes.ui.theme.APWNotesTheme
import apw.android.notes.user.AuthViewModel
import apw.android.notes.user.SessionViewModel
import apw.android.notes.user.notes.NoteTab
import apw.android.notes.user.notes.NotesViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NotesScreen(
    sessionViewModel: SessionViewModel,
    viewModel: NotesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val userState by sessionViewModel.user.collectAsState()
    val userName: String = userState.name ?: "Guest"

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                NotesHeader(username = userName)
                SearchBar()
                NotesTabRow(selected = NoteTab.NOTES) {

                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {

                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                    contentDescription = "Add new",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { padding ->
        MeshBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {

        }
    }
}

@Composable
fun NotesTabRow(selected: NoteTab, onSelect: (NoteTab) -> Unit) {
    val tabs = listOf(
        Triple(NoteTab.NOTES, Icons.Outlined.Description, "Notes"),
        Triple(NoteTab.TODO, Icons.Outlined.CheckCircle, "To-Do"),
        Triple(NoteTab.PRIVATE, Icons.Outlined.Lock, "Private"),
    )
    val isDark = isSystemInDarkTheme()
    val BgCard = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEach { (tab, icon, label) ->
            val isSelected = selected == tab
            val bgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(0.8f) else BgCard,
                animationSpec = tween(220), label = "tab_bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) Color.DarkGray else Color.Gray),
                animationSpec = tween(220), label = "tab_text"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) Color.Transparent else DividerColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(15.dp))
                Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MeshBackground(modifier: Modifier = Modifier) {

    val colors = MaterialTheme.colorScheme
    val isDark = true

    val baseColor = if (isDark) Color.Black else Color.White

    val glowPrimary = colors.primary.copy(alpha = 0.40f)
    val glowSecondary = colors.secondary.copy(alpha = 0.25f)

    val dotColor = if (isDark) {
        colors.onBackground.copy(alpha = 0.06f)
    } else {
        colors.onBackground.copy(alpha = 0.05f)
    }

    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        drawRect(color = baseColor)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowPrimary, Color.Transparent),
                center = Offset(w * 0.85f, h * 0.08f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.85f, h * 0.08f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowSecondary, Color.Transparent),
                center = Offset(w * 0.1f, h * 0.92f),
                radius = w * 0.4f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.1f, h * 0.92f)
        )

        val dotSpacing = 32.dp.toPx()
        val dotRadius = 1.2.dp.toPx()

        val cols = (w / dotSpacing).toInt() + 1
        val rows = (h / dotSpacing).toInt() + 1

        for (r in 0..rows) {
            for (c in 0..cols) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(c * dotSpacing, r * dotSpacing)
                )
            }
        }
    }
}

@Composable
fun NotesHeader(username: String) {
    val displayName = "Welcome, $username"
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "${sampleNotes.size} notes · ${sampleTodos.count { !it.isDone }} tasks pending",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = if (isDark) 0.85f else 0.7f
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.first().uppercaseChar().toString(),
                color = Color(0xFF1A1A22),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun SearchBar() {
    var query by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()
    val BgCard = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            text = query.ifEmpty { "Search your notes..." },
            color = if (query.isEmpty()) Color.DarkGray else Color.Gray,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.Tune, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewNotes() {
    APWNotesTheme(darkTheme = true) {
        NotesScreen(
            viewModel()
        )
    }
}