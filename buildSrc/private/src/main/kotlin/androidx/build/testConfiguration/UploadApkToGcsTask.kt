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

import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import java.io.File
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Opportunistically uploads a single device test or app APK to the GCS bucket used by androidx.dev
 * as soon as the APK finishes building.
 *
 * Any authentication or GCS failure is logged as a warning and does not fail the build, since
 * `androidTest.zip` still contains all APKs as a fallback.
 */
@DisableCachingByDefault(because = "Uploads APKs to GCS")
abstract class UploadApkToGcsTask : DefaultTask() {

    @get:ServiceReference(ApkGcsUploadService.SERVICE_NAME)
    abstract val uploadService: Property<ApkGcsUploadService>

    /** File existence check to determine whether to run this task. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidTestSourceCode: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val apkFile: RegularFileProperty

    @TaskAction
    fun uploadApk() {
        val file = apkFile.get().asFile
        if (!file.exists()) return
        uploadService.get().uploadApk(gcsApkObjectPath(file.name, sha256(file)), file)
    }
}

/**
 * Shared Gradle [BuildService] that reuses a single GCS client, bounds concurrent uploads via
 * `maxParallelUsages`, and disables remaining uploads for the build on auth failures.
 */
abstract class ApkGcsUploadService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val logger = Logging.getLogger(ApkGcsUploadService::class.java)
    private val storageLock = Any()

    @Volatile private var storage: Storage? = null
    private val uploadsDisabled = AtomicBoolean(false)

    internal open fun createStorage(): Storage =
        StorageOptions.newBuilder()
            .setProjectId(GCP_PROJECT_ID)
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setRetrySettings(
                RetrySettings.newBuilder()
                    .setMaxAttempts(MAX_RETRY_ATTEMPTS)
                    .setTotalTimeoutDuration(UPLOAD_TOTAL_TIMEOUT)
                    .build()
            )
            .build()
            .service

    internal open fun blobExists(storage: Storage, blobId: BlobId): Boolean =
        storage.get(blobId, Storage.BlobGetOption.fields(Storage.BlobField.NAME)) != null

    internal open fun uploadBlob(storage: Storage, blobId: BlobId, file: File) {
        storage.createFrom(
            BlobInfo.newBuilder(blobId).build(),
            file.toPath(),
            Storage.BlobWriteOption.doesNotExist(),
        )
    }

    fun uploadApk(objectPath: String, file: File) {
        val client = getOrCreateStorage() ?: return
        val blobId = BlobId.of(GCS_BUCKET_NAME, objectPath)
        try {
            if (blobExists(client, blobId)) {
                logger.info("APK already exists in GCS: gs://{}/{}", GCS_BUCKET_NAME, objectPath)
                return
            }
            uploadBlob(client, blobId, file)
            logger.info(
                "Uploaded APK ({} bytes) to gs://{}/{}",
                file.length(),
                GCS_BUCKET_NAME,
                objectPath,
            )
        } catch (e: StorageException) {
            when (e.code) {
                HTTP_PRECONDITION_FAILED ->
                    logger.info(
                        "APK concurrently uploaded to GCS: gs://{}/{}",
                        GCS_BUCKET_NAME,
                        objectPath,
                    )
                HTTP_UNAUTHORIZED,
                HTTP_FORBIDDEN ->
                    disableUploads(
                        "Disabling opportunistic APK GCS uploads for this build due to HTTP " +
                            "${e.code} accessing gs://$GCS_BUCKET_NAME/$objectPath: ${e.message}"
                    )
                else ->
                    logger.warn(
                        "Opportunistic GCS upload failed (HTTP ${e.code}) for " +
                            "gs://$GCS_BUCKET_NAME/$objectPath: ${e.message}"
                    )
            }
        } catch (e: Exception) {
            logger.warn("Opportunistic GCS upload failed for gs://$GCS_BUCKET_NAME/$objectPath", e)
        }
    }

    private fun getOrCreateStorage(): Storage? {
        if (uploadsDisabled.get()) return null
        storage?.let {
            return it
        }
        synchronized(storageLock) {
            if (uploadsDisabled.get()) return null
            storage?.let {
                return it
            }
            return try {
                createStorage().also { storage = it }
            } catch (e: Exception) {
                disableUploads(
                    "Disabling opportunistic APK GCS uploads for this build because GCS " +
                        "credentials/client could not be initialized: ${e.message}"
                )
                null
            }
        }
    }

    private fun disableUploads(message: String) {
        if (uploadsDisabled.compareAndSet(false, true)) {
            logger.warn(message)
        }
    }

    override fun close() {
        synchronized(storageLock) {
            runCatching { storage?.close() }
            storage = null
        }
    }

    companion object {
        const val SERVICE_NAME = "apkGcsUploadService"
        internal const val GCS_BUCKET_NAME = "androidx-ftl-test-results"
        internal const val GCS_BUCKET_PATH = "github-ci-action"
        private const val GCP_PROJECT_ID = "androidx-dev-prod"
        private const val MAX_CONCURRENT_UPLOADS = 8
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_PRECONDITION_FAILED = 412
        private val UPLOAD_TOTAL_TIMEOUT = Duration.ofSeconds(60)

        fun registerOrGet(project: Project): Provider<ApkGcsUploadService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                ApkGcsUploadService::class.java,
            ) { spec ->
                spec.maxParallelUsages.set(MAX_CONCURRENT_UPLOADS)
            }
    }
}

/**
 * Computes the GCS object path matching androidx.dev's `ApkInfo.filePathWithoutExtension` +
 * `ApkStore.gcpRelativePath()` + `GoogleCloudApiImpl.makeBucketPath()`:
 * `<bucketPath>/<apkFileNameWithoutExtensionWithDotsAsSlashes>/<sha256>.apk`.
 */
internal fun gcsApkObjectPath(
    apkFileName: String,
    sha256: String,
    bucketPath: String = ApkGcsUploadService.GCS_BUCKET_PATH,
): String {
    val filePathWithoutExtension =
        if ('.' in apkFileName) {
            apkFileName.split('.').dropLast(1).joinToString("/")
        } else {
            apkFileName
        }
    return "$bucketPath/$filePathWithoutExtension/$sha256.apk"
}
