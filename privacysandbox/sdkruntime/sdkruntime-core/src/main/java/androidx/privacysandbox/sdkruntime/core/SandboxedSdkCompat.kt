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

import android.app.sdksandbox.SandboxedSdk
import android.os.IBinder
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.DoNotInline
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Compat wrapper for [SandboxedSdk].
 * Represents an SDK loaded in the sandbox process or locally.
 * An application should use this object to obtain an interface to the SDK through [getInterface].
 *
 * The SDK should create it when [SandboxedSdkProviderCompat.onLoadSdk] is called, and drop all
 * references to it when [SandboxedSdkProviderCompat.beforeUnloadSdk] is called. Additionally, the
 * SDK should fail calls made to the [IBinder] returned from [getInterface] after
 * [SandboxedSdkProviderCompat.beforeUnloadSdk] has been called.
 *
 * @see [SandboxedSdk]
 *
 */
class SandboxedSdkCompat private constructor(
    private val sdkImpl: SandboxedSdkImpl
) {

    /**
     * Creates SandboxedSdkCompat from SDK Binder object.
     *
     * @param sdkInterface The SDK's interface. This will be the entrypoint into the sandboxed SDK
     * for the application. The SDK should keep this valid until it's loaded in the sandbox, and
     * start failing calls to this interface once it has been unloaded
     *
     * This interface can later be retrieved using [getInterface].
     *
     * @see [SandboxedSdk]
     */
    constructor(sdkInterface: IBinder) : this(sdkInterface, sdkInfo = null)

    /**
     * Creates SandboxedSdkCompat from SDK [IBinder] object and [SandboxedSdkInfo].
     *
     * @param sdkInterface The SDK's interface.
     * @param sdkInfo Information about SDK's name and version.
     */
    @Keep // Reflection call from client part
    @RestrictTo(LIBRARY_GROUP)
    constructor(
        sdkInterface: IBinder,
        sdkInfo: SandboxedSdkInfo?
    ) : this(CompatImpl(sdkInterface, sdkInfo))

    /**
     * Creates SandboxedSdkCompat wrapper around existing [SandboxedSdk] object.
     *
     * @param sandboxedSdk SandboxedSdk object. All calls will be delegated to that object.
     */
    @RequiresExtension(extension = AD_SERVICES, version = 4)
    @RestrictTo(LIBRARY_GROUP)
    constructor(sandboxedSdk: SandboxedSdk) : this(
        SdkImplFactory.createSdkImpl(sandboxedSdk)
    )

    /**
     * Returns the interface to the loaded SDK.
     * A null interface is returned if the Binder has since
     * become unavailable, in response to the SDK being unloaded.
     *
     * @return [IBinder] object for loaded SDK.
     *
     * @see [SandboxedSdk.getInterface]
     */
    fun getInterface() = sdkImpl.getInterface()

    /**
     * Returns information about loaded SDK.
     *
     * @return [SandboxedSdkInfo] object for loaded SDK or null if no information available.
     *
     * @see [SandboxedSdk.getSharedLibraryInfo]
     */
    fun getSdkInfo(): SandboxedSdkInfo? = sdkImpl.getSdkInfo()

    /**
     * Create [SandboxedSdk] from compat object.
     *
     * @return Platform SandboxedSdk
     */
    @RequiresExtension(extension = AD_SERVICES, version = 4)
    internal fun toSandboxedSdk() = sdkImpl.toSandboxedSdk()

    internal interface SandboxedSdkImpl {
        fun getInterface(): IBinder?

        fun getSdkInfo(): SandboxedSdkInfo?

        @RequiresExtension(extension = AD_SERVICES, version = 4)
        @DoNotInline
        fun toSandboxedSdk(): SandboxedSdk
    }

    @RequiresExtension(extension = AD_SERVICES, version = 4)
    private open class ApiAdServicesV4Impl(
        protected val sandboxedSdk: SandboxedSdk
    ) : SandboxedSdkImpl {

        @DoNotInline
        override fun getInterface(): IBinder? {
            return sandboxedSdk.getInterface()
        }

        override fun getSdkInfo(): SandboxedSdkInfo? = null

        @DoNotInline
        override fun toSandboxedSdk(): SandboxedSdk {
            return sandboxedSdk
        }

        companion object {
            @DoNotInline
            fun createSandboxedSdk(sdkInterface: IBinder): SandboxedSdk {
                return SandboxedSdk(sdkInterface)
            }
        }
    }

    @RequiresApi(33)
    @RequiresExtension(extension = AD_SERVICES, version = 5)
    private class ApiAdServicesV5Impl(
        sandboxedSdk: SandboxedSdk
    ) : ApiAdServicesV4Impl(sandboxedSdk) {

        override fun getSdkInfo(): SandboxedSdkInfo {
            val sharedLibraryInfo = sandboxedSdk.sharedLibraryInfo
            return SandboxedSdkInfo(
                name = sharedLibraryInfo.name,
                version = sharedLibraryInfo.longVersion,
            )
        }
    }

    @RequiresExtension(extension = AD_SERVICES, version = 4)
    private object SdkImplFactory {
        fun createSdkImpl(sandboxedSdk: SandboxedSdk): SandboxedSdkImpl {
            return if (AdServicesInfo.isAtLeastV5()) {
                ApiAdServicesV5Impl(sandboxedSdk)
            } else {
                ApiAdServicesV4Impl(sandboxedSdk)
            }
        }
    }

    private class CompatImpl(
        private val sdkInterface: IBinder,
        private val sdkInfo: SandboxedSdkInfo?
    ) : SandboxedSdkImpl {

        override fun getInterface(): IBinder {
            // This will be null if the SDK has been unloaded and the IBinder originally provided
            // is now a dead object.
            return sdkInterface
        }

        override fun getSdkInfo(): SandboxedSdkInfo? = sdkInfo

        @RequiresExtension(extension = AD_SERVICES, version = 4)
        override fun toSandboxedSdk(): SandboxedSdk {
            // avoid class verifications errors
            return ApiAdServicesV4Impl.createSandboxedSdk(sdkInterface)
        }
    }
}
