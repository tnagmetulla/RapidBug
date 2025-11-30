package com.tnagmetulla.rapidbug.api

import com.tnagmetulla.rapidbug.model.CreateIssueRequest
import com.tnagmetulla.rapidbug.model.CreateIssueResponse
import com.tnagmetulla.rapidbug.model.ProjectsResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface JiraApi {

    @GET("rest/api/3/project/search")
    suspend fun getProjects(
        @Header("Authorization") authHeader: String,
        @Query("maxResults") maxResults: Int = 50
    ): Response<ProjectsResponse>

    @POST("rest/api/3/issue")
    suspend fun createIssue(
        @Header("Authorization") authHeader: String,
        @Body request: CreateIssueRequest
    ): Response<CreateIssueResponse>

    @Multipart
    @POST("rest/api/3/issue/{issueKey}/attachments")
    suspend fun addAttachment(
        @Header("Authorization") authHeader: String,
        @Path("issueKey") issueKey: String,
        @Part file: MultipartBody.Part,
        @Header("X-Atlassian-Token") token: String = "no-check"
    ): Response<Void>
}