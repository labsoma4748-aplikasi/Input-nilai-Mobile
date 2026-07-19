package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val rowNum: Int,
    val no: String,
    val abs: String,
    val id_pps: String,
    val nama: String,
    val domisili: String,
    val kelas: String,
    val no_imda: String,
    val ruang_imda: String
)

@Entity(tableName = "student_scores", primaryKeys = ["studentIdPps", "subject"])
data class StudentScore(
    val studentIdPps: String,
    val subject: String,
    val score: Double?
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val rowNum: Int, // Rows in User_Guru sheet (starts at 2)
    val nama: String,
    val username: String,
    val password: String,
    val role: String, // "ADMIN" or "GURU"
    val startDate: String, // ISO Date string or format
    val endDate: String
)

@Entity(tableName = "teacher_duties", primaryKeys = ["teacherRowNum", "subject", "className"])
data class TeacherDuty(
    val teacherRowNum: Int,
    val subject: String,
    val className: String
)
