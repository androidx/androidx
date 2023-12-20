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
package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import android.os.Build
import androidx.privacysandbox.sdkruntime.client.loader.impl.SandboxedSdkContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
internal class SandboxedSdkContextCompatTest(
    private val contextType: String,
    private val sdkContextCompat: SandboxedSdkContextCompat,
    private val appStorageContext: Context
) {

    @Test
    fun getClassloader_returnSdkClassloader() {
        val sdkClassLoader = javaClass.classLoader!!.parent!!
        assertThat(sdkContextCompat.classLoader)
            .isEqualTo(sdkClassLoader)
    }

    @Test
    fun getDataDir_returnSdkDataDirInAppDir() {
        val expectedSdksRoot = appStorageContext.getDir(SDK_ROOT_FOLDER, Context.MODE_PRIVATE)
        val expectedSdkDataDir = File(expectedSdksRoot, SDK_PACKAGE_NAME)

        assertThat(sdkContextCompat.dataDir)
            .isEqualTo(expectedSdkDataDir)

        assertThat(expectedSdkDataDir.exists()).isTrue()
    }

    @Test
    fun getCacheDir_returnSdkCacheDirInAppCacheDir() {
        val expectedSdksCacheRoot = File(appStorageContext.cacheDir, SDK_ROOT_FOLDER)
        val expectedSdkCache = File(expectedSdksCacheRoot, SDK_PACKAGE_NAME)

        assertThat(sdkContextCompat.cacheDir)
            .isEqualTo(expectedSdkCache)

        assertThat(expectedSdkCache.exists()).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    fun getCodeCacheDir_returnSdkCodeCacheDirInAppCodeCacheDir() {
        val expectedSdksCodeCacheRoot = File(appStorageContext.codeCacheDir, SDK_ROOT_FOLDER)
        val expectedSdkCodeCache = File(expectedSdksCodeCacheRoot, SDK_PACKAGE_NAME)

        assertThat(sdkContextCompat.codeCacheDir)
            .isEqualTo(expectedSdkCodeCache)

        assertThat(expectedSdkCodeCache.exists()).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    fun getNoBackupFilesDir_returnSdkNoBackupDirInAppNoBackupDir() {
        val expectedSdksNoBackupRoot = File(appStorageContext.noBackupFilesDir, SDK_ROOT_FOLDER)
        val expectedSdkNoBackupDir = File(expectedSdksNoBackupRoot, SDK_PACKAGE_NAME)

        assertThat(sdkContextCompat.noBackupFilesDir)
            .isEqualTo(expectedSdkNoBackupDir)

        assertThat(expectedSdkNoBackupDir.exists()).isTrue()
    }

    @Test
    fun getDir_returnDirWithPrefixInSdkDataDir() {
        val expectedDir = File(sdkContextCompat.dataDir, "app_test")

        assertThat(sdkContextCompat.getDir("test", Context.MODE_PRIVATE))
            .isEqualTo(expectedDir)

        assertThat(expectedDir.exists()).isTrue()
    }

    @Test
    fun getFilesDir_returnFilesDirInSdkDataDir() {
        val expectedFilesDir = File(sdkContextCompat.dataDir, "files")

        assertThat(sdkContextCompat.filesDir)
            .isEqualTo(expectedFilesDir)

        assertThat(expectedFilesDir.exists()).isTrue()
    }

    @Test
    fun openFileInput_openFileInSdkFilesDir() {
        val fileToOpen = File(sdkContextCompat.filesDir, "testOpenFileInput")
        fileToOpen.outputStream().use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                dataStream.writeInt(42)
            }
        }

        val content = sdkContextCompat.openFileInput("testOpenFileInput")
            .use { inputStream ->
                DataInputStream(inputStream).use { dataStream ->
                    dataStream.readInt()
                }
            }

        assertThat(content)
            .isEqualTo(42)
    }

    @Test
    fun openFileInput_whenFileNameContainsFileSeparator_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            sdkContextCompat.openFileInput("folder/file")
        }
    }

    @Test
    fun openFileOutput_openFileInSdkFilesDir() {
        sdkContextCompat.openFileOutput("testOpenFileOutput", Context.MODE_PRIVATE)
            .use { outputStream ->
                DataOutputStream(outputStream).use { dataStream ->
                    dataStream.writeInt(42)
                }
            }

        val expectedFile = File(sdkContextCompat.filesDir, "testOpenFileOutput")
        val content = expectedFile.inputStream().use { inputStream ->
            DataInputStream(inputStream).use { dataStream ->
                dataStream.readInt()
            }
        }

        assertThat(content)
            .isEqualTo(42)
    }

    @Test
    fun openFileOutput_whenAppendFlagSet_appendToFileInSdkFilesDir() {
        sdkContextCompat.openFileOutput(
            "testOpenFileOutputAppend",
            Context.MODE_PRIVATE or Context.MODE_APPEND
        ).use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                dataStream.writeInt(42)
            }
        }
        sdkContextCompat.openFileOutput(
            "testOpenFileOutputAppend",
            Context.MODE_PRIVATE or Context.MODE_APPEND
        ).use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                dataStream.writeInt(1)
            }
        }

        val expectedFile = File(sdkContextCompat.filesDir, "testOpenFileOutputAppend")
        val content = expectedFile.inputStream().use { inputStream ->
            DataInputStream(inputStream).use { dataStream ->
                dataStream.readInt() + dataStream.readInt()
            }
        }

        assertThat(content)
            .isEqualTo(43)
    }

    @Test
    fun openFileOutput_whenFileNameContainsFileSeparator_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            sdkContextCompat.openFileOutput("folder/file", Context.MODE_PRIVATE)
        }
    }

    @Test
    fun deleteFile_deleteFileInSdkFilesDir() {
        val fileToDelete = File(sdkContextCompat.filesDir, "testDelete")
        fileToDelete.createNewFile()
        assertThat(fileToDelete.exists()).isTrue()

        assertThat(sdkContextCompat.deleteFile("testDelete")).isTrue()
        assertThat(fileToDelete.exists()).isFalse()
    }

    @Test
    fun deleteFile_whenFileNameContainsFileSeparator_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            sdkContextCompat.deleteFile("folder/file")
        }
    }

    @Test
    fun getFileStreamPath_returnFileFromSdkFilesDir() {
        val expectedFile = File(sdkContextCompat.filesDir, "testGetFileStreamPath")

        assertThat(sdkContextCompat.getFileStreamPath("testGetFileStreamPath"))
            .isEqualTo(expectedFile)
    }

    @Test
    fun getFileStreamPath_whenFileNameContainsFileSeparator_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            sdkContextCompat.getFileStreamPath("folder/file")
        }
    }

    @Test
    fun fileList_returnContentOfSdkFilesDir() {
        val expectedFile = File(sdkContextCompat.filesDir, "testFileList")
        expectedFile.createNewFile()

        val result = sdkContextCompat.fileList().asList()
        assertThat(result).contains("testFileList")
        assertThat(result).isEqualTo(sdkContextCompat.filesDir.list()!!.asList())
    }

    @Test
    fun getDatabasePath_whenDataBaseNamePassed_returnPathToDatabaseInSdkDatabasesDir() {
        val expectedDatabasePath = File(
            sdkContextCompat.dataDir,
            "databases/testGetDatabasePath"
        )

        assertThat(sdkContextCompat.getDatabasePath("testGetDatabasePath"))
            .isEqualTo(expectedDatabasePath)
    }

    @Test
    fun getDatabasePath_whenDataBasePathPassed_returnSamePath() {
        val expectedDatabasePath = File(
            sdkContextCompat.dataDir,
            "databases/testGetDatabasePathAbsolute"
        )

        assertThat(sdkContextCompat.getDatabasePath(expectedDatabasePath.absolutePath))
            .isEqualTo(expectedDatabasePath)
    }

    @Test
    fun openOrCreateDatabase_returnDatabaseFromSdkDatabasesDir() {
        val databaseName = "testOpenDataBase.db"

        sdkContextCompat.deleteDatabase(databaseName)
        val database = sdkContextCompat.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null
        )

        database.execSQL("CREATE TABLE test (data int)")
        database.execSQL("INSERT INTO test (data) values (42)")

        val databaseFrom4ParamMethod = sdkContextCompat.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null,
            errorHandler = null
        )

        val result = databaseFrom4ParamMethod.rawQuery("SELECT * FROM test", null)
        result.moveToFirst()
        assertThat(result.getInt(0))
            .isEqualTo(42)

        val databasePath = sdkContextCompat.getDatabasePath(databaseName)
        assertThat(databasePath.exists()).isTrue()
    }

    @Test
    fun deleteDatabase_deleteDatabaseFromSdkDatabasesDir() {
        val databaseName = "testDeleteDatabase.db"
        sdkContextCompat.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null
        )
        assertThat(sdkContextCompat.getDatabasePath(databaseName).exists()).isTrue()

        sdkContextCompat.deleteDatabase(databaseName)

        assertThat(sdkContextCompat.getDatabasePath(databaseName).exists()).isFalse()
    }

    @Test
    fun databaseList_returnContentOfSdkDatabasesDir() {
        val databaseName = "testDatabaseList.db"
        sdkContextCompat.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null
        )

        val result = sdkContextCompat.databaseList().asList()
        assertThat(result).contains(databaseName)
        assertThat(result).isEqualTo(
            File(sdkContextCompat.dataDir, "databases").list()!!.asList()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun moveDatabaseFrom_migrateDatabaseToSdkDatabasesDir() {
        val sourceAppStorageContext = if (sdkContextCompat.isDeviceProtectedStorage) {
            ApplicationProvider.getApplicationContext()
        } else {
            appStorageContext.createDeviceProtectedStorageContext()
        }
        val sourceContext = SandboxedSdkContextCompat(
            sourceAppStorageContext,
            sdkPackageName = SDK_PACKAGE_NAME,
            classLoader = javaClass.classLoader!!.parent!!
        )

        val databaseName = "testMoveTo$contextType.db"

        sourceContext.deleteDatabase(databaseName)
        val database = sourceContext.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null
        )

        database.execSQL("CREATE TABLE test (data int)")
        database.execSQL("INSERT INTO test (data) values (42)")

        val moveResult = sdkContextCompat.moveDatabaseFrom(sourceContext, databaseName)
        assertThat(moveResult).isTrue()

        val migratedDatabase = sdkContextCompat.openOrCreateDatabase(
            name = databaseName,
            mode = Context.MODE_PRIVATE,
            factory = null
        )

        val result = migratedDatabase.rawQuery("SELECT * FROM test", null)
        result.moveToFirst()
        assertThat(result.getInt(0))
            .isEqualTo(42)

        val oldDatabasePath = sourceContext.getDatabasePath(databaseName)
        assertThat(oldDatabasePath.exists()).isFalse()
    }

    @Test
    fun getSharedPreferences_returnPrefixedSharedPreferencesFromApp() {
        val sdkSharedPreferencesName = "getSharedPreferencesTest"
        val sdkSharedPreferences = sdkContextCompat.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )

        sdkSharedPreferences.edit().putInt("test", 42).commit()

        val appSharedPreferences = appStorageContext.getSharedPreferences(
            "${SDK_SHARED_PREFERENCES_PREFIX}_${SDK_PACKAGE_NAME}_$sdkSharedPreferencesName",
            Context.MODE_PRIVATE
        )
        val result = appSharedPreferences.getInt("test", 0)
        assertThat(result).isEqualTo(42)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun deleteSharedPreferences() {
        val sdkSharedPreferencesName = "deleteSharedPreferencesTest"
        val sdkSharedPreferences = sdkContextCompat.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )
        sdkSharedPreferences.edit().putInt("test", 42).commit()

        sdkContextCompat.deleteSharedPreferences(sdkSharedPreferencesName)

        val sdkSharedPreferencesAfterDelete = sdkContextCompat.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )
        assertThat(sdkSharedPreferencesAfterDelete.contains("test")).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun moveSharedPreferencesFrom_migrateSharedPreferencesFromAnotherSdkContext() {
        val sourceAppStorageContext = if (sdkContextCompat.isDeviceProtectedStorage) {
            ApplicationProvider.getApplicationContext()
        } else {
            appStorageContext.createDeviceProtectedStorageContext()
        }
        val sourceContext = SandboxedSdkContextCompat(
            sourceAppStorageContext,
            sdkPackageName = SDK_PACKAGE_NAME,
            classLoader = javaClass.classLoader!!.parent!!
        )

        val sdkSharedPreferencesName = "testMoveTo$contextType"

        sourceContext.deleteSharedPreferences(sdkSharedPreferencesName)
        val sourceSharedPreferences = sourceContext.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )
        sourceSharedPreferences.edit().putInt("test", 42).commit()

        val moveResult = sdkContextCompat.moveSharedPreferencesFrom(
            sourceContext,
            sdkSharedPreferencesName
        )
        assertThat(moveResult).isTrue()

        val migratedSharedPreferences = sdkContextCompat.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )

        val result = migratedSharedPreferences.getInt("test", 0)
        assertThat(result)
            .isEqualTo(42)

        val oldSharedPreferences = sourceContext.getSharedPreferences(
            sdkSharedPreferencesName,
            Context.MODE_PRIVATE
        )
        assertThat(oldSharedPreferences.contains("test")).isFalse()
    }

    companion object {
        private const val SDK_ROOT_FOLDER = "RuntimeEnabledSdksData"
        private const val SDK_SHARED_PREFERENCES_PREFIX = "RuntimeEnabledSdk"
        private const val SDK_PACKAGE_NAME = "androidx.privacysandbox.sdkruntime.storageContextTest"

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun params(): List<Array<Any>> = buildList {
            val appContext = ApplicationProvider.getApplicationContext<Context>()

            val sdkContext = SandboxedSdkContextCompat(
                appContext,
                sdkPackageName = SDK_PACKAGE_NAME,
                classLoader = javaClass.classLoader!!.parent!!
            )
            add(
                arrayOf(
                    "SimpleContext",
                    sdkContext,
                    appContext
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val deviceProtectedSdkContext = sdkContext.createDeviceProtectedStorageContext()
                add(
                    arrayOf(
                        "DeviceProtectedStorageContext",
                        deviceProtectedSdkContext,
                        appContext.createDeviceProtectedStorageContext()
                    )
                )
            }
        }
    }
}
