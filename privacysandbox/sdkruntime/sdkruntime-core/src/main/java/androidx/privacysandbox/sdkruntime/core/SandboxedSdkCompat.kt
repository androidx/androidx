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
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.IBinder
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat

/**
 * Compat wrapper for [SandboxedSdk].
 * Represents an SDK loaded in the sandbox process or locally.
 *
 * @see [SandboxedSdk]
 *
 */
sealed class SandboxedSdkCompat {

    /**
     * Returns the interface to the loaded SDK.
     *
     * @return Binder object for loaded SDK.
     *
     * @see [SandboxedSdk.getInterface]
     */
    @DoNotInline
    abstract fun getInterface(): IBinder?

    /**
     * Create [SandboxedSdk] from compat object.
     *
     * @return Platform SandboxedSdk
     */
    // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
    @RequiresApi(UPSIDE_DOWN_CAKE)
    @DoNotInline
    abstract fun toSandboxedSdk(): SandboxedSdk

    // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
    @RequiresApi(UPSIDE_DOWN_CAKE)
    private class Api33Impl(private val mSandboxedSdk: SandboxedSdk) : SandboxedSdkCompat() {
        constructor(binder: IBinder) : this(createSandboxedSdk(binder))

        @DoNotInline
        override fun getInterface(): IBinder? {
            return mSandboxedSdk.getInterface()
        }

        @DoNotInline
        override fun toSandboxedSdk(): SandboxedSdk {
            return mSandboxedSdk
        }

        companion object {
            fun createSandboxedSdk(binder: IBinder): SandboxedSdk {
                return SandboxedSdk(binder)
            }
        }
    }

    private class CompatImpl(private val mInterface: IBinder) : SandboxedSdkCompat() {

        @DoNotInline
        override fun getInterface(): IBinder? {
            return mInterface
        }

        // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
        @RequiresApi(UPSIDE_DOWN_CAKE)
        @DoNotInline
        override fun toSandboxedSdk(): SandboxedSdk {
            // avoid class verifications errors
            return Api33Impl.createSandboxedSdk(mInterface)
        }
    }

    companion object {
        /**
         *  Creates [SandboxedSdkCompat] object from SDK Binder object.
         *
         *  @param binder Binder object for SDK Interface
         *  @return SandboxedSdkCompat object
         *
         *  @see [SandboxedSdk]
         */
        @JvmStatic
        @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        fun create(binder: IBinder): SandboxedSdkCompat {
            // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
            return if (BuildCompat.isAtLeastU()) {
                Api33Impl(binder)
            } else {
                CompatImpl(binder)
            }
        }

        /**
         *  Creates [SandboxedSdkCompat] object from existing [SandboxedSdk] object.
         *
         *  @param sandboxedSdk SandboxedSdk object
         *  @return SandboxedSdkCompat object
         */
        // TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
        @RequiresApi(UPSIDE_DOWN_CAKE)
        @JvmStatic
        fun toSandboxedSdkCompat(sandboxedSdk: SandboxedSdk): SandboxedSdkCompat {
            return Api33Impl(sandboxedSdk)
        }
    }
}