package com.rndm.app.data.update

import com.rndm.app.domain.model.UpdateInfo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
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

class UpdateManifestAdapter : JsonAdapter<UpdateManifest>() {
    override fun fromJson(reader: JsonReader): UpdateManifest {
        var versionCode = 0
        var versionName = ""
        var updateIdentity = 0L
        var publishedAt = ""
        var apkUrl = ""
        var apkSize = 0L
        var apkSha256 = ""
        var mandatory = false
        var releaseNotes: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "versionCode" -> versionCode = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0 } else reader.nextInt()
                "versionName" -> versionName = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "updateIdentity" -> updateIdentity = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0L } else reader.nextLong()
                "publishedAt" -> publishedAt = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "apkUrl" -> apkUrl = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "apkSize" -> apkSize = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0L } else reader.nextLong()
                "apkSha256" -> apkSha256 = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "mandatory" -> mandatory = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); false } else reader.nextBoolean()
                "releaseNotes" -> releaseNotes = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return UpdateManifest(
            versionCode = versionCode,
            versionName = versionName,
            updateIdentity = updateIdentity,
            publishedAt = publishedAt,
            apkUrl = apkUrl,
            apkSize = apkSize,
            apkSha256 = apkSha256,
            mandatory = mandatory,
            releaseNotes = releaseNotes
        )
    }

    override fun toJson(writer: JsonWriter, value: UpdateManifest?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("versionCode").value(value.versionCode)
        writer.name("versionName").value(value.versionName)
        writer.name("updateIdentity").value(value.updateIdentity)
        writer.name("publishedAt").value(value.publishedAt)
        writer.name("apkUrl").value(value.apkUrl)
        writer.name("apkSize").value(value.apkSize)
        writer.name("apkSha256").value(value.apkSha256)
        writer.name("mandatory").value(value.mandatory)
        writer.name("releaseNotes").value(value.releaseNotes)
        writer.endObject()
    }
}

class GitHubAssetAdapter : JsonAdapter<GitHubAsset>() {
    override fun fromJson(reader: JsonReader): GitHubAsset {
        var name = ""
        var size = 0L
        var browserDownloadUrl = ""
        var digest: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> name = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "size" -> size = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0L } else reader.nextLong()
                "browser_download_url" -> browserDownloadUrl = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "digest" -> digest = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return GitHubAsset(name, size, browserDownloadUrl, digest)
    }

    override fun toJson(writer: JsonWriter, value: GitHubAsset?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("name").value(value.name)
        writer.name("size").value(value.size)
        writer.name("browser_download_url").value(value.browserDownloadUrl)
        writer.name("digest").value(value.digest)
        writer.endObject()
    }
}

class GitHubReleaseResponseAdapter(
    private val assetAdapter: JsonAdapter<GitHubAsset> = GitHubAssetAdapter()
) : JsonAdapter<GitHubReleaseResponse>() {
    override fun fromJson(reader: JsonReader): GitHubReleaseResponse {
        var tagName = ""
        var name: String? = null
        var publishedAt = ""
        var body: String? = null
        val assets = mutableListOf<GitHubAsset>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag_name" -> tagName = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "name" -> name = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                "published_at" -> publishedAt = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "body" -> body = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                "assets" -> {
                    if (reader.peek() == JsonReader.Token.NULL) {
                        reader.skipValue()
                    } else {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val asset = assetAdapter.fromJson(reader)
                            if (asset != null) {
                                assets.add(asset)
                            }
                        }
                        reader.endArray()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return GitHubReleaseResponse(tagName, name, publishedAt, body, assets)
    }

    override fun toJson(writer: JsonWriter, value: GitHubReleaseResponse?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("tag_name").value(value.tagName)
        writer.name("name").value(value.name)
        writer.name("published_at").value(value.publishedAt)
        writer.name("body").value(value.body)
        writer.name("assets")
        writer.beginArray()
        value.assets.forEach { assetAdapter.toJson(writer, it) }
        writer.endArray()
        writer.endObject()
    }
}

class UpdateInfoAdapter : JsonAdapter<UpdateInfo>() {
    override fun fromJson(reader: JsonReader): UpdateInfo {
        var hasUpdate = false
        var versionCode = 0
        var versionName = ""
        var updateIdentity = 0L
        var apkUrl = ""
        var apkSize = 0L
        var apkSha256 = ""
        var mandatory = false
        var releaseNotes: String? = null
        var publishedAt: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "hasUpdate" -> hasUpdate = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); false } else reader.nextBoolean()
                "versionCode" -> versionCode = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0 } else reader.nextInt()
                "versionName" -> versionName = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "updateIdentity" -> updateIdentity = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0L } else reader.nextLong()
                "apkUrl" -> apkUrl = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "apkSize" -> apkSize = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); 0L } else reader.nextLong()
                "apkSha256" -> apkSha256 = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); "" } else reader.nextString()
                "mandatory" -> mandatory = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); false } else reader.nextBoolean()
                "releaseNotes" -> releaseNotes = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                "publishedAt" -> publishedAt = if (reader.peek() == JsonReader.Token.NULL) { reader.skipValue(); null } else reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return UpdateInfo(
            hasUpdate = hasUpdate,
            versionCode = versionCode,
            versionName = versionName,
            updateIdentity = updateIdentity,
            apkUrl = apkUrl,
            apkSize = apkSize,
            apkSha256 = apkSha256,
            mandatory = mandatory,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt
        )
    }

    override fun toJson(writer: JsonWriter, value: UpdateInfo?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("hasUpdate").value(value.hasUpdate)
        writer.name("versionCode").value(value.versionCode)
        writer.name("versionName").value(value.versionName)
        writer.name("updateIdentity").value(value.updateIdentity)
        writer.name("apkUrl").value(value.apkUrl)
        writer.name("apkSize").value(value.apkSize)
        writer.name("apkSha256").value(value.apkSha256)
        writer.name("mandatory").value(value.mandatory)
        writer.name("releaseNotes").value(value.releaseNotes)
        writer.name("publishedAt").value(value.publishedAt)
        writer.endObject()
    }
}
