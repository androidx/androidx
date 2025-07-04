/*
 * Copyright 2025 The Android Open Source Project
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
@file:JvmName("DirectBootExceptionUtilKt") // Workaround for b/313964643

package androidx.datastore.core

import android.os.Build
import android.os.Parcel
import android.os.Process
import android.util.Log
import androidx.annotation.RestrictTo
import java.io.File
import java.lang.reflect.Method

/**
 * Wraps the provided [java.io.FileNotFoundException] exception with a [DirectBootUsageException] if
 * the [DataStore] in the provided path is in Credential Encrypted Storage and the device is locked
 * (in direct boot mode).
 *
 * @param parentDirPath Path of the parent directory containing the [DataStore] file.
 * @param exception the [java.io.FileNotFoundException] encountered
 * @return An instance of a [DirectBootUsageException] or the provided [exception].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual fun wrapExceptionIfDueToDirectBoot(parentDirPath: String?, exception: Exception): Exception {
    if (exception.isDeviceUnlocked()) {
        return exception
    } else {
        // If the device is locked, we need to also verify that the file path is in CE storage
        // before we can throw a DirectBootUsageException. We do this by attempting to write a
        // sibling file in the same file path, and if that fails, we can conclude that the path is
        // in CE storage.
        if (parentDirPath == null) {
            // We don't have enough info, we return the exception.
            return exception
        }
        val siblingTestFile = File(parentDirPath, "siblingTestFile.txt")
        if (siblingTestFile.exists()) {
            // This is not expected, but delete the test file if for some reason it exists.
            siblingTestFile.delete()
        }
        try {
            // Attempt to write a sibling file.
            siblingTestFile.createNewFile()
        } catch (e: IOException) {
            // IOException indicates the path is in CE storage as it is unavailable when device is
            // locked. We wrap the exception with a DirectBootUsageException.
            return DirectBootUsageException(exception)
        } finally {
            // Sibling file successfully written, indicating the file path is in Device Encrypted
            // storage as it is available when device is locked. We clean up by removing the test
            // file.
            siblingTestFile.delete()
        }
        return exception
    }
}

/**
 * Uses system properties to check if Credential Encrypted (CE) storage is available. If CE storage
 * is available, this indicates that the device is unlocked.
 *
 * It is important to note that the SystemProperties value for sys.user.(USER_ID).ce_available is
 * only set to true once the device is unlocked. The value for this property only changes once.
 *
 * @return true if the device is unlocked.
 */
internal fun Throwable.isDeviceUnlocked(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        // Direct boot was not available before version 24, so we can assume the device is unlocked.
        return true
    } else {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method =
                systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            val userId = primaryUserId()
            val isCeAvailable =
                getMethod.invoke(null, "sys.user.$userId.ce_available", "false") as String == "true"
            return isCeAvailable
        } catch (t: Throwable) {
            // System properties is unavailable for some reason. Assume device is unlocked.
            this.addSuppressed(t)
            return false
        }
    }
}

private const val TAG: String = "DirectBootExceptionUtil"

private fun primaryUserId(): Int {
    // UserHandle does not have public api but it's parcelable so we can read through that.
    try {
        val parcel = Parcel.obtain()
        Process.myUserHandle().writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        return parcel.readInt()
    } catch (t: Throwable) {
        Log.d(TAG, "Error when reading current user id. Selected default user id `0`.")
        return 0
    }
}

internal class DirectBootUsageException(ex: Exception) : IOException(ex) {
    override val message =
        "Encountered a [${ex.message}]. If you are trying to use DataStore " +
            "during direct boot, this exception likely indicates that your DataStore file is " +
            "not located in the Device Encrypted Storage and therefore is not available for " +
            "write access during direct boot mode. DataStore to be used during direct boot must" +
            " be initialized using `DataStoreFactory.createInDeviceProtectedStorage()`."
}
