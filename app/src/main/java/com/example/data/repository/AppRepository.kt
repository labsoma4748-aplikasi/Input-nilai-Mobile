package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDao
import com.example.data.mock.MockData
import com.example.data.model.*
import com.example.data.remote.SyncApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppRepository(private val appDao: AppDao) {

    val allStudents: Flow<List<Student>> = appDao.getAllStudentsFlow()
    val allScores: Flow<List<StudentScore>> = appDao.getAllScoresFlow()
    val allTeachers: Flow<List<Teacher>> = appDao.getAllTeachersFlow()
    val allDuties: Flow<List<TeacherDuty>> = appDao.getAllDutiesFlow()

    private val apiService: SyncApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://script.google.com/") // Placeholder, we use @Url dynamic endpoints
            .client(client)
            .build()

        apiService = retrofit.create(SyncApiService::class.java)
    }

    suspend fun checkAndPrePopulate() = withContext(Dispatchers.IO) {
        val students = appDao.getAllStudents()
        if (students.isEmpty()) {
            Log.d("AppRepository", "Database empty. Pre-populating students and scores...")
            appDao.insertStudents(MockData.defaultStudents)
            appDao.insertScores(MockData.defaultScores)
        }
        
        val teachers = appDao.getAllTeachers()
        if (teachers.size < MockData.defaultTeachers.size) {
            Log.d("AppRepository", "Teachers list empty or outdated. Populating default teachers...")
            appDao.clearAllTeachers()
            appDao.insertTeachers(MockData.defaultTeachers)
        }

        val duties = appDao.getAllDutiesFlow() // or check with flow first or just insert if empty
        // Since we don't have a direct suspend fun for get all duties, we can check size of teachers
        // and if teachers was updated/empty, we can pre-populate duties too or always ensure default duties
        // are there. Actually, let's also ensure duties are populated. Let's see if we have a suspend query for duties.
        // Wait, appDao.getAllDutiesFlow() returns Flow. Let's look at AppDao to see if we have a suspend list query.
        // There isn't, but we can check if teachers size was smaller, or we can just always insert duties on conflict replace.
        appDao.clearAllDuties()
        appDao.insertDuties(MockData.defaultDuties)
    }

    suspend fun saveScoreLocally(studentIdPps: String, subject: String, score: Double?) = withContext(Dispatchers.IO) {
        appDao.insertScore(StudentScore(studentIdPps, subject, score))
    }

    suspend fun deleteStudentLocally(rowNum: Int) = withContext(Dispatchers.IO) {
        appDao.deleteStudentByRowNum(rowNum)
    }

    suspend fun insertStudentLocally(student: Student) = withContext(Dispatchers.IO) {
        appDao.insertStudent(student)
    }

    suspend fun insertTeacherLocally(teacher: Teacher) = withContext(Dispatchers.IO) {
        appDao.insertTeacher(teacher)
    }

    suspend fun insertDutyLocally(duty: TeacherDuty) = withContext(Dispatchers.IO) {
        appDao.insertDuty(duty)
    }

    suspend fun deleteDutyLocally(rowNum: Int, subject: String, className: String) = withContext(Dispatchers.IO) {
        appDao.deleteDuty(rowNum, subject, className)
    }

    // Dynamic 2D Spreadsheet Parser
    suspend fun syncWithRemote(webAppUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSheetData(webAppUrl)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error: ${response.code()}"))
            }

            val bodyString = response.body()?.string() ?: return@withContext Result.failure(Exception("Empty body response"))
            val jsonArray = JSONArray(bodyString)

            if (jsonArray.length() < 1) {
                return@withContext Result.failure(Exception("Invalid data structure in Sheet"))
            }

            val headersArray = jsonArray.getJSONArray(0)
            val headers = mutableListOf<String>()
            for (i in 0 until headersArray.length()) {
                headers.add(headersArray.getString(i))
            }

            // At least 8 columns required for student metadata
            if (headers.size < 8) {
                return@withContext Result.failure(Exception("Spreadsheet does not match required columns format"))
            }

            val parsedStudents = mutableListOf<Student>()
            val parsedScores = mutableListOf<StudentScore>()

            for (i in 1 until jsonArray.length()) {
                val row = jsonArray.getJSONArray(i)
                if (row.length() == 0 || row.getString(0).trim().isEmpty()) continue

                val rowNum = i + 1
                val no = row.optString(0, "")
                val abs = row.optString(1, "")
                val id_pps = row.optString(2, "")
                val nama = row.optString(3, "")
                val domisili = row.optString(4, "")
                val kelas = row.optString(5, "")
                val no_imda = row.optString(6, "")
                val ruang_imda = row.optString(7, "")

                if (id_pps.isBlank()) continue

                parsedStudents.add(
                    Student(
                        rowNum = rowNum,
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

                // Subjects start at column index 8
                for (j in 8 until row.length()) {
                    if (j < headers.size) {
                        val subjectName = headers[j]
                        val scoreStr = row.optString(j, "").trim()
                        if (scoreStr.isNotEmpty()) {
                            val scoreValue = scoreStr.toDoubleOrNull()
                            parsedScores.add(StudentScore(id_pps, subjectName, scoreValue))
                        }
                    }
                }
            }

            if (parsedStudents.isNotEmpty()) {
                // Keep local changes but sync new sheet data
                appDao.clearAllStudents()
                appDao.insertStudents(parsedStudents)

                appDao.clearAllScores()
                appDao.insertScores(parsedScores)
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("No valid student rows found in sheet"))
            }

        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing with remote spreadsheet", e)
            return@withContext Result.failure(e)
        }
    }

    // Remote scoring upload
    suspend fun uploadScoreRemote(webAppUrl: String, studentIdPps: String, subject: String, score: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateStudentScore(webAppUrl, studentIdPps = studentIdPps, subjectName = subject, scoreValue = score)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Remote update failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadStudentDataRemote(webAppUrl: String, student: Student, action: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = if (action == "add") {
                apiService.addStudentData(
                    url = webAppUrl,
                    no = student.no,
                    abs = student.abs,
                    id_pps = student.id_pps,
                    nama = student.nama,
                    domisili = student.domisili,
                    kelas = student.kelas,
                    no_imda = student.no_imda,
                    ruang_imda = student.ruang_imda
                )
            } else {
                apiService.updateStudentData(
                    url = webAppUrl,
                    rowNum = student.rowNum,
                    no = student.no,
                    abs = student.abs,
                    id_pps = student.id_pps,
                    nama = student.nama,
                    domisili = student.domisili,
                    kelas = student.kelas,
                    no_imda = student.no_imda,
                    ruang_imda = student.ruang_imda
                )
            }
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudentRemote(webAppUrl: String, rowNum: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteStudentData(url = webAppUrl, rowNum = rowNum)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadTeacherDutyRemote(webAppUrl: String, rowNum: Int, subject: String, className: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addTeacherDuty(webAppUrl, rowNum = rowNum, newSubject = subject, newClass = className)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeacherDutyRemote(webAppUrl: String, rowNum: Int, indexToDelete: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteTeacherDuty(webAppUrl, rowNum = rowNum, indexToDelete = indexToDelete)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
