package com.example.data.mock

import com.example.data.model.*

object MockData {
    val defaultTeachers = listOf(
        Teacher(
            rowNum = 2,
            nama = "Ustadz H. Ahmad Ridho, Lc.",
            username = "admin",
            password = "123",
            role = "ADMIN",
            startDate = "2026-07-01",
            endDate = "2026-08-31"
        ),
        Teacher(
            rowNum = 3,
            nama = "Ustadz Abdul Wahab, S.Pd.I.",
            username = "guru",
            password = "123",
            role = "GURU",
            startDate = "2026-07-01",
            endDate = "2026-08-31"
        ),
        Teacher(
            rowNum = 4,
            nama = "Ustadzah Siti Fatimah, S.S.",
            username = "guru2",
            password = "123",
            role = "GURU",
            startDate = "2026-07-01",
            endDate = "2026-08-31"
        ),
        // Teachers from Google Sheet
        Teacher(rowNum = 5, nama = "ALI ABDILLAH", username = "301", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 6, nama = "ANGGI PRADANA", username = "302", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 7, nama = "M. JUNAIDI SANURIP", username = "303", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 8, nama = "A. BASHORI SYAFI'I", username = "304", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 9, nama = "MAHBUBILLAH", username = "305", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 10, nama = "M. AMINULLOH YAHYA", username = "306", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 11, nama = "KHOLIL SALAM", username = "307", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 12, nama = "LUKMAN ANIS", username = "308", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 13, nama = "M. FAUZAN IMRON", username = "101", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 14, nama = "LUKMAN HAKIM", username = "4001", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 15, nama = "MUHAMMAD FIRDAUS SHOLEH", username = "4002", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 16, nama = "MOHAMMAD SA'DULLOH", username = "4003", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 17, nama = "IMADUDDIN", username = "4004", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 18, nama = "M. SUYUTI", username = "4005", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 19, nama = "QOSIM ANWARI", username = "4006", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 20, nama = "GHOZALI ALI", username = "4007", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 21, nama = "ABD. ROHIM", username = "4008", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 22, nama = "SAHLAN", username = "4009", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 23, nama = "M. WAHYU HIDAYAT", username = "4010", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 24, nama = "AHMAD KHOLIL", username = "4011", password = "1234", role = "GURU", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 25, nama = "MOH KARROR", username = "4123", password = "1234", role = "ADMIN", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 26, nama = "ABD. ALIM", username = "4111", password = "1234", role = "ADMIN", startDate = "2026-06-28", endDate = "2026-07-25"),
        Teacher(rowNum = 27, nama = "MOH SAHLAN", username = "4000", password = "1234", role = "ADMIN", startDate = "2026-06-28", endDate = "2026-07-25")
    )

    val defaultDuties = listOf(
        // M. FAUZAN IMRON (101) - rowNum 13
        TeacherDuty(13, "Tafsir", "1"),

        // LUKMAN HAKIM (4001) - rowNum 14
        TeacherDuty(14, "Ilmu Akhlaq", "1"),
        TeacherDuty(14, "Tarikh Islami (Sejarah Islam)", "2"),
        TeacherDuty(14, "Fikih", "3"),
        TeacherDuty(14, "I", "3"),

        // MUHAMMAD FIRDAUS SHOLEH (4002) - rowNum 15
        TeacherDuty(15, "Qaidah Fikih", "1"),
        TeacherDuty(15, "Tauhid", "2"),
        TeacherDuty(15, "Ilmu Mantiq", "2"),
        TeacherDuty(15, "Balaghah", "3"),

        // MOHAMMAD SA'DULLOH (4003) - rowNum 16
        TeacherDuty(16, "Ushul Fikih", "1"),
        TeacherDuty(16, "Balaghah", "1"),
        TeacherDuty(16, "Qaidah Fikih", "2"),
        TeacherDuty(16, "Nahwu", "3"),

        // IMADUDDIN (4004) - rowNum 17
        TeacherDuty(17, "Imla'", "1"),
        TeacherDuty(17, "Nahwu", "2"),
        TeacherDuty(17, "Mushthalah Hadits", "2"),
        TeacherDuty(17, "Ushul Fikih", "3"),

        // M. SUYUTI (4005) - rowNum 18
        TeacherDuty(18, "Nahwu", "1"),
        TeacherDuty(18, "Ilmu Akhlaq", "2"),
        TeacherDuty(18, "Hadits Nabawy", "3"),
        TeacherDuty(18, "Tarikh Islam", "3"),

        // QOSIM ANWARI (4006) - rowNum 19
        TeacherDuty(19, "Fikih", "1"),
        TeacherDuty(19, "Faraidh", "1"),
        TeacherDuty(19, "balaghah", "2"),
        TeacherDuty(19, "Ilmu Tafsir", "3"),

        // GHOZALI ALI (4007) - rowNum 20
        TeacherDuty(20, "Hadits Nabawy", "2"),
        TeacherDuty(20, "Qaidah Fikih", "3"),

        // ABD. ROHIM (4008) - rowNum 21
        TeacherDuty(21, "Tauhid", "1"),
        TeacherDuty(21, "Imla'", "2"),
        TeacherDuty(21, "Tafsir", "2"),
        TeacherDuty(21, "Ilmu Akhlaq", "3"),

        // SAHLAN (4009) - rowNum 22
        TeacherDuty(22, "Falak/Hisab", "1"),
        TeacherDuty(22, "Falak/Hisab", "2"),
        TeacherDuty(22, "Falak/Hisab", "3"),

        // M. WAHYU HIDAYAT (4010) - rowNum 23
        TeacherDuty(23, "Tarikh Islami (Sejarah Islam)", "1"),
        TeacherDuty(23, "Fikih", "2"),
        TeacherDuty(23, "Tauhid", "3"),
        TeacherDuty(23, "Tafsir", "3"),

        // AHMAD KHOLIL (4011) - rowNum 24
        TeacherDuty(24, "Hadits Nabawy", "1"),
        TeacherDuty(24, "Ushul Fikih", "2")
    )

    val defaultSubjects = listOf(
        "Tafsir",
        "Ilmu Akhlaq",
        "Tarikh Islami (Sejarah Islam)",
        "Fikih",
        "I",
        "Qaidah Fikih",
        "Tauhid",
        "Ilmu Mantiq",
        "Balaghah",
        "Ushul Fikih",
        "Nahwu",
        "Imla'",
        "Mushthalah Hadits",
        "Hadits Nabawy",
        "Tarikh Islam",
        "Faraidh",
        "balaghah",
        "Ilmu Tafsir",
        "Falak/Hisab"
    )

    val defaultStudents = listOf(
        Student(2, "1", "1", "PPS-001", "AHMAD FAUZI AL-ANSARI", "Gedung A (01)", "1A", "IMDA-101", "1"),
        Student(3, "2", "2", "PPS-002", "MUHAMMAD YUSUF", "Gedung A (02)", "1A", "IMDA-102", "1"),
        Student(4, "3", "3", "PPS-003", "SITI AMINAH AZ-ZAHRA", "Gedung B (05)", "1A", "IMDA-103", "1"),
        Student(5, "4", "4", "PPS-004", "ABDUL HAMID LUBIS", "Gedung A (03)", "1A", "IMDA-104", "1"),
        Student(6, "5", "5", "PPS-005", "FATIMAH AZ-ZAHRA", "Gedung B (06)", "1A", "IMDA-105", "1"),
        Student(7, "6", "6", "PPS-006", "LUQMAN HAKIM AL-FIKRI", "Gedung A (04)", "1A", "IMDA-106", "1"),
        Student(8, "7", "7", "PPS-007", "AISYAH HUMAIRAH", "Gedung B (07)", "1A", "IMDA-107", "1"),
        Student(9, "8", "8", "PPS-008", "MUHAMMAD RIZAL", "Gedung A (05)", "1A", "IMDA-108", "1"),
        Student(10, "9", "9", "PPS-009", "ZULFA NUR FAUZIAH", "Gedung B (08)", "1A", "IMDA-109", "1"),
        Student(11, "10", "10", "PPS-010", "ZAKIYUDDIN JAFAR", "Gedung A (06)", "1A", "IMDA-110", "1"),
        
        Student(12, "11", "1", "PPS-011", "ALI IMRON HARAHAP", "Gedung A (07)", "1B", "IMDA-201", "2"),
        Student(13, "12", "2", "PPS-012", "KHADIJAH KUBRA", "Gedung B (01)", "1B", "IMDA-202", "2"),
        Student(14, "13", "3", "PPS-013", "ABDULLAH MAKRUF", "Gedung A (08)", "1B", "IMDA-203", "2"),
        Student(15, "14", "4", "PPS-014", "NURUL HIDAYATI", "Gedung B (02)", "1B", "IMDA-204", "2"),
        Student(16, "15", "5", "PPS-015", "HASAN BASRI", "Gedung A (09)", "1B", "IMDA-205", "2"),
        Student(17, "16", "6", "PPS-016", "HUSEIN AL-MUJAHID", "Gedung A (10)", "1B", "IMDA-206", "2"),
        Student(18, "17", "7", "PPS-017", "SOFIA AL-MUNAWAROH", "Gedung B (03)", "1B", "IMDA-207", "2"),
        Student(19, "18", "8", "PPS-018", "IBRAHIM AL-KHALIL", "Gedung A (11)", "1B", "IMDA-208", "2"),
        Student(20, "19", "9", "PPS-019", "AISYAH NILA SARI", "Gedung B (04)", "1B", "IMDA-209", "2"),
        Student(21, "20", "10", "PPS-020", "UMAR BIN KHATTAB", "Gedung A (12)", "1B", "IMDA-210", "2"),

        Student(22, "21", "1", "PPS-021", "ABU BAKAR SHIDDIQ", "Gedung C (01)", "2A", "IMDA-301", "3"),
        Student(23, "22", "2", "PPS-022", "UTSMAN BIN AFFAN", "Gedung C (02)", "2A", "IMDA-302", "3"),
        Student(24, "23", "3", "PPS-023", "ALI BIN ABI THALIB", "Gedung C (03)", "2A", "IMDA-303", "3"),
        Student(25, "24", "4", "PPS-024", "HAMZAH BIN ABDUL MUTHALIB", "Gedung C (04)", "2A", "IMDA-304", "3"),
        Student(26, "25", "5", "PPS-025", "BILAL BIN RABAH", "Gedung C (05)", "2A", "IMDA-305", "3"),

        Student(27, "26", "1", "PPS-026", "FATIMAH AZ-ZAHRA S.", "Gedung D (01)", "2B", "IMDA-401", "4"),
        Student(28, "27", "2", "PPS-027", "RUQAYYAH BINTI MUHAMMAD", "Gedung D (02)", "2B", "IMDA-402", "4"),
        Student(29, "28", "3", "PPS-028", "UMMU KULSUM BINTI MUHAMMAD", "Gedung D (03)", "2B", "IMDA-403", "4"),
        Student(30, "29", "4", "PPS-029", "ZAINAB BINTI MUHAMMAD", "Gedung D (04)", "2B", "IMDA-404", "4"),
        Student(31, "30", "5", "PPS-030", "MARIA AL-QIBTHIYAH", "Gedung D (05)", "2B", "IMDA-405", "4")
    )

    val defaultScores = listOf(
        StudentScore("PPS-001", "Fiqih", 92.0),
        StudentScore("PPS-001", "Tauhid", 85.0),
        StudentScore("PPS-002", "Fiqih", 78.0),
        StudentScore("PPS-002", "Tauhid", 74.0),
        StudentScore("PPS-003", "Fiqih", 95.0),
        StudentScore("PPS-003", "Tauhid", 98.0),
        StudentScore("PPS-004", "Fiqih", 60.0),
        StudentScore("PPS-004", "Tauhid", 65.0),
        StudentScore("PPS-005", "Fiqih", 88.0),
        StudentScore("PPS-005", "Tauhid", 82.0),
        StudentScore("PPS-006", "Fiqih", 25.0), // Example of a critical student score
        StudentScore("PPS-006", "Tauhid", 40.0),
        StudentScore("PPS-007", "Fiqih", 81.0),
        StudentScore("PPS-007", "Tauhid", 87.0),
        StudentScore("PPS-008", "Fiqih", 73.0),
        StudentScore("PPS-008", "Tauhid", 76.0),
        StudentScore("PPS-011", "Fiqih", 85.0),
        StudentScore("PPS-011", "Tauhid", 90.0),
        StudentScore("PPS-012", "Fiqih", 72.0),
        StudentScore("PPS-012", "Tauhid", 80.0),
        StudentScore("PPS-013", "Fiqih", 15.0), // Another critical student score
        StudentScore("PPS-013", "Tauhid", 28.0)
    )
}
