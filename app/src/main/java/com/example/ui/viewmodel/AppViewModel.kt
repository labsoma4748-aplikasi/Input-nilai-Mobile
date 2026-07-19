package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application, val repository: AppRepository) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Theme Mode
    var isDarkMode by mutableStateOf(sharedPrefs.getBoolean("is_dark_mode", false))
        private set

    fun toggleTheme() {
        isDarkMode = !isDarkMode
        sharedPrefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
    }

    // Sync Web App URL
    private val defaultUrl = "https://script.google.com/macros/s/AKfycbzqWnlODDMc6V6mQNlpq6vHXYUZLWzm7A7iKgx0n9cfCvT_JTFipxD8UAY64zIstjg1/exec"
    private val _webAppUrl = MutableStateFlow(
        sharedPrefs.getString("web_app_url", defaultUrl)?.let { url ->
            if (url.isEmpty() || url.contains("AKfycbxbzRR4wFMsXBFf3zc_mhFp18b1ona99q6Zjp17uBj-1WmRso6wHQpNQ_zwRW1Xynbp")) {
                sharedPrefs.edit().putString("web_app_url", defaultUrl).apply()
                defaultUrl
            } else {
                url
            }
        } ?: defaultUrl
    )
    val webAppUrl: StateFlow<String> = _webAppUrl.asStateFlow()

    fun updateWebAppUrl(url: String) {
        _webAppUrl.value = url
        sharedPrefs.edit().putString("web_app_url", url).apply()
    }

    // Auth State
    var currentUser by mutableStateOf<Teacher?>(null)
        private set
    var loginError by mutableStateOf<String?>(null)

    // Navigation Tab
    var currentTab by mutableStateOf("input")

    // Sync State
    var isSyncing by mutableStateOf(false)
        private set
    var syncMessage by mutableStateOf<String?>(null)

    // Observables from Room
    val students: StateFlow<List<Student>> = repository.allStudents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scores: StateFlow<List<StudentScore>> = repository.allScores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val teachers: StateFlow<List<Teacher>> = repository.allTeachers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val duties: StateFlow<List<TeacherDuty>> = repository.allDuties.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Score Input State
    var selectedSubject by mutableStateOf<String?>(null)
    var selectedClassFilter by mutableStateOf<String?>(null) // Format: "ClassA" or null
    var currentIndex by mutableStateOf(0)
    var inputMode by mutableStateOf("manual") // "manual" or "calc"
    var calcWeights by mutableStateOf(listOf(2.0)) // Deduction weights
    var mistakeCounts by mutableStateOf(mapOf<Int, Int>()) // Index to Count of mistakes
    var manualScoreText by mutableStateOf("")

    // Alpa State
    var alpaSubject by mutableStateOf<String?>(null)
    var alpaClassFilter by mutableStateOf<String?>(null)
    var alpaRoomFilter by mutableStateOf<String?>(null)
    var alpaSearchQuery by mutableStateOf("")

    // Komplain State
    var komplainSubject by mutableStateOf<String?>(null)
    var komplainClassFilter by mutableStateOf<String?>(null)
    var komplainSearchQuery by mutableStateOf("")
    var recentlyEditedIds by mutableStateOf(listOf<String>())

    // Analisa State
    var analisaSubject by mutableStateOf<String?>(null)

    // Siswa Management State
    var siswaClassFilter by mutableStateOf<String?>(null)
    var siswaRoomFilter by mutableStateOf<String?>(null)
    var siswaSearchQuery by mutableStateOf("")
    var siswaCurrentPage by mutableStateOf(1)
    val itemsPerPageSiswa = 10

    // Emergency Error Dialog State (Network Connection issue)
    var showEmergencyErrorDialog by mutableStateOf(false)
    var emergencyErrorStudentName by mutableStateOf("")

    init {
        viewModelScope.launch {
            repository.checkAndPrePopulate()
            // Try to auto-restore session if desired, but default is to require login
            val savedUser = sharedPrefs.getString("logged_in_user", null)
            if (savedUser != null) {
                // Auto login for better UX
                val tList = repository.allTeachers.stateIn(viewModelScope).value
                val matched = tList.find { it.username == savedUser }
                if (matched != null) {
                    currentUser = matched
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val teacherList = repository.allTeachers.stateIn(this).value
            val matched = teacherList.find {
                it.username.trim().equals(username.trim(), ignoreCase = true) &&
                        it.password.trim() == password.trim()
            }

            if (matched != null) {
                currentUser = matched
                loginError = null
                sharedPrefs.edit().putString("logged_in_user", matched.username).apply()
            } else {
                loginError = "Username atau Password salah!"
            }
        }
    }

    fun logout() {
        currentUser = null
        sharedPrefs.edit().remove("logged_in_user").apply()
        currentTab = "input"
    }

    fun triggerSync() {
        if (webAppUrl.value.isBlank()) {
            syncMessage = "Harap atur Web App URL Google Sheet di tab Setelan!"
            return
        }

        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Menghubungkan ke Google Sheet..."
            val result = repository.syncWithRemote(webAppUrl.value)
            isSyncing = false
            if (result.isSuccess) {
                syncMessage = "Sinkronisasi Berhasil! Database telah diperbarui."
            } else {
                syncMessage = "Sinkronisasi Gagal: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearSyncMessage() {
        syncMessage = null
    }

    // Save Score locally & remotely
    fun saveScore(studentIdPps: String, subject: String, scoreValue: Double, studentName: String) {
        viewModelScope.launch {
            // Save locally first
            repository.saveScoreLocally(studentIdPps, subject, scoreValue)

            // Upload to Google Sheet if Web App URL is present
            if (webAppUrl.value.isNotBlank()) {
                val result = repository.uploadScoreRemote(
                    webAppUrl = webAppUrl.value,
                    studentIdPps = studentIdPps,
                    subject = subject,
                    score = scoreValue.toString()
                )
                if (result.isFailure) {
                    // Show emergency error if network failed
                    emergencyErrorStudentName = studentName
                    showEmergencyErrorDialog = true
                }
            }
        }
    }

    // Admin Student CRUD
    fun addOrUpdateStudent(student: Student, isEdit: Boolean) {
        viewModelScope.launch {
            repository.insertStudentLocally(student)
            if (webAppUrl.value.isNotBlank()) {
                val action = if (isEdit) "edit" else "add"
                val result = repository.uploadStudentDataRemote(webAppUrl.value, student, action)
                if (result.isFailure) {
                    Log.e("AppViewModel", "Failed to upload student remote")
                }
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudentLocally(student.rowNum)
            if (webAppUrl.value.isNotBlank()) {
                val result = repository.deleteStudentRemote(webAppUrl.value, student.rowNum)
                if (result.isFailure) {
                    Log.e("AppViewModel", "Failed to delete student remote")
                }
            }
        }
    }

    // Admin Teacher Duties CRUD
    fun addTeacherDuty(rowNum: Int, subject: String, className: String) {
        viewModelScope.launch {
            val duty = TeacherDuty(rowNum, subject, className)
            repository.insertDutyLocally(duty)
            if (webAppUrl.value.isNotBlank()) {
                val result = repository.uploadTeacherDutyRemote(webAppUrl.value, rowNum, subject, className)
                if (result.isFailure) {
                    Log.e("AppViewModel", "Failed to add teacher duty remote")
                }
            }
        }
    }

    fun deleteTeacherDuty(rowNum: Int, duty: TeacherDuty, indexToDelete: Int) {
        viewModelScope.launch {
            repository.deleteDutyLocally(rowNum, duty.subject, duty.className)
            if (webAppUrl.value.isNotBlank()) {
                val result = repository.deleteTeacherDutyRemote(webAppUrl.value, rowNum, indexToDelete)
                if (result.isFailure) {
                    Log.e("AppViewModel", "Failed to delete teacher duty remote")
                }
            }
        }
    }

    // Helper to calculate score in calc mode
    fun getCalculatedScore(): Double {
        var pen = 0.0
        calcWeights.forEachIndexed { i, weight ->
            val count = mistakeCounts[i] ?: 0
            pen += count * weight
        }
        return kotlin.math.max(0.0, 100.0 - pen)
    }

    fun resetCalculator() {
        mistakeCounts = emptyMap()
    }
}
