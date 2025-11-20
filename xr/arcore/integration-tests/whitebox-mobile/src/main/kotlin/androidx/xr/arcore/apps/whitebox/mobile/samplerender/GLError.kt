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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender

import android.opengl.GLES30
import android.opengl.GLException
import android.opengl.GLU
import android.util.Log
import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.mutableIntListOf
import java.util.Locale

/** Methods for handling OpenGL errors. */

/** Throws a [GLException] if a GL error occurred. */
public fun maybeThrowGLException(reason: String, api: String) {
    val errorCodes: IntList? = getGlErrors()
    if (errorCodes != null) {
        throw GLException(errorCodes.get(0), formatErrorMessage(reason, api, errorCodes))
    }
}

/** Logs a message with the given logcat priority if a GL error occurred. */
public fun maybeLogGLError(priority: Int, tag: String, reason: String, api: String) {
    val errorCodes: IntList? = getGlErrors()
    if (errorCodes != null) {
        Log.println(priority, tag, formatErrorMessage(reason, api, errorCodes))
    }
}

private fun formatErrorMessage(reason: String, api: String, errorCodes: IntList): String {
    val builder: StringBuilder = StringBuilder(String.format("%s: %s: ", reason, api))
    errorCodes.forEach { errorCode ->
        builder.append(
            String.format(Locale.US, "%s (%d)", GLU.gluErrorString(errorCode), errorCode)
        )
        if (errorCode != errorCodes.last()) {
            builder.append(", ")
        }
    }
    return builder.toString()
}

private fun getGlErrors(): IntList? {
    var errorCode = GLES30.glGetError()
    // Shortcut for no errors
    if (errorCode == GLES30.GL_NO_ERROR) {
        return null
    }
    val errorCodes: MutableIntList = mutableIntListOf()
    errorCodes.add(errorCode)
    while (true) {
        errorCode = GLES30.glGetError()
        if (errorCode == GLES30.GL_NO_ERROR) {
            break
        }
        errorCodes.add(errorCode)
    }
    return errorCodes
}
