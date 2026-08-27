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
}
