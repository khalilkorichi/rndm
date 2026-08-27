package com.rndm.app.data.update

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

data class UpdateManifest(
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "versionName") val versionName: String,
    @Json(name = "updateIdentity") val updateIdentity: Long,
    @Json(name = "publishedAt") val publishedAt: String,
    @Json(name = "apkUrl") val apkUrl: String,
    @Json(name = "apkSize") val apkSize: Long,
    @Json(name = "apkSha256") val apkSha256: String,
    @Json(name = "mandatory") val mandatory: Boolean = false,
    @Json(name = "releaseNotes") val releaseNotes: String? = null
)

data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "published_at") val publishedAt: String,
    @Json(name = "body") val body: String?,
    @Json(name = "assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @Json(name = "name") val name: String,
    @Json(name = "size") val size: Long,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "digest") val digest: String? = null
)

interface GitHubReleaseClient {
    @Headers("Cache-Control: no-cache, no-store, must-revalidate")
    @GET
    suspend fun fetchUpdateManifest(@Url url: String): UpdateManifest

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun fetchLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseResponse
}
