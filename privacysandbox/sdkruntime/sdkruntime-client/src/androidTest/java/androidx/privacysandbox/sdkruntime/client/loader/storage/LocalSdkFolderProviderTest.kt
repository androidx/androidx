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
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalSdkFolderProviderTest {

    private lateinit var context: Context

    private lateinit var sdkRootFolder: File
    private lateinit var versionFile: File

    private var appLastUpdateTime = 0L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val codeCache = File(context.applicationInfo.dataDir, "code_cache")

        sdkRootFolder = File(codeCache, "RuntimeEnabledSdk")
        versionFile = File(sdkRootFolder, "Folder.version")

        @Suppress("DEPRECATION")
        appLastUpdateTime =
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

        sdkRootFolder.deleteRecursively()
    }

    @Test
    fun create_whenNoSdkRootFolder_createSdkRootFolderAndVersionFile() {
        LocalSdkFolderProvider.create(context)

        assertThat(sdkRootFolder.exists()).isTrue()
        val version = readVersionFromFile()
        assertThat(version).isEqualTo(appLastUpdateTime)
    }

    @Test
    fun create_whenVersionNotChanged_doNotRemoveContents() {
        // Initial create
        LocalSdkFolderProvider.create(context)
        val fileToKeep = File(sdkRootFolder, "file")
        fileToKeep.createNewFile()

        LocalSdkFolderProvider.create(context)

        assertThat(fileToKeep.exists()).isTrue()
    }

    @Test
    fun create_whenVersionChanged_deleteSdkRootFolderContentAndCreateVersionFile() {
        val fileToDelete = createFileToDeleteInSdkRootFolder()
        createVersionFile {
            it.writeLong(42)
        }

        LocalSdkFolderProvider.create(context)

        assertThat(fileToDelete.exists()).isFalse()
        val version = readVersionFromFile()
        assertThat(version).isEqualTo(appLastUpdateTime)
    }

    @Test
    fun create_whenNoVersionFile_deleteSdkRootFolderContentAndCreateVersionFile() {
        val fileToDelete = createFileToDeleteInSdkRootFolder()

        LocalSdkFolderProvider.create(context)

        assertThat(sdkRootFolder.exists()).isTrue()
        assertThat(fileToDelete.exists()).isFalse()

        val version = readVersionFromFile()
        assertThat(version).isEqualTo(appLastUpdateTime)
    }

    @Test
    fun create_whenInvalidVersionFile_deleteSdkRootFolderContentAndCreateVersionFile() {
        val fileToDelete = createFileToDeleteInSdkRootFolder()
        createVersionFile {
            // Version is long type, byte is not enough
            it.writeByte(1)
        }

        LocalSdkFolderProvider.create(context)

        assertThat(fileToDelete.exists()).isFalse()
        val version = readVersionFromFile()
        assertThat(version).isEqualTo(appLastUpdateTime)
    }

    @Test
    fun dexFolderFor_returnPathToSdkDexFolder() {
        val sdkFolderProvider = LocalSdkFolderProvider.create(context)
        val dexFolder = sdkFolderProvider.dexFolderFor(
            LocalSdkConfig(
                packageName = "com.test.sdk.package",
                dexPaths = listOf("1.dex", "2.dex"),
                entryPoint = "compat.sdk.provider",
            )
        )
        assertThat(dexFolder.exists()).isTrue()
        assertThat(dexFolder).isEqualTo(
            File(sdkRootFolder, "com.test.sdk.package")
        )
    }

    @Test
    fun dexFolderFor_doNotRemoveExistingFiles() {
        val sdkFolderProvider = LocalSdkFolderProvider.create(context)

        val sdkConfig = LocalSdkConfig(
            packageName = "com.test.sdk.package",
            dexPaths = listOf("1.dex", "2.dex"),
            entryPoint = "compat.sdk.provider",
        )

        val dexFolder = sdkFolderProvider.dexFolderFor(sdkConfig)

        val fileToKeep = File(dexFolder, "testFile")
        fileToKeep.createNewFile()

        val dexFolder2 = sdkFolderProvider.dexFolderFor(sdkConfig)
        assertThat(dexFolder).isEqualTo(dexFolder2)

        assertThat(fileToKeep.exists()).isTrue()
    }

    private fun createFileToDeleteInSdkRootFolder(): File {
        val folder = File(sdkRootFolder, "folder")
        folder.mkdirs()
        val file = File(folder, "file")
        file.createNewFile()
        file.setReadOnly()
        assertThat(file.exists()).isTrue()
        return file
    }

    private fun readVersionFromFile(): Long {
        return versionFile.inputStream().use { inputStream ->
            DataInputStream(inputStream).use { dataStream ->
                dataStream.readLong()
            }
        }
    }

    private fun createVersionFile(versionWriter: (DataOutputStream) -> Unit) {
        if (!sdkRootFolder.exists()) {
            sdkRootFolder.mkdirs()
        }
        versionFile.createNewFile()
        versionFile.outputStream().use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                versionWriter(dataStream)
            }
        }
    }
}
