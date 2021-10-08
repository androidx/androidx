/*
 * Copyright 2021 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraAccessException
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log
import kotlin.jvm.Throws

/**
 * Thrown when an operation cannot be executed because underlying object is closed or in an
 * unusable state.
 */
internal class ObjectUnavailableException(e: Throwable) : Exception(e)

/**
 * Catch specific exceptions that are not normally thrown, log them, then rethrow.
 */
@Throws(ObjectUnavailableException::class)
internal inline fun <T> rethrowCamera2Exceptions(crossinline block: () -> T): T {
    // Camera2 has, at different points in time, thrown a large number of checked and/or
    // unchecked exceptions under different circumstances that are not listed in the
    // documentation. This method catches and recasts these exceptions into a common exception
    // type.
    //
    // Specific examples:
    // * Some exceptions (such as IllegalArgumentException) can happen if a surface is destroyed
    //   out of band during configuration.
    // * Some exceptions (such as IllegalStateException) can be thrown but are not reported as
    //   being thrown by various methods on some versions of the OS.
    // * Some exceptions (such as SecurityException) can happen even when the application has
    //   permission to access the camera but a higher priority or security sensitive service is
    //   currently using the camera.
    // * Some exceptions (such as UnsupportedOperationException) can be thrown on some versions
    //   of the OS (b/28617016)
    try {
        return block()
    } catch (e: Exception) {
        throw when (e) {
            is IllegalArgumentException,
            is IllegalStateException,
            is CameraAccessException,
            is SecurityException,
            is UnsupportedOperationException -> {
                Log.debug(e) { "Rethrowing ${e::class.java.simpleName} from Camera2" }
                ObjectUnavailableException(e)
            }
            else -> e
        }
    }
}