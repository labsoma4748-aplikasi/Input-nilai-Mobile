package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SyncApiService {
    @GET
    suspend fun getSheetData(
        @Url url: String,
        @Query("action") action: String = "getData"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun updateStudentScore(
        @Url url: String,
        @Field("action") action: String = "updateStudentScore",
        @Field("studentIdPps") studentIdPps: String,
        @Field("subjectName") subjectName: String,
        @Field("scoreValue") scoreValue: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun addTeacherDuty(
        @Url url: String,
        @Field("action") action: String = "addTeacherDuty",
        @Field("rowNum") rowNum: Int,
        @Field("newSubject") newSubject: String,
        @Field("newClass") newClass: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun deleteTeacherDuty(
        @Url url: String,
        @Field("action") action: String = "deleteTeacherDuty",
        @Field("rowNum") rowNum: Int,
        @Field("indexToDelete") indexToDelete: Int
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun addStudentData(
        @Url url: String,
        @Field("action") action: String = "addStudentData",
        @Field("no") no: String,
        @Field("abs") abs: String,
        @Field("id_pps") id_pps: String,
        @Field("nama") nama: String,
        @Field("domisili") domisili: String,
        @Field("kelas") kelas: String,
        @Field("no_imda") no_imda: String,
        @Field("ruang_imda") ruang_imda: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun updateStudentData(
        @Url url: String,
        @Field("action") action: String = "updateStudentData",
        @Field("rowNum") rowNum: Int,
        @Field("no") no: String,
        @Field("abs") abs: String,
        @Field("id_pps") id_pps: String,
        @Field("nama") nama: String,
        @Field("domisili") domisili: String,
        @Field("kelas") kelas: String,
        @Field("no_imda") no_imda: String,
        @Field("ruang_imda") ruang_imda: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun deleteStudentData(
        @Url url: String,
        @Field("action") action: String = "deleteStudentData",
        @Field("rowNum") rowNum: Int
    ): Response<ResponseBody>
}
