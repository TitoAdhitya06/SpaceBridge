package com.example.spacebridgeapp.core.data.drive

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Serializable
data class DriveFileList(
    val files: List<DriveFileItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class DriveFileItem(
    val id: String,
    val name: String? = null,
    val mimeType: String? = null,
    val size: Long? = null,
    val md5Checksum: String? = null,
    val webViewLink: String? = null,
    val trashed: Boolean? = null,
    val appProperties: Map<String, String>? = null,
)

@Serializable
data class CreateFileRequest(
    val name: String,
    val mimeType: String? = null,
    val parents: List<String>? = null,
    val appProperties: Map<String, String>? = null,
)

@Serializable
data class UpdateFileRequest(
    val name: String? = null,
    val trashed: Boolean? = null,
)

@Serializable
data class CreatePermissionRequest(
    val type: String = "anyone",
    val role: String = "reader",
)

@Serializable
data class PermissionResponse(
    val id: String,
)

@Serializable
data class AboutResponse(
    val storageQuota: StorageQuota? = null,
)

@Serializable
data class StorageQuota(
    val limit: Long? = null,
    val usage: Long? = null,
    val usageInDrive: Long? = null,
    val usageInDriveTrash: Long? = null,
)

interface DriveApi {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") auth: String,
        @Query("q") query: String? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("fields") fields: String? = null,
        @Query("pageToken") pageToken: String? = null,
    ): DriveFileList

    @POST("drive/v3/files")
    suspend fun createFile(
        @Header("Authorization") auth: String,
        @Query("fields") fields: String? = "id,name,mimeType,appProperties",
        @Body request: CreateFileRequest,
    ): DriveFileItem

    @GET("drive/v3/files/{id}")
    suspend fun getFile(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
        @Query("fields") fields: String? = "id,name,mimeType,size,md5Checksum,webViewLink,trashed,appProperties",
    ): DriveFileItem

    @POST("upload/drive/v3/files?uploadType=resumable")
    suspend fun initiateResumableUpload(
        @Header("Authorization") auth: String,
        @Header("X-Upload-Content-Type") uploadContentType: String,
        @Header("X-Upload-Content-Length") uploadContentLength: Long,
        @Body request: CreateFileRequest,
    ): Response<Unit>

    @Streaming
    @GET("drive/v3/files/{id}?alt=media")
    suspend fun downloadFile(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
    ): ResponseBody

    @PATCH("drive/v3/files/{id}")
    suspend fun updateFile(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
        @Query("fields") fields: String? = "id,name,trashed",
        @Body request: UpdateFileRequest,
    ): DriveFileItem

    @DELETE("drive/v3/files/{id}")
    suspend fun deleteFile(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
    ): Response<Unit>

    @POST("drive/v3/files/{id}/permissions")
    suspend fun createPermission(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
        @Query("fields") fields: String? = "id",
        @Body request: CreatePermissionRequest = CreatePermissionRequest(),
    ): PermissionResponse

    @DELETE("drive/v3/files/{id}/permissions/{permissionId}")
    suspend fun deletePermission(
        @Header("Authorization") auth: String,
        @Path("id") fileId: String,
        @Path("permissionId") permissionId: String,
    ): Response<Unit>

    @GET("drive/v3/about")
    suspend fun getAbout(
        @Header("Authorization") auth: String,
        @Query("fields") fields: String? = "storageQuota",
    ): AboutResponse
}
