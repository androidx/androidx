/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.loader.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CachedLocalSdkStorageTest {

    private lateinit var context: Context

    private lateinit var storageUnderTest: CachedLocalSdkStorage

    private lateinit var testSdkConfig: LocalSdkConfig

    private lateinit var sdkFolder: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        storageUnderTest = CachedLocalSdkStorage.create(
            context,
            lowSpaceThreshold = disabledLowSpaceModeThreshold()
        )

        testSdkConfig = TestSdkConfigs.CURRENT_WITH_RESOURCES
        assertThat(testSdkConfig.dexPaths.size).isEqualTo(2)

        sdkFolder = LocalSdkFolderProvider
            .create(context)
            .dexFolderFor(testSdkConfig)

        // Clean up between tests
        sdkFolder.deleteRecursively()
        sdkFolder.deleteOnExit()
    }

    @Test
    fun dexFilesFor_extractSdkDexFilesAndMakeThemReadOnly() {
        val dexFiles = storageUnderTest.dexFilesFor(testSdkConfig)
        assertThat(dexFiles).isNotNull()
        val dexList = dexFiles!!.files

        assertThat(dexList.size).isEqualTo(testSdkConfig.dexPaths.size)
        for (index in testSdkConfig.dexPaths.indices) {
            assertAssetExtractedToReadOnlyFile(testSdkConfig.dexPaths[index], dexList[index])
        }
    }

    @Test
    fun dexFilesFor_whenAlreadyExtracted_returnExistingFilesWithoutModification() {
        val firstResult = storageUnderTest.dexFilesFor(testSdkConfig)!!.files
        val lastModifiedBefore = firstResult[0].lastModified()

        // Wait some time to check that no new files will be created (same lastModified)
        Thread.sleep(100)

        val secondResult = storageUnderTest.dexFilesFor(testSdkConfig)!!.files
        val lastModifiedAfter = secondResult[0].lastModified()

        assertThat(secondResult).isEqualTo(firstResult)
        assertThat(lastModifiedAfter).isEqualTo(lastModifiedBefore)
    }

    @Test
    fun dexFilesFor_whenFileMissing_extractOnlyThisFile() {
        val firstResult = storageUnderTest.dexFilesFor(testSdkConfig)!!.files
        val fileToDelete = firstResult[0]
        val fileToKeep = firstResult[1]

        fileToDelete.delete()
        val fileToKeepLastModified = fileToKeep.lastModified()

        // Wait some time to check that existing file will not be modified (same lastModified)
        Thread.sleep(100)

        val secondResult = storageUnderTest.dexFilesFor(testSdkConfig)!!.files
        val extractedFile = secondResult[0]
        val notModifiedFile = secondResult[1]

        assertThat(secondResult).isEqualTo(firstResult)
        assertAssetExtractedToReadOnlyFile(testSdkConfig.dexPaths[0], extractedFile)
        assertThat(notModifiedFile.lastModified()).isEqualTo(fileToKeepLastModified)
    }

    @Test
    fun dexFilesFor_whenLowSpaceAndNoExtractedFiles_returnNull() {
        val storageWithLowSpaceEnabled = CachedLocalSdkStorage.create(
            context,
            lowSpaceThreshold = enabledLowSpaceModeThreshold()
        )
        val result = storageWithLowSpaceEnabled.dexFilesFor(testSdkConfig)
        assertThat(result).isNull()
    }

    @Test
    fun dexFilesFor_whenLowSpaceAndFilesExtractedBefore_returnExistingFiles() {
        val extractedFiles = storageUnderTest.dexFilesFor(testSdkConfig)
        assertThat(extractedFiles).isNotNull()

        val storageWithLowSpaceEnabled = CachedLocalSdkStorage.create(
            context,
            lowSpaceThreshold = enabledLowSpaceModeThreshold()
        )
        val result = storageWithLowSpaceEnabled.dexFilesFor(testSdkConfig)
        assertThat(result).isEqualTo(extractedFiles)
    }

    @Test
    fun dexFilesFor_whenFailedToExtract_deleteFolderAndThrowException() {
        val invalidConfig = LocalSdkConfig(
            packageName = "storage.test.invalid.dexPath",
            dexPaths = listOf("NOT_EXISTS"),
            entryPoint = "EntryPoint"
        )

        val rootFolder = LocalSdkFolderProvider
            .create(context)
            .dexFolderFor(invalidConfig)

        val fileToDelete = File(rootFolder, "toDelete")
        fileToDelete.createNewFile()
        assertThat(fileToDelete.exists()).isTrue()

        assertThrows<FileNotFoundException> {
            storageUnderTest.dexFilesFor(invalidConfig)
        }.hasMessageThat().contains("NOT_EXISTS")

        assertThat(fileToDelete.exists()).isFalse()
    }

    private fun assertAssetExtractedToReadOnlyFile(assetPath: String, outputFile: File) {
        assertThat(outputFile.exists()).isTrue()
        assertThat(outputFile.parentFile).isEqualTo(sdkFolder)
        assertThat(outputFile.canWrite()).isFalse()

        val assetContent = context.assets.open(assetPath).use(InputStream::readBytes)
        val fileContent = outputFile.inputStream().use(InputStream::readBytes)
        assertThat(fileContent).isEqualTo(assetContent)
    }

    private fun enabledLowSpaceModeThreshold(): Long =
        availableBytes() + 10_000_000

    private fun disabledLowSpaceModeThreshold(): Long =
        availableBytes() - 10_000_000

    @Suppress("DEPRECATION")
    private fun availableBytes(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSize.toLong()
        val availableBlocks = stat.availableBlocks.toLong()
        return availableBlocks * blockSize
    }
}
