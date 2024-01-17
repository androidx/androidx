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
package androidx.privacysandbox.sdkruntime.client.loader.impl

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Refers to the context of the SDK loaded locally.
 *
 * Supports Per-SDK storage by pointing storage related APIs to folders unique for each SDK.
 * Where possible maintains same folders hierarchy as for applications by creating folders
 * inside [getDataDir].
 * Folders with special permissions or additional logic (caches, etc) created as subfolders of same
 * application folders.
 * SDK Shared Preferences supported by adding prefix to name and delegating to Application
 * Shared Preferences.
 *
 * SDK Folders hierarchy (from application [getDataDir]):
 * 1) /cache/RuntimeEnabledSdksData/<sdk_package_name> - cache
 * 2) /code_cache/RuntimeEnabledSdksData/<sdk_package_name> - code_cache
 * 3) /no_backup/RuntimeEnabledSdksData/<sdk_package_name> - no_backup
 * 4) /app_RuntimeEnabledSdksData/<sdk_package_name>/ - SDK Root (data dir)
 * 5) /app_RuntimeEnabledSdksData/<sdk_package_name>/files - [getFilesDir]
 * 6) /app_RuntimeEnabledSdksData/<sdk_package_name>/app_<folder_name> - [getDir]
 * 7) /app_RuntimeEnabledSdksData/<sdk_package_name>/databases - SDK Databases
 */
internal class SandboxedSdkContextCompat(
    base: Context,
    private val sdkPackageName: String,
    private val classLoader: ClassLoader?
) : ContextWrapper(base) {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun createDeviceProtectedStorageContext(): Context {
        return SandboxedSdkContextCompat(
            Api24.createDeviceProtectedStorageContext(baseContext),
            sdkPackageName,
            classLoader
        )
    }

    /**
     *  Points to <app_data_dir>/app_RuntimeEnabledSdksData/<sdk_package_name>
     */
    override fun getDataDir(): File {
        val sdksDataRoot = baseContext.getDir(
            SDK_ROOT_FOLDER,
            Context.MODE_PRIVATE
        )
        return ensureDirExists(sdksDataRoot, sdkPackageName)
    }

    /**
     *  Points to <app_data_dir>/cache/RuntimeEnabledSdksData/<sdk_package_name>
     */
    override fun getCacheDir(): File {
        val sdksCacheRoot = ensureDirExists(baseContext.cacheDir, SDK_ROOT_FOLDER)
        return ensureDirExists(sdksCacheRoot, sdkPackageName)
    }

    /**
     *  Points to <app_data_dir>/code_cache/RuntimeEnabledSdksData/<sdk_package_name>
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getCodeCacheDir(): File {
        val sdksCodeCacheRoot = ensureDirExists(
            Api21.codeCacheDir(baseContext),
            SDK_ROOT_FOLDER
        )
        return ensureDirExists(sdksCodeCacheRoot, sdkPackageName)
    }

    /**
     *  Points to <app_data_dir>/no_backup/RuntimeEnabledSdksData/<sdk_package_name>
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getNoBackupFilesDir(): File {
        val sdksNoBackupRoot = ensureDirExists(
            Api21.noBackupFilesDir(baseContext),
            SDK_ROOT_FOLDER
        )
        return ensureDirExists(sdksNoBackupRoot, sdkPackageName)
    }

    /**
     *  Points to <app_data_dir>/app_RuntimeEnabledSdksData/<sdk_package_name>/app_<folder_name>
     *  Prefix required to maintain same hierarchy as for applications - when dir could be
     *  accessed by both [getDir] and [getDir]/app_<folder_name>.
     */
    override fun getDir(name: String, mode: Int): File {
        val dirName = "app_$name"
        return ensureDirExists(dataDir, dirName)
    }

    /**
     *  Points to <app_data_dir>/app_RuntimeEnabledSdksData/<sdk_package_name>/files
     */
    override fun getFilesDir(): File {
        return ensureDirExists(dataDir, "files")
    }

    override fun openFileInput(name: String): FileInputStream {
        val file = makeFilename(filesDir, name)
        return FileInputStream(file)
    }

    override fun openFileOutput(name: String, mode: Int): FileOutputStream {
        val file = makeFilename(filesDir, name)
        val append = (mode and MODE_APPEND) != 0
        return FileOutputStream(file, append)
    }

    override fun deleteFile(name: String): Boolean {
        val file = makeFilename(filesDir, name)
        return file.delete()
    }

    override fun getFileStreamPath(name: String): File {
        return makeFilename(filesDir, name)
    }

    override fun fileList(): Array<String> {
        return listOrEmpty(filesDir)
    }

    override fun getDatabasePath(name: String): File {
        if (name[0] == File.separatorChar) {
            return baseContext.getDatabasePath(name)
        }
        val absolutePath = File(getDatabasesDir(), name)
        return baseContext.getDatabasePath(absolutePath.absolutePath)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?
    ): SQLiteDatabase {
        return openOrCreateDatabase(name, mode, factory, null)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase {
        return baseContext.openOrCreateDatabase(
            getDatabasePath(name).absolutePath,
            mode,
            factory,
            errorHandler
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun moveDatabaseFrom(sourceContext: Context, name: String): Boolean {
        synchronized(SandboxedSdkContextCompat::class.java) {
            val source = sourceContext.getDatabasePath(name)
            val target = getDatabasePath(name)
            return MigrationUtils.moveFiles(
                source.parentFile!!,
                target.parentFile!!,
                source.name
            )
        }
    }

    override fun deleteDatabase(name: String): Boolean {
        return baseContext.deleteDatabase(
            getDatabasePath(name).absolutePath
        )
    }

    override fun databaseList(): Array<String> {
        return listOrEmpty(getDatabasesDir())
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return baseContext.getSharedPreferences(
            getSdkSharedPreferenceName(name),
            mode
        )
    }

    /**
     * Only moving between instances of [SandboxedSdkContextCompat] supported.
     * Supporting of other contexts not possible as prefixed name will not be found by
     * internal context implementation.
     * SDK should work ONLY with SDK context.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    override fun moveSharedPreferencesFrom(sourceContext: Context, name: String): Boolean {
        if (sourceContext !is SandboxedSdkContextCompat) {
            return false
        }

        val sourceBaseContext = sourceContext.baseContext
        val sourceBaseDataDir = Api24.dataDir(sourceBaseContext)
        val sourceSharedPreferencesDir = File(sourceBaseDataDir, "shared_prefs")

        val targetBaseDataDir = Api24.dataDir(baseContext)
        val targetSharedPreferencesDir = File(targetBaseDataDir, "shared_prefs")
        if (!targetSharedPreferencesDir.exists()) {
            targetSharedPreferencesDir.mkdir()
        }

        if (sourceSharedPreferencesDir == targetSharedPreferencesDir) {
            return true
        }

        synchronized(SandboxedSdkContextCompat::class.java) {
            val sdkSharedPreferencesName = getSdkSharedPreferenceName(name)
            val moveResult = MigrationUtils.moveFiles(
                sourceSharedPreferencesDir,
                targetSharedPreferencesDir,
                "$sdkSharedPreferencesName.xml"
            )

            if (moveResult) {
                // clean cache in source context
                Api24.deleteSharedPreferences(sourceBaseContext, sdkSharedPreferencesName)
            }

            return moveResult
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun deleteSharedPreferences(name: String): Boolean {
        return Api24.deleteSharedPreferences(
            baseContext,
            getSdkSharedPreferenceName(name)
        )
    }

    override fun getClassLoader(): ClassLoader? {
        return classLoader
    }

    private fun getDatabasesDir(): File =
        ensureDirExists(dataDir, "databases")

    private fun getSdkSharedPreferenceName(originalName: String) =
        "${SDK_SHARED_PREFERENCES_PREFIX}_${sdkPackageName}_$originalName"

    private fun listOrEmpty(dir: File?): Array<String> {
        return dir?.list() ?: emptyArray()
    }

    private fun makeFilename(parent: File, name: String): File {
        if (name.indexOf(File.separatorChar) >= 0) {
            throw IllegalArgumentException(
                "File $name contains a path separator"
            )
        }
        return File(parent, name)
    }

    private fun ensureDirExists(parent: File, dirName: String): File {
        val dir = File(parent, dirName)
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private object Api21 {
        @DoNotInline
        fun codeCacheDir(context: Context): File = context.codeCacheDir

        @DoNotInline
        fun noBackupFilesDir(context: Context): File = context.noBackupFilesDir
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private object Api24 {
        @DoNotInline
        fun createDeviceProtectedStorageContext(context: Context): Context =
            context.createDeviceProtectedStorageContext()

        @DoNotInline
        fun dataDir(context: Context): File = context.dataDir

        @DoNotInline
        fun deleteSharedPreferences(context: Context, name: String): Boolean =
            context.deleteSharedPreferences(name)
    }

    private companion object {
        private const val SDK_ROOT_FOLDER = "RuntimeEnabledSdksData"
        private const val SDK_SHARED_PREFERENCES_PREFIX = "RuntimeEnabledSdk"
    }
}
