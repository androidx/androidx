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
package androidx.privacysandbox.sdkruntime.core

import android.app.sdksandbox.LoadSdkException
import android.os.Bundle
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.DoNotInline
import androidx.annotation.IntDef
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Compat alternative for [LoadSdkException].
 * Thrown from [SandboxedSdkProviderCompat.onLoadSdk].
 *
 * @see [LoadSdkException]
 */
class LoadSdkCompatException : Exception {

    /**
     * Result code this exception was constructed with.
     *
     * @see [LoadSdkException.getLoadSdkErrorCode]
     */
    @field:LoadSdkErrorCode
    @get:LoadSdkErrorCode
    val loadSdkErrorCode: Int

    /**
     * Extra error information this exception was constructed with.
     *
     * @see [LoadSdkException.getExtraInformation]
     */
    val extraInformation: Bundle

    /**
     * Initializes a LoadSdkCompatException with a result code, a message, a cause and extra
     * information.
     *
     * @param loadSdkErrorCode The result code.
     * @param message The detailed message.
     * @param cause The cause of the exception. A null value is permitted, and indicates that the
     *  cause is nonexistent or unknown.
     * @param extraInformation Extra error information. This is empty if there is no such information.
     */
    @RestrictTo(LIBRARY_GROUP)
    @JvmOverloads
    constructor(
        @LoadSdkErrorCode loadSdkErrorCode: Int,
        message: String?,
        cause: Throwable?,
        extraInformation: Bundle = Bundle()
    ) : super(message, cause) {
        this.loadSdkErrorCode = loadSdkErrorCode
        this.extraInformation = extraInformation
    }

    /**
     * Initializes a LoadSdkCompatException with a result code and a message
     *
     * @param loadSdkErrorCode The result code.
     * @param message The detailed message.
     */
    @RestrictTo(LIBRARY_GROUP)
    constructor(
        @LoadSdkErrorCode loadSdkErrorCode: Int,
        message: String?
    ) : this(loadSdkErrorCode, message, cause = null)

    /**
     * Initializes a LoadSdkCompatException with a Throwable and a Bundle.
     *
     * @param cause The cause of the exception.
     * @param extraInfo Extra error information. This is empty if there is no such information.
     */
    constructor(
        cause: Throwable,
        extraInfo: Bundle
    ) : this(LOAD_SDK_SDK_DEFINED_ERROR, "", cause, extraInfo)

    @IntDef(
        SDK_SANDBOX_PROCESS_NOT_AVAILABLE,
        LOAD_SDK_NOT_FOUND,
        LOAD_SDK_ALREADY_LOADED,
        LOAD_SDK_SDK_DEFINED_ERROR,
        LOAD_SDK_SDK_SANDBOX_DISABLED,
        LOAD_SDK_INTERNAL_ERROR,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadSdkErrorCode

    /**
     *  Create platform [LoadSdkException] from compat exception.
     *
     *  @return Platform exception.
     */
    @RequiresExtension(extension = AD_SERVICES, version = 4)
    internal fun toLoadSdkException(): LoadSdkException {
        return ApiAdServicesV4Impl.toLoadSdkException(this)
    }

    @RequiresExtension(extension = AD_SERVICES, version = 4)
    private object ApiAdServicesV4Impl {

        @DoNotInline
        fun toLoadSdkException(ex: LoadSdkCompatException): LoadSdkException {
            return LoadSdkException(
                ex.cause!!,
                ex.extraInformation
            )
        }

        @DoNotInline
        fun toLoadCompatSdkException(ex: LoadSdkException): LoadSdkCompatException {
            return LoadSdkCompatException(
                toLoadSdkErrorCodeCompat(ex.loadSdkErrorCode),
                ex.message,
                ex.cause,
                ex.extraInformation
            )
        }

        @LoadSdkErrorCode
        private fun toLoadSdkErrorCodeCompat(
            value: Int
        ): Int {
            return value // TODO(b/249982002): Validate and convert
        }
    }

    companion object {

        /**
         * Sdk sandbox process is not available.
         *
         * This indicates that the sdk sandbox process is not available, either because it has died,
         * disconnected or was not created in the first place.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.SDK_SANDBOX_PROCESS_NOT_AVAILABLE]
         */
        const val SDK_SANDBOX_PROCESS_NOT_AVAILABLE = 503

        /**
         * SDK not found.
         *
         * This indicates that client application tried to load a non-existing SDK.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_NOT_FOUND]
         */
        const val LOAD_SDK_NOT_FOUND = 100

        /**
         * SDK is already loaded.
         *
         * This indicates that client application tried to reload the same SDK after being
         * successfully loaded.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_ALREADY_LOADED]
         */
        const val LOAD_SDK_ALREADY_LOADED = 101

        /**
         * SDK error after being loaded.
         *
         * This indicates that the SDK encountered an error during post-load initialization. The
         * details of this can be obtained from the Bundle returned in [LoadSdkCompatException].
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_SDK_DEFINED_ERROR]
         */
        const val LOAD_SDK_SDK_DEFINED_ERROR = 102

        /**
         * SDK sandbox is disabled.
         *
         * This indicates that the SDK sandbox is disabled. Any subsequent attempts to load SDKs in
         * this boot will also fail.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_SDK_SANDBOX_DISABLED]
         */
        const val LOAD_SDK_SDK_SANDBOX_DISABLED = 103

        /**
         * Internal error while loading SDK.
         *
         * This indicates a generic internal error happened while applying the call from
         * client application.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_INTERNAL_ERROR]
         */
        const val LOAD_SDK_INTERNAL_ERROR = 500

        /**
         *  Create compat exception from platform [LoadSdkException].
         *
         *  @param ex Platform exception
         *  @return Compat exception.
         */
        @RequiresExtension(extension = AD_SERVICES, version = 4)
        @RestrictTo(LIBRARY_GROUP)
        fun toLoadCompatSdkException(ex: LoadSdkException): LoadSdkCompatException {
            return ApiAdServicesV4Impl.toLoadCompatSdkException(ex)
        }
    }
}
