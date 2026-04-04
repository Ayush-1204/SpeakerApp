package com.example.speakerapp.features.enrollment.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.speakerapp.features.enrollment.data.SpeakerListItem
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerListScreen(
    viewModel: SpeakerListViewModel,
    onEnrollNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingSpeaker by remember { mutableStateOf<SpeakerListItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var deletingSpeaker by remember { mutableStateOf<SpeakerListItem?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pendingAvatarUri = uri
    }

    val displayedSpeakers = remember(uiState.speakers, searchQuery, sortAscending) {
        val filtered = uiState.speakers.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
        if (sortAscending) {
            filtered.sortedBy { it.displayName.lowercase() }
        } else {
            filtered.sortedByDescending { it.displayName.lowercase() }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF004650))
                        Text(
                            "SafeEar",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF004650)
                        )
                    }
                },
                actions = {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDWjdvw_HFhJIEVTgwlFUTjNQBSzvmPrQHNSWaZpM00jtGscVXu9XV37UEeK2g9S_lr_mDHQHytcQ28EKWbtQs08TCKYONlCQDODLHT8LiAZi0KTPrYYUVbi0lhN9HmQ7xyt2Tr7df-HadkngwLr2TthC5rf2cFqBkQ_Vo9T5Zn5xo53JIkQl3LJ-OAS1IP4YUBkJConGyg2eDIGi-HRyf1s9y_V7Mx9HoLnuxkLpyCbVaXunHAPCweKxQyY7yeJhHUqaQ6MnQuHynM",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF135F6B).copy(alpha = 0.2f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6FAFA).copy(alpha = 0.8f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEnrollNew,
                containerColor = Color(0xFF004650),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Speaker")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        "SECURITY LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF50686D),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Familiar Speakers",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF181C1D)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SafeEar monitors surroundings for unknown voices. Register family members to prevent false alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3F4949),
                        lineHeight = 20.sp
                    )
                }
            }

            // Search Bar
            item {
                val sortInteraction = remember { MutableInteractionSource() }
                val sortPressed by sortInteraction.collectIsPressedAsState()
                val sortScale by animateFloatAsState(
                    targetValue = if (sortPressed) 0.96f else 1f,
                    label = "sortScale"
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search profiles...", color = Color(0xFF6F7979)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6F7979)) },
                    trailingIcon = {
                        Surface(
                            onClick = { showSortMenu = true },
                            interactionSource = sortInteraction,
                            color = Color.White.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBEC8C9).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .graphicsLayer {
                                    scaleX = sortScale
                                    scaleY = sortScale
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Filter", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = Color.White.copy(alpha = 0.94f),
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBEC8C9).copy(alpha = 0.5f))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort Ascending") },
                                leadingIcon = {
                                    if (sortAscending) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    sortAscending = true
                                    showSortMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color(0xFF181C1D),
                                    leadingIconColor = Color(0xFF004650)
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("Sort Descending") },
                                leadingIcon = {
                                    if (!sortAscending) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    sortAscending = false
                                    showSortMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color(0xFF181C1D),
                                    leadingIconColor = Color(0xFF004650)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F4F4),
                        unfocusedContainerColor = Color(0xFFF0F4F4),
                        focusedBorderColor = Color(0xFF135F6B),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // Enrollment Teaser
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF135F6B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        // Decorative gradient circle (simulated)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 40.dp, y = 40.dp)
                                .size(160.dp)
                                .background(Color(0xFF004650).copy(alpha = 0.2f), CircleShape)
                        )

                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "Improve Recognition",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Register new voice profiles to ensure SafeEar can distinguish between loved ones and strangers instantly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onEnrollNew,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.MicExternalOn, contentDescription = null, tint = Color(0xFF004650))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enroll New Profile", color = Color(0xFF004650), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (uiState.error != null) {
                item { Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error) }
            } else {
                items(displayedSpeakers, key = { it.id }) { speaker ->
                    SpeakerCard(
                        speaker = speaker,
                        profileImageUrl = speaker.profileImageUrl,
                        onEdit = {
                            editingSpeaker = speaker
                            editName = speaker.displayName
                            pendingAvatarUri = null
                        },
                        onDelete = {
                            deletingSpeaker = speaker
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (editingSpeaker != null) {
        AlertDialog(
            onDismissRequest = {
                editingSpeaker = null
                pendingAvatarUri = null
            },
            title = { Text("Edit Speaker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Speaker Name") },
                        singleLine = true
                    )

                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (pendingAvatarUri == null) "Set Profile Pic" else "Change Profile Pic")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val speaker = editingSpeaker ?: return@TextButton
                    val updatedName = editName.trim()
                    if (updatedName.isNotEmpty() && updatedName != speaker.displayName) {
                        viewModel.updateSpeaker(speaker.id, updatedName)
                    }

                    val selectedAvatarUri = pendingAvatarUri
                    if (selectedAvatarUri != null) {
                        scope.launch {
                            val imageFile = runCatching {
                                val input = context.contentResolver.openInputStream(selectedAvatarUri)
                                    ?: throw IllegalStateException("Unable to read selected image")
                                val tempFile = File.createTempFile("speaker_avatar_", ".jpg", context.cacheDir)
                                input.use { inputStream ->
                                    tempFile.outputStream().use { output ->
                                        inputStream.copyTo(output)
                                    }
                                }
                                tempFile
                            }.getOrElse { error ->
                                viewModel.setError(error.message ?: "Failed to read selected image")
                                return@launch
                            }

                            viewModel.updateSpeakerAvatar(speaker.id, imageFile)
                        }
                    }

                    editingSpeaker = null
                    pendingAvatarUri = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingSpeaker = null
                    pendingAvatarUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingSpeaker != null) {
        val speaker = deletingSpeaker!!
        AlertDialog(
            onDismissRequest = { deletingSpeaker = null },
            title = { Text("Delete Speaker") },
            text = { Text("Are you sure want to delete \"${speaker.displayName}\" from familiar speakers?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSpeaker(speaker.id)
                    deletingSpeaker = null
                }) {
                    Text("Delete", color = Color(0xFFBA1A1A))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSpeaker = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SpeakerCard(
    speaker: SpeakerListItem,
    profileImageUrl: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val menuInteraction = remember { MutableInteractionSource() }
    val menuPressed by menuInteraction.collectIsPressedAsState()
    val menuScale by animateFloatAsState(
        targetValue = if (menuPressed) 0.9f else 1f,
        label = "menuScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color(0xFFE5E9E9)
                ) {
                    if (!profileImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Speaker",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = Color(0xFF6F7979)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFF9DF898), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF002204))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    speaker.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF181C1D)
                )
                Text(
                    "Registered ${speaker.sampleCount} voice ${if (speaker.sampleCount == 1) "sample" else "samples"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6F7979)
                )
            }
            Box {
                Surface(
                    onClick = { showMenu = true },
                    interactionSource = menuInteraction,
                    shape = CircleShape,
                    color = Color(0xFFF0F4F4).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDFE3E3)),
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = menuScale
                            scaleY = menuScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color(0xFF50686D))
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = Color.White.copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBEC8C9).copy(alpha = 0.5f))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFF181C1D),
                            leadingIconColor = Color(0xFF50686D)
                        )
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFBA1A1A)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFF181C1D),
                            leadingIconColor = Color(0xFFBA1A1A)
                        )
                    )
                }
            }
        }
    }
}
