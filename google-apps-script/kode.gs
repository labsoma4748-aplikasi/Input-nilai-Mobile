/**
 * GOOGLE APPS SCRIPT (kode.gs)
 * Untuk Sinkronisasi Aplikasi Input Nilai IMDA (Materi Ujian & Nilai Kelas Tsanawiyah)
 * 
 * Silakan salin semua kode ini dan tempelkan ke editor Google Apps Script Anda:
 * 1. Buka Spreadsheet Google Anda.
 * 2. Klik Menu "Ekstensi" (Extensions) -> "Apps Script".
 * 3. Hapus semua kode default di editor, lalu paste kode ini.
 * 4. Klik ikon Simpan (Save).
 * 5. Klik tombol "Terapkan" (Deploy) -> "Terapkan Baru" (New deployment).
 * 6. Pilih jenis: "Aplikasi Web" (Web app).
 * 7. Konfigurasi:
 *    - Jalankan sebagai: "Saya" (Me / Akun Google Anda)
 *    - Siapa yang memiliki akses: "Siapa saja" (Anyone) -> SANGAT PENTING agar Android bisa mengakses!
 * 8. Klik "Terapkan" (Deploy) dan salin URL Aplikasi Web yang diberikan.
 * 9. Tempelkan URL tersebut ke tab Setelan (Settings) di aplikasi Android Anda.
 */

// Konstanta Nama Sheet
var SHEET_SISWA = "Siswa";
var SHEET_GURU = "Guru";

// Helper untuk mendapatkan atau membuat Sheet jika belum ada
function getOrCreateSheet(sheetName, defaultIndex) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    var sheets = ss.getSheets();
    if (sheets.length > defaultIndex) {
      sheet = sheets[defaultIndex];
      sheet.setName(sheetName);
    } else {
      sheet = ss.insertSheet(sheetName);
    }
  }
  return sheet;
}

// Handler GET request (Mendapatkan data Siswa & Nilai)
function doGet(e) {
  try {
    var action = e.parameter.action || "getData";
    
    if (action === "getData") {
      var sheet = getOrCreateSheet(SHEET_SISWA, 0);
      var lastRow = sheet.getLastRow();
      var lastCol = sheet.getLastColumn();
      
      if (lastRow === 0) {
        return createJsonResponse([]);
      }
      
      // Ambil seluruh data rentang aktif
      var values = sheet.getRange(1, 1, lastRow, lastCol).getValues();
      
      // Filter baris kosong agar data bersih
      var cleanValues = values.filter(function(row) {
        return row.length > 0 && row[0].toString().trim() !== "";
      });
      
      return createJsonResponse(cleanValues);
    }
    
    return createJsonResponse({ status: "error", message: "Aksi GET tidak dikenali" });
  } catch (error) {
    return createJsonResponse({ status: "error", message: error.toString() });
  }
}

// Handler POST request (Update Nilai, Kelola Guru/Tugas, Kelola Data Siswa)
function doPost(e) {
  try {
    // Parsing parameter POST dari url-encoded form data maupun JSON payload
    var params = {};
    if (e.parameter && Object.keys(e.parameter).length > 0) {
      params = e.parameter;
    } else if (e.postData && e.postData.contents) {
      try {
        params = JSON.parse(e.postData.contents);
      } catch (err) {
        // Fallback jika bukan JSON
      }
    }
    
    var action = params.action;
    if (!action) {
      return createJsonResponse({ status: "error", message: "Parameter 'action' tidak ditemukan!" });
    }
    
    // 1. UPDATE NILAI SISWA
    if (action === "updateStudentScore") {
      var studentIdPps = params.studentIdPps;
      var subjectName = params.subjectName;
      var scoreValue = params.scoreValue;
      
      if (!studentIdPps || !subjectName) {
        return createJsonResponse({ status: "error", message: "Parameter studentIdPps atau subjectName kurang!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_SISWA, 0);
      var lastRow = sheet.getLastRow();
      var lastCol = sheet.getLastColumn();
      
      if (lastRow < 2) {
        return createJsonResponse({ status: "error", message: "Sheet Siswa masih kosong!" });
      }
      
      // Ambil Header (Baris ke-1)
      var headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0];
      var subjectColIndex = -1;
      
      for (var i = 0; i < headers.length; i++) {
        if (headers[i].toString().trim().toLowerCase() === subjectName.trim().toLowerCase()) {
          subjectColIndex = i + 1; // spreadsheet 1-indexed
          break;
        }
      }
      
      // Jika kolom mata pelajaran belum ada, buat kolom baru di ujung kanan
      if (subjectColIndex === -1) {
        subjectColIndex = lastCol + 1;
        sheet.getRange(1, subjectColIndex).setValue(subjectName);
      }
      
      // Cari baris berdasarkan ID PPS (Kolom C / Ke-3)
      var idValues = sheet.getRange(1, 3, lastRow, 1).getValues();
      var targetRowIndex = -1;
      
      for (var r = 1; r < idValues.length; r++) { // Skip header
        if (idValues[r][0].toString().trim() === studentIdPps.trim()) {
          targetRowIndex = r + 1;
          break;
        }
      }
      
      if (targetRowIndex !== -1) {
        // Tulis nilai ke sel tujuan
        var finalVal = (scoreValue === "" || scoreValue === null || scoreValue === undefined) ? "" : parseFloat(scoreValue);
        sheet.getRange(targetRowIndex, subjectColIndex).setValue(finalVal);
        return createJsonResponse({ status: "success", message: "Nilai berhasil diperbarui!" });
      } else {
        return createJsonResponse({ status: "error", message: "Siswa dengan ID PPS: " + studentIdPps + " tidak ditemukan!" });
      }
    }
    
    // 2. TAMBAH TUGAS MENGAJAR GURU (DUTY)
    else if (action === "addTeacherDuty") {
      var rowNum = parseInt(params.rowNum);
      var newSubject = params.newSubject;
      var newClass = params.newClass;
      
      if (isNaN(rowNum) || !newSubject || !newClass) {
        return createJsonResponse({ status: "error", message: "Parameter untuk tambah tugas tidak valid!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_GURU, 1);
      
      // Kolom G (Tugas_Pelajaran) = indeks ke-7
      // Kolom H (Tugas_Kelas) = indeks ke-8
      var currentSubjects = sheet.getRange(rowNum, 7).getValue().toString().trim();
      var currentClasses = sheet.getRange(rowNum, 8).getValue().toString().trim();
      
      var updatedSubjects = currentSubjects ? currentSubjects + ", " + newSubject : newSubject;
      var updatedClasses = currentClasses ? currentClasses + ", " + newClass : newClass;
      
      sheet.getRange(rowNum, 7).setValue(updatedSubjects);
      sheet.getRange(rowNum, 8).setValue(updatedClasses);
      
      return createJsonResponse({ status: "success", message: "Tugas mengajar berhasil ditambahkan!" });
    }
    
    // 3. HAPUS TUGAS MENGAJAR GURU (DUTY)
    else if (action === "deleteTeacherDuty") {
      var rowNum = parseInt(params.rowNum);
      var indexToDelete = parseInt(params.indexToDelete);
      
      if (isNaN(rowNum) || isNaN(indexToDelete)) {
        return createJsonResponse({ status: "error", message: "Parameter hapus tugas tidak valid!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_GURU, 1);
      
      var currentSubjects = sheet.getRange(rowNum, 7).getValue().toString().trim();
      var currentClasses = sheet.getRange(rowNum, 8).getValue().toString().trim();
      
      var subjectList = currentSubjects ? currentSubjects.split(",").map(function(s) { return s.trim(); }) : [];
      var classList = currentClasses ? currentClasses.split(",").map(function(c) { return c.trim(); }) : [];
      
      if (indexToDelete >= 0 && indexToDelete < subjectList.length) {
        subjectList.splice(indexToDelete, 1);
      }
      if (indexToDelete >= 0 && indexToDelete < classList.length) {
        classList.splice(indexToDelete, 1);
      }
      
      sheet.getRange(rowNum, 7).setValue(subjectList.join(", "));
      sheet.getRange(rowNum, 8).setValue(classList.join(", "));
      
      return createJsonResponse({ status: "success", message: "Tugas mengajar berhasil dihapus!" });
    }
    
    // 4. TAMBAH DATA SISWA BARU
    else if (action === "addStudentData") {
      var no = params.no || "";
      var abs = params.abs || "";
      var id_pps = params.id_pps || "";
      var nama = params.nama || "";
      var domisili = params.domisili || "";
      var kelas = params.kelas || "";
      var no_imda = params.no_imda || "";
      var ruang_imda = params.ruang_imda || "";
      
      if (!id_pps || !nama) {
        return createJsonResponse({ status: "error", message: "ID PPS dan Nama Siswa wajib diisi!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_SISWA, 0);
      var lastCol = Math.max(8, sheet.getLastColumn());
      
      var newRow = [no, abs, id_pps, nama, domisili, kelas, no_imda, ruang_imda];
      // Isi sisa kolom pelajaran dengan string kosong
      while (newRow.length < lastCol) {
        newRow.push("");
      }
      
      sheet.appendRow(newRow);
      return createJsonResponse({ status: "success", message: "Data siswa berhasil ditambahkan!" });
    }
    
    // 5. UPDATE DATA SISWA
    else if (action === "updateStudentData") {
      var rowNum = parseInt(params.rowNum);
      var no = params.no || "";
      var abs = params.abs || "";
      var id_pps = params.id_pps || "";
      var nama = params.nama || "";
      var domisili = params.domisili || "";
      var kelas = params.kelas || "";
      var no_imda = params.no_imda || "";
      var ruang_imda = params.ruang_imda || "";
      
      if (isNaN(rowNum) || !id_pps || !nama) {
        return createJsonResponse({ status: "error", message: "Parameter tidak valid untuk update data siswa!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_SISWA, 0);
      sheet.getRange(rowNum, 1, 1, 8).setValues([[no, abs, id_pps, nama, domisili, kelas, no_imda, ruang_imda]]);
      
      return createJsonResponse({ status: "success", message: "Data siswa berhasil diperbarui!" });
    }
    
    // 6. HAPUS DATA SISWA
    else if (action === "deleteStudentData") {
      var rowNum = parseInt(params.rowNum);
      
      if (isNaN(rowNum)) {
        return createJsonResponse({ status: "error", message: "Parameter rowNum tidak valid!" });
      }
      
      var sheet = getOrCreateSheet(SHEET_SISWA, 0);
      if (rowNum > 1 && rowNum <= sheet.getLastRow()) {
        sheet.deleteRow(rowNum);
        return createJsonResponse({ status: "success", message: "Data siswa berhasil dihapus dari sheet!" });
      } else {
        return createJsonResponse({ status: "error", message: "Row number di luar jangkauan!" });
      }
    }
    
    return createJsonResponse({ status: "error", message: "Aksi POST tidak dikenal!" });
  } catch (error) {
    return createJsonResponse({ status: "error", message: error.toString() });
  }
}

// Helper untuk membuat response JSON yang diizinkan CORS
function createJsonResponse(data) {
  var JSONString = JSON.stringify(data);
  return ContentService.createTextOutput(JSONString)
    .setMimeType(ContentService.MimeType.JSON);
}
