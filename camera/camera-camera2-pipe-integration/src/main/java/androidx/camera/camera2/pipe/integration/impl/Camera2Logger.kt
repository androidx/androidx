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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import android.util.Log
import androidx.camera.camera2.pipe.core.Log as PipeLog
import androidx.camera.camera2.pipe.integration.impl.Camera2Logger.TAG
import androidx.camera.core.Logger

/**
 * An integration-layer logger that bridges CameraX Core's dynamic log level with CameraPipe's
 * logging conventions.
 *
 * This logger respects the dynamic log level set via [androidx.camera.core.Logger.setMinLogLevel]
 * while logging with the standard CameraPipe "CXCP" tag and providing lazy (inline) evaluation for
 * log messages.
 *
 * To use this, replace imports from `androidx.camera.camera2.pipe.core.Log` with this object, e.g.,
 * `import androidx.camera.camera2.pipe.integration.impl.Camera2Logger.debug`.
 */
internal object Camera2Logger {

    /** The log tag used by CameraPipe, imported from [PipeLog.TAG]. */
    private const val TAG = PipeLog.TAG

    /**
     * On API levels strictly below 24 (N_MR1), the log tag's length must not exceed 23 characters.
     */
    private const val MAX_TAG_LENGTH = 23

    /** The potentially truncated tag, pre-calculated for efficiency. */
    private val TRUNCATED_TAG: String = truncateTag(TAG)

    /**
     * Truncates the tag so it can be used to log.
     *
     * <p>
     * Logic copied from [androidx.camera.core.Logger] to ensure compatibility. On API 26, the tag
     * length limit of 23 characters was removed.
     */
    private fun truncateTag(tag: String): String {
        return if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 && MAX_TAG_LENGTH < tag.length
        ) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else {
            tag
        }
    }

    /** Logs a [Log.VERBOSE] message, checking the level via [Camera2Logger] against the [TAG]. */
    inline fun verbose(crossinline msg: () -> String) {
        if (Logger.isVerboseEnabled(TAG)) {
            Log.v(TRUNCATED_TAG, msg())
        }
    }

    /**
     * Logs a [Log.VERBOSE] message and throwable, checking the level via [Camera2Logger] against
     * the [TAG].
     */
    inline fun verbose(throwable: Throwable, crossinline msg: () -> String) {
        if (Logger.isVerboseEnabled(TAG)) {
            Log.v(TRUNCATED_TAG, msg(), throwable)
        }
    }

    /** Logs a [Log.DEBUG] message, checking the level via [Camera2Logger] against the [TAG]. */
    inline fun debug(crossinline msg: () -> String) {
        if (Logger.isDebugEnabled(TAG)) {
            Log.d(TRUNCATED_TAG, msg())
        }
    }

    /**
     * Logs a [Log.DEBUG] message and throwable, checking the level via [Camera2Logger] against the
     * [TAG].
     */
    inline fun debug(throwable: Throwable, crossinline msg: () -> String) {
        if (Logger.isDebugEnabled(TAG)) {
            Log.d(TRUNCATED_TAG, msg(), throwable)
        }
    }

    /** Logs a [Log.INFO] message, checking the level via [Camera2Logger] against the [TAG]. */
    inline fun info(crossinline msg: () -> String) {
        if (Logger.isInfoEnabled(TAG)) {
            Log.i(TRUNCATED_TAG, msg())
        }
    }

    /**
     * Logs a [Log.INFO] message and throwable, checking the level via [Camera2Logger] against the
     * [TAG].
     */
    inline fun info(throwable: Throwable, crossinline msg: () -> String) {
        if (Logger.isInfoEnabled(TAG)) {
            Log.i(TRUNCATED_TAG, msg(), throwable)
        }
    }

    /** Logs a [Log.WARN] message, checking the level via [Camera2Logger] against the [TAG]. */
    inline fun warn(crossinline msg: () -> String) {
        if (Logger.isWarnEnabled(TAG)) {
            Log.w(TRUNCATED_TAG, msg())
        }
    }

    /**
     * Logs a [Log.WARN] message and throwable, checking the level via [Camera2Logger] against the
     * [TAG].
     */
    inline fun warn(throwable: Throwable, crossinline msg: () -> String) {
        if (Logger.isWarnEnabled(TAG)) {
            Log.w(TRUNCATED_TAG, msg(), throwable)
        }
    }

    /** Logs a [Log.ERROR] message, checking the level via [Camera2Logger] against the [TAG]. */
    inline fun error(crossinline msg: () -> String) {
        if (Logger.isErrorEnabled(TAG)) {
            Log.e(TRUNCATED_TAG, msg())
        }
    }

    /**
     * Logs a [Log.ERROR] message and throwable, checking the level via [Camera2Logger] against the
     * [TAG].
     */
    inline fun error(throwable: Throwable, crossinline msg: () -> String) {
        if (Logger.isErrorEnabled(TAG)) {
            Log.e(TRUNCATED_TAG, msg(), throwable)
        }
    }
}
