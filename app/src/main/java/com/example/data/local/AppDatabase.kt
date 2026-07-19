package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Student Queries
    @Query("SELECT * FROM students ORDER BY rowNum ASC")
    fun getAllStudentsFlow(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY rowNum ASC")
    suspend fun getAllStudents(): List<Student>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("DELETE FROM students WHERE rowNum = :rowNum")
    suspend fun deleteStudentByRowNum(rowNum: Int)

    @Query("DELETE FROM students")
    suspend fun clearAllStudents()

    // Score Queries
    @Query("SELECT * FROM student_scores")
    fun getAllScoresFlow(): Flow<List<StudentScore>>

    @Query("SELECT * FROM student_scores")
    suspend fun getAllScores(): List<StudentScore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<StudentScore>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: StudentScore)

    @Query("DELETE FROM student_scores WHERE studentIdPps = :studentIdPps")
    suspend fun deleteScoresForStudent(studentIdPps: String)

    @Query("DELETE FROM student_scores")
    suspend fun clearAllScores()

    // Teacher Queries
    @Query("SELECT * FROM teachers ORDER BY rowNum ASC")
    fun getAllTeachersFlow(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers ORDER BY rowNum ASC")
    suspend fun getAllTeachers(): List<Teacher>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Query("DELETE FROM teachers")
    suspend fun clearAllTeachers()

    // Duty Queries
    @Query("SELECT * FROM teacher_duties")
    fun getAllDutiesFlow(): Flow<List<TeacherDuty>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuties(duties: List<TeacherDuty>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuty(duty: TeacherDuty)

    @Query("DELETE FROM teacher_duties WHERE teacherRowNum = :rowNum AND subject = :subject AND className = :className")
    suspend fun deleteDuty(rowNum: Int, subject: String, className: String)

    @Query("DELETE FROM teacher_duties")
    suspend fun clearAllDuties()
}

@Database(
    entities = [
        Student::class,
        StudentScore::class,
        Teacher::class,
        TeacherDuty::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
