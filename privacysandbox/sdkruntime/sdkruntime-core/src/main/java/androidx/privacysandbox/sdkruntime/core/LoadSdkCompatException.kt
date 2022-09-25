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
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Compat alternative for [LoadSdkException].
 * Thrown from [SandboxedSdkProviderCompat.onLoadSdk].
 *
 * @see [LoadSdkException]
 */
class LoadSdkCompatException @JvmOverloads internal constructor(
    @field:LoadSdkErrorCode @get:LoadSdkErrorCode
    @param:LoadSdkErrorCode val loadSdkErrorCode: Int,
    message: String?,
    cause: Throwable?,
    val extraInformation: Bundle = Bundle()
) : Exception(message, cause) {

    constructor(
        cause: Throwable,
        extraInfo: Bundle
    ) : this(LOAD_SDK_SDK_DEFINED_ERROR, "", cause, extraInfo)

    @RestrictTo(LIBRARY_GROUP)
    constructor(
        @LoadSdkErrorCode loadSdkErrorCode: Int,
        message: String?
    ) : this(loadSdkErrorCode, message, /*cause=*/null)

    /** @hide */
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
    // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
    @RequiresApi(UPSIDE_DOWN_CAKE)
    fun toLoadSdkException(): LoadSdkException {
        return Api33Impl.toLoadSdkException(this)
    }

    // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
    @RequiresApi(UPSIDE_DOWN_CAKE)
    private object Api33Impl {

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
         * @see [android.app.sdksandbox.SdkSandboxManager.SDK_SANDBOX_PROCESS_NOT_AVAILABLE]
         */
        const val SDK_SANDBOX_PROCESS_NOT_AVAILABLE = 503

        /**
         * SDK not found.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_NOT_FOUND]
         */
        const val LOAD_SDK_NOT_FOUND = 100

        /**
         * SDK is already loaded.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_ALREADY_LOADED]
         */
        const val LOAD_SDK_ALREADY_LOADED = 101

        /**
         * SDK error after being loaded.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_SDK_DEFINED_ERROR]
         */
        const val LOAD_SDK_SDK_DEFINED_ERROR = 102

        /**
         * SDK sandbox is disabled.
         *
         * @see [android.app.sdksandbox.SdkSandboxManager.LOAD_SDK_SDK_SANDBOX_DISABLED]
         */
        const val LOAD_SDK_SDK_SANDBOX_DISABLED = 103

        /** Internal error while loading SDK.
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
        // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
        @RequiresApi(UPSIDE_DOWN_CAKE)
        @JvmStatic
        fun toLoadCompatSdkException(ex: LoadSdkException): LoadSdkCompatException {
            return Api33Impl.toLoadCompatSdkException(ex)
        }
    }
}