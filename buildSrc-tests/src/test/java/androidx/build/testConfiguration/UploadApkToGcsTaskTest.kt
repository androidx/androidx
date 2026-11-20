/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.testConfiguration

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import org.gradle.api.services.BuildServiceParameters
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UploadApkToGcsTaskTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun gcsApkObjectPath_matchesAndroidxDevFormat() {
        assertThat(gcsApkObjectPath("core_core-releaseAndroidTest.apk", "abc123"))
            .isEqualTo("github-ci-action/core_core-releaseAndroidTest/abc123.apk")
    }

    @Test
    fun gcsApkObjectPath_convertsExtraDotsToSlashes() {
        assertThat(gcsApkObjectPath("foo.bar-debug.apk", "deadbeef"))
            .isEqualTo("github-ci-action/foo/bar-debug/deadbeef.apk")
    }

    @Test
    fun uploadApk_uploadsWhenMissing() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        val uploaded = mutableListOf<BlobId>()
        val service =
            testService(
                onExists = { false },
                onUpload = { blobId, file ->
                    assertThat(file).isEqualTo(apk)
                    uploaded.add(blobId)
                },
            )

        service.use { it.uploadApk("github-ci-action/test/abc.apk", apk) }

        assertThat(uploaded)
            .containsExactly(
                BlobId.of(ApkGcsUploadService.GCS_BUCKET_NAME, "github-ci-action/test/abc.apk")
            )
    }

    @Test
    fun uploadApk_skipsWhenAlreadyExists() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        var uploadCalls = 0
        val service = testService(onExists = { true }, onUpload = { _, _ -> uploadCalls++ })

        service.use { it.uploadApk("github-ci-action/test/abc.apk", apk) }

        assertThat(uploadCalls).isEqualTo(0)
    }

    @Test
    fun uploadApk_ignoresHttp412PreconditionRaceAndKeepsUploading() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        var uploadCalls = 0
        val service =
            testService(
                onExists = { false },
                onUpload = { _, _ ->
                    uploadCalls++
                    if (uploadCalls == 1) throw StorageException(412, "Precondition Failed")
                },
            )

        service.use {
            it.uploadApk("github-ci-action/a/1.apk", apk)
            it.uploadApk("github-ci-action/b/2.apk", apk)
        }

        assertThat(uploadCalls).isEqualTo(2)
    }

    @Test
    fun uploadApk_disablesRemainingUploadsWhenCredentialsMissing() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        var factoryCalls = 0
        val service =
            testService(
                storageFactory = {
                    factoryCalls++
                    throw IOException("Default credentials not found")
                }
            )

        service.use {
            it.uploadApk("github-ci-action/a/1.apk", apk)
            it.uploadApk("github-ci-action/b/2.apk", apk)
        }

        assertThat(factoryCalls).isEqualTo(1)
    }

    @Test
    fun uploadApk_disablesRemainingUploadsOnHttp403() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        var existsCalls = 0
        val service =
            testService(
                onExists = {
                    existsCalls++
                    throw StorageException(403, "Forbidden")
                }
            )

        service.use {
            it.uploadApk("github-ci-action/a/1.apk", apk)
            it.uploadApk("github-ci-action/b/2.apk", apk)
        }

        assertThat(existsCalls).isEqualTo(1)
    }

    @Test
    fun uploadApk_continuesAfterTransientError() {
        val apk = tempFolder.newFile("test.apk").apply { writeText("apk-bytes") }
        var uploadCalls = 0
        val service =
            testService(
                onExists = { false },
                onUpload = { _, _ ->
                    uploadCalls++
                    if (uploadCalls == 1) throw StorageException(503, "Service Unavailable")
                },
            )

        service.use {
            it.uploadApk("github-ci-action/a/1.apk", apk)
            it.uploadApk("github-ci-action/b/2.apk", apk)
        }

        assertThat(uploadCalls).isEqualTo(2)
    }

    private fun testService(
        storageFactory: () -> Storage = { noopStorage() },
        onExists: (BlobId) -> Boolean = { false },
        onUpload: (BlobId, File) -> Unit = { _, _ -> },
    ): ApkGcsUploadService =
        object : ApkGcsUploadService() {
            override fun getParameters(): BuildServiceParameters.None =
                throw UnsupportedOperationException()

            override fun createStorage(): Storage = storageFactory()

            override fun blobExists(storage: Storage, blobId: BlobId): Boolean = onExists(blobId)

            override fun uploadBlob(storage: Storage, blobId: BlobId, file: File) =
                onUpload(blobId, file)
        }

    private fun noopStorage(): Storage =
        Proxy.newProxyInstance(Storage::class.java.classLoader, arrayOf(Storage::class.java)) {
            _,
            method,
            _ ->
            if (method.name == "close") null else throw UnsupportedOperationException(method.name)
        } as Storage
}
