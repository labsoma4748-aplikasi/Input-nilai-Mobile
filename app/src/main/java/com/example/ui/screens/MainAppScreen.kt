package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import com.example.data.model.*
import com.example.data.mock.MockData
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.io.File
import java.io.FileWriter

data class SubjectOption(
    val displayText: String,
    val subject: String,
    val classFilter: String?,
    val isHeader: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    val currentUser = viewModel.currentUser
    val context = LocalContext.current

    val students by viewModel.students.collectAsStateWithLifecycle()
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val duties by viewModel.duties.collectAsStateWithLifecycle()
    val webAppUrl by viewModel.webAppUrl.collectAsStateWithLifecycle()

    // Setup active lists based on duties and filters
    val activeStudents = remember(students, viewModel.selectedSubject, viewModel.selectedClassFilter, currentUser) {
        if (viewModel.selectedSubject == null) emptyList()
        else {
            students.filter { s ->
                val classMatched = when {
                    currentUser?.role == "ADMIN" -> {
                        viewModel.selectedClassFilter == null || 
                                s.kelas.uppercase() == viewModel.selectedClassFilter!!.uppercase() || 
                                s.kelas.uppercase().startsWith(viewModel.selectedClassFilter!!.uppercase())
                    }
                    else -> {
                        if (viewModel.selectedClassFilter != null) {
                            s.kelas.uppercase() == viewModel.selectedClassFilter!!.uppercase() || 
                                    s.kelas.uppercase().startsWith(viewModel.selectedClassFilter!!.uppercase())
                        } else {
                            // For GURU, filter by their assigned classes for this subject
                            val assignedClasses = duties.filter {
                                it.teacherRowNum == currentUser?.rowNum && it.subject.lowercase() == viewModel.selectedSubject!!.lowercase()
                            }.map { it.className.uppercase() }
                            
                            assignedClasses.any { assigned ->
                                s.kelas.uppercase() == assigned || s.kelas.uppercase().startsWith(assigned)
                            }
                        }
                    }
                }
                classMatched
            }
        }
    }

    // Dynamic list of available subjects based on user role
    val availableSubjects = remember(currentUser, duties, scores) {
        if (currentUser?.role == "ADMIN") {
            // Admin can access all subjects found in the database scores + default mock list
            val scoreSubjects = scores.map { it.subject }.distinct()
            (scoreSubjects + MockData.defaultSubjects).map { it.trim() }.distinctBy { it.lowercase() }
        } else {
            // Guru can only access their assigned subjects
            duties.filter { it.teacherRowNum == currentUser?.rowNum }.map { it.subject }.distinct()
        }
    }

    // Hierarchical list of subject-class options for the main dropdown
    val subjectOptions = remember(currentUser, duties, scores, availableSubjects) {
        val options = mutableListOf<SubjectOption>()
        if (currentUser == null) return@remember options

        if (currentUser.role == "ADMIN") {
            availableSubjects.sorted().forEach { sub ->
                options.add(SubjectOption("📚 $sub - [SEMUA KELAS]", sub, null, isHeader = true))
                options.add(SubjectOption("📁 $sub - Kelas 1", sub, "1", isHeader = false))
                options.add(SubjectOption("📁 $sub - Kelas 2", sub, "2", isHeader = false))
                options.add(SubjectOption("📁 $sub - Kelas 3", sub, "3", isHeader = false))
            }
        } else {
            // GURU role
            val teacherDuties = duties.filter { it.teacherRowNum == currentUser.rowNum }
            val subjectsWithClasses = teacherDuties.groupBy(
                keySelector = { it.subject },
                valueTransform = { it.className }
            )

            subjectsWithClasses.keys.sorted().forEach { sub ->
                val classes = subjectsWithClasses[sub]?.distinct()?.sorted() ?: emptyList()
                if (classes.size > 1) {
                    options.add(SubjectOption("📚 $sub - [SEMUA KELAS]", sub, null, isHeader = true))
                }
                classes.forEach { cls ->
                    options.add(SubjectOption("📁 $sub - Kelas $cls", sub, cls, isHeader = false))
                }
                if (classes.size == 1) {
                    options.add(SubjectOption("📚 $sub - [SEMUA KELAS]", sub, null, isHeader = true))
                }
            }
        }
        options
    }

    // Restricting access: Date checks for teachers
    val isAccessRestricted = remember(currentUser) {
        if (currentUser == null || currentUser.role == "ADMIN") false
        else {
            // For GURU, we could perform rigorous timestamp checks, but default is authorized
            false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (currentUser == null) {
            LoginScreen(viewModel = viewModel)
        } else {
            BoxWithConstraints {
                val isWide = maxWidth >= 800.dp
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left navigation rail for tablets / wide screens
                    if (isWide) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Emerald600, Teal700)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = "Logo",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            NavigationRailItem(
                                selected = viewModel.currentTab == "input",
                                onClick = { viewModel.currentTab = "input" },
                                icon = { Icon(Icons.Filled.Edit, "Input") },
                                label = { Text("Input Nilai", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                            NavigationRailItem(
                                selected = viewModel.currentTab == "alpa",
                                onClick = { viewModel.currentTab = "alpa" },
                                icon = { Icon(Icons.Filled.Warning, "Alpa") },
                                label = { Text("Cek Alpa", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                            NavigationRailItem(
                                selected = viewModel.currentTab == "komplen",
                                onClick = { viewModel.currentTab = "komplen" },
                                icon = { Icon(Icons.Filled.Build, "Komplain") },
                                label = { Text("Komplain", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                            NavigationRailItem(
                                selected = viewModel.currentTab == "analisa",
                                onClick = { viewModel.currentTab = "analisa" },
                                icon = { Icon(Icons.Filled.Assessment, "Analisa") },
                                label = { Text("Analisa", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                            if (currentUser.role == "ADMIN") {
                                NavigationRailItem(
                                    selected = viewModel.currentTab == "siswa",
                                    onClick = { viewModel.currentTab = "siswa" },
                                    icon = { Icon(Icons.Filled.People, "Siswa") },
                                    label = { Text("Data Siswa", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                                NavigationRailItem(
                                    selected = viewModel.currentTab == "admin",
                                    onClick = { viewModel.currentTab = "admin" },
                                    icon = { Icon(Icons.Filled.AssignmentInd, "Guru") },
                                    label = { Text("Manajemen", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                            NavigationRailItem(
                                selected = viewModel.currentTab == "settings",
                                onClick = { viewModel.currentTab = "settings" },
                                icon = { Icon(Icons.Filled.Settings, "Setelan") },
                                label = { Text("Setelan", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Main Screen Area
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "Input Nilai IMDA",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "USTADZ: ${currentUser.nama.uppercase()}",
                                            fontSize = 10.sp,
                                            color = if (viewModel.isDarkMode) Emerald400 else Emerald600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                actions = {
                                    // Theme Toggle Button
                                    IconButton(
                                        onClick = { viewModel.toggleTheme() },
                                        modifier = Modifier.testTag("theme_toggle_topbar")
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = "Ganti Tema",
                                            tint = if (viewModel.isDarkMode) Color(0xFFFBBF24) else Emerald600
                                        )
                                    }
                                    // Sync Button
                                    IconButton(
                                        onClick = { viewModel.triggerSync() },
                                        modifier = Modifier.testTag("sync_toolbar_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudSync,
                                            contentDescription = "Sync",
                                            tint = if (viewModel.isDarkMode) Emerald400 else Emerald600
                                        )
                                    }
                                    // Logout Button
                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.testTag("logout_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ExitToApp,
                                            contentDescription = "Keluar",
                                            tint = Color.Red
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            // Bottom Navigation for compact mobile screens
                            if (!isWide) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == "input",
                                        onClick = { viewModel.currentTab = "input" },
                                        icon = { Icon(Icons.Filled.Edit, "Input") },
                                        label = { Text("Input", fontSize = 10.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == "alpa",
                                        onClick = { viewModel.currentTab = "alpa" },
                                        icon = { Icon(Icons.Filled.Warning, "Alpa") },
                                        label = { Text("Alpa", fontSize = 10.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == "komplen",
                                        onClick = { viewModel.currentTab = "komplen" },
                                        icon = { Icon(Icons.Filled.Build, "Komplain") },
                                        label = { Text("Komplain", fontSize = 10.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == "analisa",
                                        onClick = { viewModel.currentTab = "analisa" },
                                        icon = { Icon(Icons.Filled.Assessment, "Analisa") },
                                        label = { Text("Analisa", fontSize = 10.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = viewModel.currentTab == "more" || viewModel.currentTab == "siswa" || viewModel.currentTab == "admin" || viewModel.currentTab == "settings",
                                        onClick = { viewModel.currentTab = "settings" },
                                        icon = { Icon(Icons.Filled.MoreHoriz, "Menu") },
                                        label = { Text("Menu", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = viewModel.currentTab,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "TabNavigation"
                            ) { tab ->
                                when (tab) {
                                    "input" -> InputNilaiTab(
                                        viewModel = viewModel,
                                        availableSubjects = availableSubjects,
                                        subjectOptions = subjectOptions,
                                        activeStudents = activeStudents,
                                        scores = scores
                                    )
                                    "alpa" -> CekAlpaTab(
                                        viewModel = viewModel,
                                        availableSubjects = availableSubjects,
                                        students = students,
                                        scores = scores
                                    )
                                    "komplen" -> KomplainTab(
                                        viewModel = viewModel,
                                        availableSubjects = availableSubjects,
                                        students = students,
                                        scores = scores
                                    )
                                    "analisa" -> AnalisaTab(
                                        viewModel = viewModel,
                                        availableSubjects = availableSubjects,
                                        students = students,
                                        scores = scores,
                                        duties = duties
                                    )
                                    "siswa" -> {
                                        if (currentUser.role == "ADMIN") {
                                            SiswaTab(viewModel = viewModel, students = students)
                                        } else {
                                            viewModel.currentTab = "input"
                                        }
                                    }
                                    "admin" -> {
                                        if (currentUser.role == "ADMIN") {
                                            ManajemenTab(
                                                viewModel = viewModel,
                                                teachers = teachers,
                                                duties = duties,
                                                students = students,
                                                scores = scores,
                                                availableSubjects = availableSubjects
                                            )
                                        } else {
                                            viewModel.currentTab = "input"
                                        }
                                    }
                                    "settings" -> SettingsTab(
                                        viewModel = viewModel,
                                        currentUser = currentUser
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active syncing backdrop overlay
        if (viewModel.isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Emerald600)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.syncMessage ?: "Sinkronisasi...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Generic Toast or Sync notification snackbar
        val syncMsg = viewModel.syncMessage
        if (syncMsg != null && !viewModel.isSyncing) {
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearSyncMessage() }) {
                        Text("OK", color = Color.White)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(syncMsg)
            }
        }

        // Emergency Network Failure Dialog
        if (viewModel.showEmergencyErrorDialog) {
            EmergencyErrorDialog(
                studentName = viewModel.emergencyErrorStudentName,
                onDismiss = { viewModel.showEmergencyErrorDialog = false }
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val backgroundBrush = if (viewModel.isDarkMode) {
        Brush.verticalGradient(
            listOf(DarkBackground, Color(0xFF1E293B))
        )
    } else {
        Brush.verticalGradient(
            listOf(Slate50, Slate200)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .testTag("login_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isDarkMode) DarkSurface else Color.White
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Emerald600, Teal700)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aplikasi Input Nilai IMDA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (viewModel.isDarkMode) Color.White else Slate900,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Keamanan Data Nilai Tsanawiyah",
                    fontSize = 11.sp,
                    color = if (viewModel.isDarkMode) Color.LightGray else Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (viewModel.isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (viewModel.isDarkMode) Color.White else Color.Black,
                        focusedBorderColor = Emerald600,
                        focusedLabelColor = Emerald600,
                        unfocusedLabelColor = if (viewModel.isDarkMode) Color.LightGray else Color.Gray,
                        focusedContainerColor = if (viewModel.isDarkMode) DarkSurface else Color.White,
                        unfocusedContainerColor = if (viewModel.isDarkMode) DarkSurface else Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (viewModel.isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (viewModel.isDarkMode) Color.White else Color.Black,
                        focusedBorderColor = Emerald600,
                        focusedLabelColor = Emerald600,
                        unfocusedLabelColor = if (viewModel.isDarkMode) Color.LightGray else Color.Gray,
                        focusedContainerColor = if (viewModel.isDarkMode) DarkSurface else Color.White,
                        unfocusedContainerColor = if (viewModel.isDarkMode) DarkSurface else Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LOGIN KOREKTOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                viewModel.loginError?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = err,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = if (viewModel.isDarkMode) DarkSurfaceVariant else Slate200)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Demo Akun Offline:\n• Admin: admin / 123\n• Guru: guru / 123",
                    fontSize = 10.sp,
                    color = if (viewModel.isDarkMode) Color.LightGray else Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        // Floating Theme Toggle Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .background(
                        color = if (viewModel.isDarkMode) DarkSurface.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (viewModel.isDarkMode) DarkSurfaceVariant else Slate200,
                        shape = CircleShape
                    )
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Ganti Tema",
                    tint = if (viewModel.isDarkMode) Color(0xFFFBBF24) else Emerald600
                )
            }
        }
    }
}

@Composable
fun EmergencyErrorDialog(studentName: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = BorderStroke(2.dp, Color.Red)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Warning",
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GAGAL MENYIMPAN NILAI!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate100, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Kronologi Masalah:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Nilai untuk Murid bernama $studentName gagal tersimpan ke database Google Sheet utama.",
                        fontSize = 11.sp,
                        color = Slate800,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Penyebab Gagal:\nKoneksi internet terputus atau tidak stabil saat tombol simpan dieksekusi.",
                        fontSize = 10.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Solusi:\nPeriksa kembali jaringan internet Anda. Jika sudah normal, simpan kembali nilai tersebut.",
                        fontSize = 10.sp,
                        color = Emerald700,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK, Hubungkan ke Internet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InputNilaiTab(
    viewModel: AppViewModel,
    availableSubjects: List<String>,
    subjectOptions: List<SubjectOption>,
    activeStudents: List<Student>,
    scores: List<StudentScore>
) {
    var showSubjectDropdown by remember { mutableStateOf(false) }

    val currentStudent = activeStudents.getOrNull(viewModel.currentIndex)
    val scoreMap = remember(scores) { scores.associate { Pair(it.studentIdPps, it.subject) to it.score } }

    val context = LocalContext.current

    LaunchedEffect(viewModel.selectedSubject, activeStudents) {
        if (currentStudent != null) {
            val scoreVal = scoreMap[Pair(currentStudent.id_pps, viewModel.selectedSubject!!)]
            viewModel.manualScoreText = if (scoreVal != null) {
                if (scoreVal % 1.0 == 0.0) scoreVal.toInt().toString() else scoreVal.toString()
            } else ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Card 1: Subject Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PILIK FAN PELAJARAN / KELAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showSubjectDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("subject_dropdown"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate900)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.selectedSubject?.let { sub ->
                                        val cls = viewModel.selectedClassFilter
                                        if (cls != null) "$sub - Kelas $cls" else "$sub - [SEMUA KELAS]"
                                    } ?: "-- Pilih Fan & Kelas --",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Icon(Icons.Filled.ArrowDropDown, "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            subjectOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt.displayText,
                                            fontWeight = if (opt.isHeader) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (opt.isHeader) 14.sp else 13.sp,
                                            modifier = Modifier.padding(start = if (opt.isHeader) 0.dp else 16.dp),
                                            color = if (opt.isHeader) MaterialTheme.colorScheme.primary else Slate800
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectedSubject = opt.subject
                                        viewModel.selectedClassFilter = opt.classFilter
                                        viewModel.currentIndex = 0
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (activeStudents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Progres Input: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            val filledCount = activeStudents.count {
                                scoreMap[Pair(it.id_pps, viewModel.selectedSubject!!)] != null
                            }
                            val percentage = (filledCount.toFloat() / activeStudents.size.toFloat() * 100).toInt()
                            Text(
                                text = "$percentage% ($filledCount/${activeStudents.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald600
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val filledCount = activeStudents.count {
                            scoreMap[Pair(it.id_pps, viewModel.selectedSubject!!)] != null
                        }
                        LinearProgressIndicator(
                            progress = { filledCount.toFloat() / activeStudents.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Emerald600,
                            trackColor = Slate200
                        )
                    }
                }
            }
        }

        if (viewModel.selectedSubject != null) {
            if (activeStudents.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Siswa yang Sesuai",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Atur tugas kelas atau lengkapi data induk siswa.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else if (currentStudent != null) {
                item {
                    var searchJumpQuery by remember { mutableStateOf("") }
                    var showSearchDropdown by remember { mutableStateOf(false) }
                    var showClassJumpDropdown by remember { mutableStateOf(false) }
                    var showRoomJumpDropdown by remember { mutableStateOf(false) }

                    val uniqueClasses = remember(activeStudents) {
                        activeStudents.map { it.kelas }.distinct().sorted()
                    }
                    val uniqueRooms = remember(activeStudents) {
                        activeStudents.map { it.ruang_imda }.distinct().sorted()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Search Jump Field
                        Box(modifier = Modifier.weight(1.4f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NO",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                
                                BasicTextField(
                                    value = searchJumpQuery,
                                    onValueChange = {
                                        searchJumpQuery = it
                                        showSearchDropdown = it.isNotEmpty()
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF1E293B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchJumpQuery.isEmpty()) {
                                            Text(
                                                text = "Cari Nama / NO / ID / No IMDA...",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Cari",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            val filteredSearchStudents = remember(searchJumpQuery, activeStudents) {
                                if (searchJumpQuery.isBlank()) emptyList()
                                else {
                                    val q = searchJumpQuery.lowercase().trim()
                                    activeStudents.filter {
                                        it.nama.lowercase().contains(q) ||
                                                it.no.lowercase().contains(q) ||
                                                it.id_pps.lowercase().contains(q) ||
                                                it.no_imda.lowercase().contains(q)
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showSearchDropdown && filteredSearchStudents.isNotEmpty(),
                                onDismissRequest = { showSearchDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                filteredSearchStudents.take(8).forEach { std ->
                                    val idx = activeStudents.indexOf(std)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = std.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(text = "NO: ${std.no} | ID: ${std.id_pps} | IMDA: ${std.no_imda} | Kls: ${std.kelas}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            if (idx >= 0) {
                                                viewModel.currentIndex = idx
                                            }
                                            searchJumpQuery = ""
                                            showSearchDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. KELAS Jump Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { showClassJumpDropdown = true }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "KELAS",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "-- Lompat --",
                                        color = Color(0xFF1E293B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showClassJumpDropdown,
                                onDismissRequest = { showClassJumpDropdown = false }
                            ) {
                                uniqueClasses.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text("Kelas $cls", fontSize = 12.sp) },
                                        onClick = {
                                            val targetIdx = activeStudents.indexOfFirst { it.kelas == cls }
                                            if (targetIdx >= 0) {
                                                viewModel.currentIndex = targetIdx
                                            }
                                            showClassJumpDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. RUANG Jump Dropdown
                        Box(modifier = Modifier.weight(1.1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { showRoomJumpDropdown = true }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "RUANG",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "-- Lompat --",
                                        color = Color(0xFF1E293B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showRoomJumpDropdown,
                                onDismissRequest = { showRoomJumpDropdown = false }
                            ) {
                                uniqueRooms.forEach { rm ->
                                    DropdownMenuItem(
                                        text = { Text("Ruang $rm", fontSize = 12.sp) },
                                        onClick = {
                                            val targetIdx = activeStudents.indexOfFirst { it.ruang_imda == rm }
                                            if (targetIdx >= 0) {
                                                viewModel.currentIndex = targetIdx
                                            }
                                            showRoomJumpDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Student card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.testTag("student_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Slate900, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NO: ${currentStudent.no}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = viewModel.selectedSubject ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600,
                                    modifier = Modifier
                                        .background(Emerald600.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentStudent.nama,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Metadata Chips
                                MetadataChip(label = "Kelas", value = currentStudent.kelas, modifier = Modifier.weight(1f))
                                MetadataChip(label = "Abs", value = currentStudent.abs, modifier = Modifier.weight(1f))
                                MetadataChip(label = "Ruang", value = "R.${currentStudent.ruang_imda}", modifier = Modifier.weight(1.2f))
                                MetadataChip(label = "IMDA", value = currentStudent.no_imda, modifier = Modifier.weight(1.2f))
                            }
                        }
                    }
                }

                // Input Controls
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            // Mode Switch
                            TabRow(
                                selectedTabIndex = if (viewModel.inputMode == "manual") 0 else 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate100)
                                    .padding(2.dp),
                                indicator = {},
                                divider = {}
                            ) {
                                Tab(
                                    selected = viewModel.inputMode == "manual",
                                    onClick = { viewModel.inputMode = "manual" },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewModel.inputMode == "manual") Color.White else Color.Transparent)
                                ) {
                                    Text(
                                        text = "✏️ Input Manual",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                        color = if (viewModel.inputMode == "manual") Emerald600 else Color.Gray
                                    )
                                }
                                Tab(
                                    selected = viewModel.inputMode == "calc",
                                    onClick = { viewModel.inputMode = "calc" },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewModel.inputMode == "calc") Color.White else Color.Transparent)
                                ) {
                                    Text(
                                        text = "🧮 Hitung dari Salah",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                        color = if (viewModel.inputMode == "calc") Emerald600 else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (viewModel.inputMode == "manual") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedTextField(
                                        value = viewModel.manualScoreText,
                                        onValueChange = {
                                            if (it.length <= 5) {
                                                viewModel.manualScoreText = it
                                            }
                                        },
                                        placeholder = { Text("0", color = Slate200) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            color = Slate900
                                        ),
                                        modifier = Modifier
                                            .width(180.dp)
                                            .testTag("score_manual_input"),
                                        shape = RoundedCornerShape(16.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Emerald600,
                                            unfocusedBorderColor = Slate200,
                                            focusedContainerColor = Slate50,
                                            unfocusedContainerColor = Slate50
                                        )
                                    )
                                }
                            } else {
                                // Calculator mistakes
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Weights configuration header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Atur Bobot Potongan",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Amber700
                                        )
                                        TextButton(
                                            onClick = {
                                                viewModel.calcWeights = viewModel.calcWeights + 2.0
                                            }
                                        ) {
                                            Text("+ Tambah Tipe", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                                        }
                                    }

                                    // Dynamic Weights Row list
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        viewModel.calcWeights.forEachIndexed { idx, w ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Amber700.copy(alpha = 0.05f)),
                                                border = BorderStroke(1.dp, Amber700.copy(alpha = 0.2f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val typeLetter = ('A' + idx).toString()
                                                    Text("Tipe $typeLetter (-$w) ", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                                                    
                                                    if (viewModel.calcWeights.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                val currentList = viewModel.calcWeights.toMutableList()
                                                                currentList.removeAt(idx)
                                                                viewModel.calcWeights = currentList
                                                                viewModel.resetCalculator()
                                                            },
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Close, "Hapus", tint = Color.Red, modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Divider(color = Slate100)

                                    // Count Inputs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        viewModel.calcWeights.forEachIndexed { idx, w ->
                                            val typeLetter = ('A' + idx).toString()
                                            val count = viewModel.mistakeCounts[idx] ?: 0

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Slate50, RoundedCornerShape(12.dp))
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Salah $typeLetter",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (count > 0) {
                                                                val copy = viewModel.mistakeCounts.toMutableMap()
                                                                copy[idx] = count - 1
                                                                viewModel.mistakeCounts = copy
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Remove, "Minus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }

                                                    Text(
                                                        text = count.toString(),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Slate900,
                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                    )

                                                    IconButton(
                                                        onClick = {
                                                            val copy = viewModel.mistakeCounts.toMutableMap()
                                                            copy[idx] = count + 1
                                                            viewModel.mistakeCounts = copy
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Add, "Add", tint = Emerald600, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Result Row
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Slate900),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "NILAI AKHIR:",
                                                color = Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            val finalScoreVal = viewModel.getCalculatedScore()
                                            Text(
                                                text = if (finalScoreVal % 1.0 == 0.0) finalScoreVal.toInt().toString() else finalScoreVal.toString(),
                                                color = Emerald400,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Core Navigation Buttons (Prev, Save, Next)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.currentIndex > 0) {
                                    viewModel.currentIndex--
                                    viewModel.resetCalculator()
                                }
                            },
                            enabled = viewModel.currentIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("prev_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Slate900,
                                disabledContainerColor = Slate100,
                                disabledContentColor = Color.LightGray
                            ),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Prev")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val scoreToSave = if (viewModel.inputMode == "manual") {
                                    viewModel.manualScoreText.toDoubleOrNull()
                                } else {
                                    viewModel.getCalculatedScore()
                                }

                                if (scoreToSave != null && scoreToSave in 0.0..100.0) {
                                    viewModel.saveScore(
                                        studentIdPps = currentStudent.id_pps,
                                        subject = viewModel.selectedSubject!!,
                                        scoreValue = scoreToSave,
                                        studentName = currentStudent.nama
                                    )

                                    // Auto-advance
                                    if (viewModel.currentIndex < activeStudents.size - 1) {
                                        viewModel.currentIndex++
                                        viewModel.resetCalculator()
                                    } else {
                                        Toast.makeText(context, "Nilai Siswa Terakhir Disimpan!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Format Nilai Harus Antara 0 - 100", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                                .testTag("save_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text("SIMPAN", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }

                        Button(
                            onClick = {
                                if (viewModel.currentIndex < activeStudents.size - 1) {
                                    viewModel.currentIndex++
                                    viewModel.resetCalculator()
                                }
                            },
                            enabled = viewModel.currentIndex < activeStudents.size - 1,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("next_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Slate900,
                                disabledContainerColor = Slate100,
                                disabledContentColor = Color.LightGray
                            ),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text("Lanjut", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.KeyboardArrowDown, "Next")
                        }
                    }
                }

                // Context list inside column (collapsible view of current range)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NAVIGASI SISWA LAIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Siswa ${viewModel.currentIndex + 1} dari ${activeStudents.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Show quick context of 4 neighboring students
                            val contextRange = remember(viewModel.currentIndex, activeStudents) {
                                val min = maxOf(0, viewModel.currentIndex - 2)
                                val max = minOf(activeStudents.size - 1, viewModel.currentIndex + 2)
                                min..max
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                contextRange.forEach { idx ->
                                    val std = activeStudents[idx]
                                    val isCurrent = idx == viewModel.currentIndex
                                    val scVal = scoreMap[Pair(std.id_pps, viewModel.selectedSubject!!)]

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) Emerald600.copy(alpha = 0.1f) else Color.Transparent)
                                            .clickable {
                                                viewModel.currentIndex = idx
                                                viewModel.resetCalculator()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrent) Emerald600 else Slate100),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = std.no,
                                                    fontSize = 10.sp,
                                                    color = if (isCurrent) Color.White else Slate900,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = std.nama,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) Emerald700 else Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 200.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (scVal != null) Emerald600.copy(alpha = 0.15f) else Color.Transparent,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = scVal?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "-",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (scVal != null) Emerald700 else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.School, "Madrasah", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Silakan Pilih Fan Pelajaran Terlebih Dahulu",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label.uppercase(), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Slate900, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekAlpaTab(
    viewModel: AppViewModel,
    availableSubjects: List<String>,
    students: List<Student>,
    scores: List<StudentScore>
) {
    var showSubDropdown by remember { mutableStateOf(false) }

    val scoreMap = remember(scores) { scores.associate { Pair(it.studentIdPps, it.subject) to it.score } }

    val filteredAlpaStudents = remember(students, viewModel.alpaSubject, viewModel.alpaClassFilter, viewModel.alpaRoomFilter, viewModel.alpaSearchQuery, scoreMap) {
        if (viewModel.alpaSubject == null) emptyList()
        else {
            students.filter { s ->
                val hasNoScore = scoreMap[Pair(s.id_pps, viewModel.alpaSubject!!)] == null
                val classMatches = viewModel.alpaClassFilter == null || s.kelas == viewModel.alpaClassFilter
                val roomMatches = viewModel.alpaRoomFilter == null || s.ruang_imda == viewModel.alpaRoomFilter
                val queryMatches = viewModel.alpaSearchQuery.isBlank() || s.nama.contains(viewModel.alpaSearchQuery, ignoreCase = true) || s.no == viewModel.alpaSearchQuery

                hasNoScore && classMatches && roomMatches && queryMatches
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PENYISIRAN SISWA ALPA (BELUM UJIAN)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Daftar siswa yang belum memiliki nilai ujian pada fan pelajaran pilihan Anda.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject Selector Button
                    Box {
                        OutlinedButton(
                            onClick = { showSubDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.alpaSubject ?: "-- Pilih Pelajaran --",
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Icon(Icons.Filled.ArrowDropDown, "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showSubDropdown,
                            onDismissRequest = { showSubDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text("📚 $sub", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.alpaSubject = sub
                                        showSubDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Simple class text field filter
                        OutlinedTextField(
                            value = viewModel.alpaClassFilter ?: "",
                            onValueChange = { viewModel.alpaClassFilter = if (it.isBlank()) null else it },
                            label = { Text("Filter Kelas", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.alpaRoomFilter ?: "",
                            onValueChange = { viewModel.alpaRoomFilter = if (it.isBlank()) null else it },
                            label = { Text("Filter Ruang", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.alpaSearchQuery,
                        onValueChange = { viewModel.alpaSearchQuery = it },
                        label = { Text("Cari Nama atau Nomor", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Filled.Search, "Search") }
                    )
                }
            }
        }

        if (viewModel.alpaSubject == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pilih Pelajaran untuk menyisir data.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            if (filteredAlpaStudents.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Emerald600.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CheckCircle, "Clean", tint = Emerald600, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Alhamdulillah Bersih!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald700
                            )
                            Text(
                                text = "Semua siswa pada kategori ini telah mengikuti ujian.",
                                fontSize = 11.sp,
                                color = Slate800,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "SISWA BELUM UJIAN (${filteredAlpaStudents.size} ANAK)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(filteredAlpaStudents) { student ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.nama,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kls: ${student.kelas} • Abs: ${student.abs} • R: ${student.ruang_imda} • IMDA: ${student.no_imda}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    // Jump to Input Tab
                                    viewModel.selectedSubject = viewModel.alpaSubject
                                    val idx = students.indexOfFirst { it.id_pps == student.id_pps }
                                    if (idx != -1) {
                                        viewModel.currentIndex = idx
                                        viewModel.currentTab = "input"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("✏️ Input", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KomplainTab(
    viewModel: AppViewModel,
    availableSubjects: List<String>,
    students: List<Student>,
    scores: List<StudentScore>
) {
    var showCompSubDropdown by remember { mutableStateOf(false) }
    val scoreMap = remember(scores) { scores.associate { Pair(it.studentIdPps, it.subject) to it.score } }

    val filteredKomplainStudents = remember(students, viewModel.komplainSubject, viewModel.komplainClassFilter, viewModel.komplainSearchQuery, scoreMap) {
        if (viewModel.komplainSubject == null) emptyList()
        else {
            students.filter { s ->
                val hasScore = scoreMap[Pair(s.id_pps, viewModel.komplainSubject!!)] != null
                val classMatches = viewModel.komplainClassFilter == null || s.kelas == viewModel.komplainClassFilter
                val queryMatches = viewModel.komplainSearchQuery.isBlank() || s.nama.contains(viewModel.komplainSearchQuery, ignoreCase = true) || s.no == viewModel.komplainSearchQuery

                hasScore && classMatches && queryMatches
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PUSAT REVISI & KOMPLAIN NILAI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Cari cepat murid yang mengajukan keberatan/komplain, lalu revisi atau update nilainya langsung.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showCompSubDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.komplainSubject ?: "-- Pilih Pelajaran --",
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Icon(Icons.Filled.ArrowDropDown, "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showCompSubDropdown,
                            onDismissRequest = { showCompSubDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text("📚 $sub", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.komplainSubject = sub
                                        showCompSubDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.komplainClassFilter ?: "",
                            onValueChange = { viewModel.komplainClassFilter = if (it.isBlank()) null else it },
                            label = { Text("Filter Kelas", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.komplainSearchQuery,
                            onValueChange = { viewModel.komplainSearchQuery = it },
                            label = { Text("Nama / No Urut", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        if (viewModel.komplainSubject == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pilih Pelajaran untuk memulai peninjauan.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            if (filteredKomplainStudents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Siswa tidak ditemukan.", color = Color.Gray)
                    }
                }
            } else {
                items(filteredKomplainStudents) { student ->
                    val scVal = scoreMap[Pair(student.id_pps, viewModel.komplainSubject!!)]
                    var currentScoreText by remember(scVal) {
                        mutableStateOf(scVal?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = student.nama,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Kelas: ${student.kelas} • Abs: ${student.abs} • R: ${student.ruang_imda}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentScoreText,
                                    onValueChange = { currentScoreText = it },
                                    placeholder = { Text("-") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(64.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Black
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        val parseD = currentScoreText.toDoubleOrNull()
                                        if (parseD != null && parseD in 0.0..100.0) {
                                            viewModel.saveScore(
                                                studentIdPps = student.id_pps,
                                                subject = viewModel.komplainSubject!!,
                                                scoreValue = parseD,
                                                studentName = student.nama
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Revisi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisaTab(
    viewModel: AppViewModel,
    availableSubjects: List<String>,
    students: List<Student>,
    scores: List<StudentScore>,
    duties: List<TeacherDuty>
) {
    var showAnalSubDropdown by remember { mutableStateOf(false) }

    val scoreMap = remember(scores) { scores.associate { Pair(it.studentIdPps, it.subject) to it.score } }

    val validScores = remember(students, viewModel.analisaSubject, scoreMap) {
        if (viewModel.analisaSubject == null) emptyList()
        else {
            students.mapNotNull { s ->
                val sc = scoreMap[Pair(s.id_pps, viewModel.analisaSubject!!)]
                if (sc != null) {
                    Triple(s.nama, s.kelas, sc)
                } else null
            }
        }
    }

    data class ClassMetrics(val count: Int, val average: Double, val highest: Double, val criticalCount: Int)

    val classStatsList = remember(validScores) {
        val grouped = validScores.groupBy { it.second }
        grouped.map { (className, data) ->
            val avg = data.map { it.third }.average()
            val high = data.map { it.third }.maxOrNull() ?: 0.0
            val crit = data.count { it.third < 30.0 }
            Pair(className, ClassMetrics(data.size, avg, high, crit))
        }.sortedByDescending { it.second.average }
    }

    val stats = remember(validScores) {
        if (validScores.isEmpty()) null
        else {
            val average = validScores.map { it.third }.average()
            val highest = validScores.map { it.third }.maxOrNull() ?: 0.0
            val lowest = validScores.map { it.third }.minOrNull() ?: 0.0
            val criticalCount = validScores.count { it.third < 30.0 }

            val distA = validScores.count { it.third >= 90.0 }
            val distB = validScores.count { it.third in 76.0..89.9 }
            val distC = validScores.count { it.third in 60.0..75.9 }
            val distD = validScores.count { it.third in 40.0..59.9 }
            val distE = validScores.count { it.third < 40.0 }

            object {
                val avg = average
                val high = highest
                val low = lowest
                val crit = criticalCount
                val dA = distA
                val dB = distB
                val dC = distC
                val dD = distD
                val dE = distE
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ANALISA PENCAPAIAN AKADEMIK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Statistik laporan global pencapaian dan perbandingan peringkat kelas.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showAnalSubDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.analisaSubject ?: "-- Pilih Pelajaran --",
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Icon(Icons.Filled.ArrowDropDown, "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showAnalSubDropdown,
                            onDismissRequest = { showAnalSubDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text("📚 $sub", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.analisaSubject = sub
                                        showAnalSubDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (viewModel.analisaSubject == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pilih Pelajaran untuk menganalisis.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            if (validScores.isEmpty() || stats == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada data nilai pada pelajaran ini.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Statistics Dashboard Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. STATISTIK GLOBAL (UMUM)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Average card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("RATA-RATA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(
                                        text = String.format("%.1f", stats.avg),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Emerald600,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text("Dari ${validScores.size} Murid", fontSize = 8.sp, color = Color.Gray)
                                }
                            }

                            // Highest score card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Emerald600.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("TERTINGGI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(
                                        text = if (stats.high % 1.0 == 0.0) stats.high.toInt().toString() else stats.high.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Emerald700,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text("Skor Tertinggi", fontSize = 8.sp, color = Color.Gray)
                                }
                            }

                            // Critical Score card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("KRITIS (<30)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(
                                        text = stats.crit.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Red,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text("Skor Terendah: ${stats.low}", fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Distributions list
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DISTRIBUSI NILAI AKADEMIK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            Spacer(modifier = Modifier.height(12.dp))

                            DistRow(grade = "A (90-100)", count = stats.dA, color = Emerald600, total = validScores.size)
                            DistRow(grade = "B (76-89)", count = stats.dB, color = Teal700, total = validScores.size)
                            DistRow(grade = "C (60-75)", count = stats.dC, color = Amber700, total = validScores.size)
                            DistRow(grade = "D (40-59)", count = stats.dD, color = Color(0xFFF97316), total = validScores.size)
                            DistRow(grade = "E (< 40)", count = stats.dE, color = Color.Red, total = validScores.size)
                        }
                    }
                }

                // Rankings per Class
                item {
                    Text("2. PERINGKAT & ANALISA KELAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }

                itemsIndexed(classStatsList) { index, stat ->
                    val className = stat.first
                    val classMetrics = stat.second

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank number
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (index) {
                                            0 -> Amber700.copy(alpha = 0.15f)
                                            1 -> Slate200
                                            else -> Slate50
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (index == 0) Amber700 else Slate900
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "KELAS: $className",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${classMetrics.count} Murid • Tertinggi: ${if (classMetrics.highest % 1.0 == 0.0) classMetrics.highest.toInt().toString() else classMetrics.highest.toString()} • Kritis: ${classMetrics.criticalCount}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("RATA-RATA", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format("%.1f", classMetrics.average),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald600
                                )
                            }
                        }
                    }
                }

                // CSV Export button
                item {
                    val contextLocal = LocalContext.current
                    Button(
                        onClick = {
                            exportCsvReport(contextLocal, viewModel.analisaSubject!!, validScores)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        Icon(Icons.Filled.Share, "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BAGIKAN LAPORAN KELAS (.CSV)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun DistRow(grade: String, count: Int, color: Color, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total.toFloat() else 0f

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(grade, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text("$count Murid (${(fraction * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Slate100
        )
    }
}

fun exportCsvReport(context: Context, subjectName: String, scores: List<Triple<String, String, Double>>) {
    try {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "Laporan_Nilai_${subjectName.replace(" ", "_")}.csv")
        val writer = FileWriter(file)
        
        writer.append("Nama Murid,Kelas,Nilai\n")
        scores.forEach { s ->
            writer.append("${s.first},${s.second},${s.third}\n")
        }
        writer.flush()
        writer.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Nilai $subjectName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Nilai"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiswaTab(viewModel: AppViewModel, students: List<Student>) {
    var showFormSiswaDialog by remember { mutableStateOf(false) }
    var selectedStudentToEdit by remember { mutableStateOf<Student?>(null) }

    val filteredList = remember(students, viewModel.siswaClassFilter, viewModel.siswaRoomFilter, viewModel.siswaSearchQuery) {
        students.filter { s ->
            val classMatches = viewModel.siswaClassFilter == null || s.kelas == viewModel.siswaClassFilter
            val roomMatches = viewModel.siswaRoomFilter == null || s.ruang_imda == viewModel.siswaRoomFilter
            val queryMatches = viewModel.siswaSearchQuery.isBlank() || s.nama.contains(viewModel.siswaSearchQuery, ignoreCase = true) || s.no == viewModel.siswaSearchQuery

            classMatches && roomMatches && queryMatches
        }
    }

    val totalPages = remember(filteredList) {
        kotlin.math.max(1, kotlin.math.ceil(filteredList.size.toFloat() / viewModel.itemsPerPageSiswa.toFloat()).toInt())
    }

    val pageData = remember(filteredList, viewModel.siswaCurrentPage) {
        val start = (viewModel.siswaCurrentPage - 1) * viewModel.itemsPerPageSiswa
        val end = minOf(start + viewModel.itemsPerPageSiswa, filteredList.size)
        if (start < filteredList.size) filteredList.subList(start, end) else emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DATA INDUK SISWA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Button(
                            onClick = {
                                selectedStudentToEdit = null
                                showFormSiswaDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Tambah", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Siswa Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.siswaClassFilter ?: "",
                            onValueChange = {
                                viewModel.siswaClassFilter = if (it.isBlank()) null else it
                                viewModel.siswaCurrentPage = 1
                            },
                            label = { Text("Filter Kelas", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.siswaRoomFilter ?: "",
                            onValueChange = {
                                viewModel.siswaRoomFilter = if (it.isBlank()) null else it
                                viewModel.siswaCurrentPage = 1
                            },
                            label = { Text("Filter Ruang", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.siswaSearchQuery,
                        onValueChange = {
                            viewModel.siswaSearchQuery = it
                            viewModel.siswaCurrentPage = 1
                        },
                        label = { Text("Nama / No Urut / PPS ID", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Filled.Search, "Cari") }
                    )
                }
            }
        }

        if (pageData.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Siswa tidak ditemukan.", color = Color.Gray)
                }
            }
        } else {
            items(pageData) { student ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = student.nama,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kls: ${student.kelas} • Abs: ${student.abs} • R: ${student.ruang_imda} • IMDA: ${student.no_imda}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    selectedStudentToEdit = student
                                    showFormSiswaDialog = true
                                }
                            ) {
                                Icon(Icons.Filled.Edit, "Edit", tint = Amber700)
                            }

                            IconButton(
                                onClick = { viewModel.deleteStudent(student) }
                            ) {
                                Icon(Icons.Filled.Delete, "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // Pagination Controls
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (viewModel.siswaCurrentPage > 1) viewModel.siswaCurrentPage-- },
                        enabled = viewModel.siswaCurrentPage > 1,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back", fontSize = 12.sp)
                    }

                    Text(
                        text = "Halaman ${viewModel.siswaCurrentPage} dari $totalPages",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Button(
                        onClick = { if (viewModel.siswaCurrentPage < totalPages) viewModel.siswaCurrentPage++ },
                        enabled = viewModel.siswaCurrentPage < totalPages,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900)
                    ) {
                        Text("Next", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showFormSiswaDialog) {
        FormSiswaDialog(
            student = selectedStudentToEdit,
            onDismiss = { showFormSiswaDialog = false },
            onSave = { updatedStudent ->
                viewModel.addOrUpdateStudent(updatedStudent, isEdit = (selectedStudentToEdit != null))
                showFormSiswaDialog = false
            },
            nextNo = if (selectedStudentToEdit == null) {
                (students.maxOfOrNull { it.no.toIntOrNull() ?: 0 } ?: 0) + 1
            } else 0
        )
    }
}

@Composable
fun FormSiswaDialog(
    student: Student?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit,
    nextNo: Int
) {
    var no by remember { mutableStateOf(student?.no ?: nextNo.toString()) }
    var abs by remember { mutableStateOf(student?.abs ?: "") }
    var nama by remember { mutableStateOf(student?.nama ?: "") }
    var id_pps by remember { mutableStateOf(student?.id_pps ?: "") }
    var kelas by remember { mutableStateOf(student?.kelas ?: "") }
    var no_imda by remember { mutableStateOf(student?.no_imda ?: "") }
    var ruang_imda by remember { mutableStateOf(student?.ruang_imda ?: "") }
    var domisili by remember { mutableStateOf(student?.domisili ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (student == null) "TAMBAH SISWA BARU" else "EDIT DATA SISWA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = no,
                        onValueChange = { no = it },
                        label = { Text("No Urut", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = abs,
                        onValueChange = { abs = it },
                        label = { Text("Absensi", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = id_pps,
                    onValueChange = { id_pps = it },
                    label = { Text("ID PPS (Kode Unik)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kelas,
                        onValueChange = { kelas = it },
                        label = { Text("Kelas", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = no_imda,
                        onValueChange = { no_imda = it },
                        label = { Text("No IMDA", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ruang_imda,
                        onValueChange = { ruang_imda = it },
                        label = { Text("Ruang IMDA", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = domisili,
                        onValueChange = { domisili = it },
                        label = { Text("Domisili (Alamat)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BATAL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (no.isNotBlank() && nama.isNotBlank() && id_pps.isNotBlank()) {
                                onSave(
                                    Student(
                                        rowNum = student?.rowNum ?: 0,
                                        no = no,
                                        abs = abs,
                                        id_pps = id_pps,
                                        nama = nama,
                                        domisili = domisili,
                                        kelas = kelas,
                                        no_imda = no_imda,
                                        ruang_imda = ruang_imda
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SIMPAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManajemenTab(
    viewModel: AppViewModel,
    teachers: List<Teacher>,
    duties: List<TeacherDuty>,
    students: List<Student>,
    scores: List<StudentScore>,
    availableSubjects: List<String>
) {
    var showAddDutyDialog by remember { mutableStateOf(false) }
    var selectedTeacherForDuty by remember { mutableStateOf<Teacher?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANAJEMEN AKUN & TUGAS GURU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Pantau progres kerja korektor nilai dan kelola hak akses atau distribusi beban fan pelajaran.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        items(teachers) { teacher ->
            val teacherDuties = duties.filter { it.teacherRowNum == teacher.rowNum }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = teacher.nama,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                            Text(
                                text = "Username: ${teacher.username} • Role: ${teacher.role}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                selectedTeacherForDuty = teacher
                                showAddDutyDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+ Tugas", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DAFTAR BEBAN TUGAS:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (teacherDuties.isEmpty()) {
                        Text(
                            text = "Belum memiliki tugas mengajar.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            teacherDuties.forEachIndexed { idx, duty ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate50, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.MenuBook, "Book", tint = Emerald600, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${duty.subject} (Kls ${duty.className})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteTeacherDuty(teacher.rowNum, duty, idx)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, "Close", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDutyDialog && selectedTeacherForDuty != null) {
        AddDutyDialog(
            teacher = selectedTeacherForDuty!!,
            availableSubjects = availableSubjects,
            onDismiss = { showAddDutyDialog = false },
            onSave = { subject, className ->
                viewModel.addTeacherDuty(selectedTeacherForDuty!!.rowNum, subject, className)
                showAddDutyDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDutyDialog(
    teacher: Teacher,
    availableSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "") }
    var className by remember { mutableStateOf("") }
    var showSubjectMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TAMBAH TUGAS GURU",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )

                Text(
                    text = "Guru Target: ${teacher.nama}",
                    fontSize = 12.sp,
                    color = Emerald600,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box {
                    OutlinedButton(
                        onClick = { showSubjectMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedSubject, fontWeight = FontWeight.Bold, color = Slate900)
                            Icon(Icons.Filled.ArrowDropDown, "Dropdown")
                        }
                    }

                    DropdownMenu(
                        expanded = showSubjectMenu,
                        onDismissRequest = { showSubjectMenu = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        availableSubjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubject = sub
                                    showSubjectMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Masukkan Kelas Tugas (Contoh: 1A)", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BATAL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedSubject.isNotBlank() && className.isNotBlank()) {
                                onSave(selectedSubject, className)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SIMPAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: AppViewModel, currentUser: Teacher) {
    var inputUrl by remember { mutableStateOf(viewModel.webAppUrl.value) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INTEGRASI GOOGLE SPREADSHEET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "Hubungkan database lokal Android Anda langsung ke Google Sheet secara dua arah.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Google Apps Script Web App URL", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webapp_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald600,
                            focusedLabelColor = Emerald600
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateWebAppUrl(inputUrl)
                            viewModel.triggerSync()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Sync, "Sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN & SINKRONISASI SEKARANG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Instructional Setup Guide Card with copy-pasteable script router!
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PETUNJUK DEPLOYMENT APPS SCRIPT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Emerald400
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Untuk menghubungkan aplikasi ini dengan Google Sheet Anda, silakan tambahkan router doGet & doPost berikut di bagian paling bawah Code.gs Anda, lalu Deploy sebagai Web App:\n\n" +
                                "```javascript\n" +
                                "function doPost(e) {\n" +
                                "  var action = e.parameter.action;\n" +
                                "  if (action === 'updateStudentScore') {\n" +
                                "    var res = updateStudentScore(e.parameter.studentIdPps, e.parameter.subjectName, e.parameter.scoreValue);\n" +
                                "    return ContentService.createTextOutput(JSON.stringify(res)).setMimeType(ContentService.MimeType.JSON);\n" +
                                "  }\n" +
                                "  // ... tambahkan router untuk addStudentData dsb.\n" +
                                "}\n" +
                                "```",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
