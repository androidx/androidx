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

package androidx.camera.effects.playservices.lowlight

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * An exception reported via [LowLightBoostEffect.error] when [LowLightBoostEffect] cannot apply Low
 * Light Boost.
 *
 * @property errorCode The error code indicating the category of failure. One of [ERROR_UNKNOWN],
 *   [ERROR_NOT_SUPPORTED], [ERROR_UNSUPPORTED_STREAM], [ERROR_MODULE_INSTALL_FAILED], or
 *   [ERROR_SESSION_FAILED]. New error codes may be added in future releases; unrecognized codes
 *   should be treated like [ERROR_UNKNOWN].
 * @see LowLightBoostEffect.error
 * @see LowLightBoostEffect.Listener.onErrorChanged
 */
public class LowLightBoostException
/**
 * Creates a [LowLightBoostException] with an error code, message, and cause.
 *
 * @param errorCode The error code describing the failure.
 * @param message A detail message describing the failure, or `null`.
 * @param cause The underlying cause of the failure, or `null`.
 */
public constructor(
    @param:ErrorCode @get:ErrorCode public val errorCode: Int,
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {

    /**
     * Creates a [LowLightBoostException] with an error code and message.
     *
     * @param errorCode The error code describing the failure.
     * @param message A detail message describing the failure, or `null`.
     */
    public constructor(
        @ErrorCode errorCode: Int,
        message: String?,
    ) : this(errorCode, message, null)

    // Intentionally inherit reference-identity equals()/hashCode() from Throwable so consecutive
    // failure instances with the same errorCode and message still count as distinct transitions.

    /** Valid error codes for [LowLightBoostException.errorCode]. */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Target(
        AnnotationTarget.TYPE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.FIELD,
    )
    @IntDef(
        ERROR_UNKNOWN,
        ERROR_NOT_SUPPORTED,
        ERROR_UNSUPPORTED_STREAM,
        ERROR_MODULE_INSTALL_FAILED,
        ERROR_SESSION_FAILED,
    )
    public annotation class ErrorCode

    public companion object {
        /**
         * An unclassified or unexpected error occurred (for example, the OpenGL pipeline or Google
         * Play Services client could not be set up).
         *
         * Internal setup failures persist for the current camera stream and only rebinding retries.
         * Unrecognized error codes added in future releases should be treated the same way as this
         * code.
         */
        public const val ERROR_UNKNOWN: Int = 0

        /**
         * **Permanent:** Low Light Boost is not supported on this Android version (below API level
         * 30), device, or camera.
         *
         * Apps can check [LowLightBoostEffect.isSupported] or
         * [LowLightBoostEffect.isSupportedAsync] beforehand to hide or disable Low Light Boost
         * controls on unsupported devices and cameras.
         */
        public const val ERROR_NOT_SUPPORTED: Int = 1

        /**
         * **Permanent for the current stream configuration:** The bound stream resolution (`> 2560`
         * pixels on a side or `> 4,194,304` total pixels) or dataspace is unsupported.
         */
        public const val ERROR_UNSUPPORTED_STREAM: Int = 2

        /**
         * The required Google Play Services module download or installation failed or was cancelled
         * (see [LowLightBoostEffect.STATUS_PENDING_INSTALLATION]).
         *
         * The effect starts a fresh attempt when a paused stream resumes or rebinds, and retries if
         * [LowLightBoostEffect.isLowLightBoostEnabled] is toggled `false` then `true`.
         */
        public const val ERROR_MODULE_INSTALL_FAILED: Int = 3

        /**
         * Creating or maintaining the Google Play Services Low Light Boost session failed (for
         * example, transient service failures or session-limit conflicts exhausted retries, the
         * active session disconnected or stopped producing frames, or Play Services rejected the
         * session).
         *
         * The effect retries transient failures automatically, starts a fresh attempt when a paused
         * stream resumes or rebinds, and retries if [LowLightBoostEffect.isLowLightBoostEnabled] is
         * toggled `false` then `true`. If Play Services rejects the session as non-retryable, the
         * error persists for the current stream and only rebinding retries.
         */
        public const val ERROR_SESSION_FAILED: Int = 4
    }
}
