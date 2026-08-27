package com.rndm.app.data.update

import android.content.Context
import com.rndm.app.data.update.UpdateRepositoryImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class UpdateVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `getTempApkFile sanitizes versionName safely`() {
        val mockContext = mockk<Context>()
        val cacheDir = File("build/tmp/cache")
        every { mockContext.cacheDir } returns cacheDir

        val tempFile = UpdateRepositoryImpl.getTempApkFile(mockContext, "1.2.3/beta:test")
        assertEquals("rndm-update-1.2.3_beta_test.temp", tempFile.name)
    }

    @Test
    fun `verifyApkSha256 returns false for non-existent or tiny files`() {
        val mockContext = mockk<Context>(relaxed = true)
        val client = mockk<GitHubReleaseClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(mockContext, client)

        val nonExistentFile = File(tempFolder.root, "does_not_exist.apk")
        assertFalse(repository.verifyApkSha256(nonExistentFile, ""))

        val tinyHtmlErrorFile = tempFolder.newFile("error.apk").apply {
            writeText("<html><body>404 Not Found</body></html>")
        }
        assertFalse(repository.verifyApkSha256(tinyHtmlErrorFile, ""))
    }

    @Test
    fun `verifyApkSha256 returns false when zip structure is missing AndroidManifest or classes dex`() {
        val mockContext = mockk<Context>(relaxed = true)
        val client = mockk<GitHubReleaseClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(mockContext, client)

        // Create a dummy zip file larger than 500KB but without AndroidManifest.xml and classes.dex
        val fakeZip = tempFolder.newFile("fake.apk")
        ZipOutputStream(FileOutputStream(fakeZip)).apply {
            setLevel(java.util.zip.Deflater.NO_COMPRESSION)
        }.use { out ->
            out.putNextEntry(ZipEntry("test.txt"))
            val dummyBytes = ByteArray(600_000)
            out.write(dummyBytes)
            out.closeEntry()
        }

        assertTrue(fakeZip.length() >= 500_000L)
        assertFalse(repository.verifyApkSha256(fakeZip, ""))
    }

    @Test
    fun `UpdateManifestAdapter parses json correctly without reflection`() {
        val json = """
            {
                "versionCode": 8,
                "versionName": "1.0.7",
                "updateIdentity": 107,
                "publishedAt": "2026-08-27T10:00:00Z",
                "apkUrl": "https://example.com/app.apk",
                "apkSize": 15000000,
                "apkSha256": "abc123sha",
                "mandatory": true,
                "releaseNotes": "ميزات جديدة وإصلاحات"
            }
        """.trimIndent()

        val adapter = UpdateManifestAdapter()
        val manifest = adapter.fromJson(json)

        org.junit.Assert.assertNotNull(manifest)
        assertEquals(8, manifest?.versionCode)
        assertEquals("1.0.7", manifest?.versionName)
        assertEquals(107L, manifest?.updateIdentity)
        assertEquals(15000000L, manifest?.apkSize)
        assertEquals("abc123sha", manifest?.apkSha256)
        assertEquals(true, manifest?.mandatory)
        assertEquals("ميزات جديدة وإصلاحات", manifest?.releaseNotes)
    }

    @Test
    fun `GitHubReleaseResponseAdapter parses GitHub release payload correctly`() {
        val json = """
            {
                "tag_name": "v1.0.7",
                "name": "Release 1.0.7",
                "published_at": "2026-08-27T10:00:00Z",
                "body": "Fix update system",
                "assets": [
                    {
                        "name": "rndm-v1.0.7.apk",
                        "size": 18000000,
                        "browser_download_url": "https://github.com/downloads/rndm.apk"
                    }
                ]
            }
        """.trimIndent()

        val adapter = GitHubReleaseResponseAdapter()
        val response = adapter.fromJson(json)

        org.junit.Assert.assertNotNull(response)
        assertEquals("v1.0.7", response?.tagName)
        assertEquals(1, response?.assets?.size)
        assertEquals("rndm-v1.0.7.apk", response?.assets?.get(0)?.name)
        assertEquals(18000000L, response?.assets?.get(0)?.size)
    }
}
