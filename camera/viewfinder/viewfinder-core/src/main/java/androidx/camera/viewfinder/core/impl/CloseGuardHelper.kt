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
package androidx.camera.viewfinder.core.impl

import android.os.Build
import android.util.CloseGuard
import androidx.annotation.RequiresApi

/**
 * Helper for accessing CloseGuard on API levels that support it.
 *
 * All operations will be no-ops on non-supported API levels.
 */
internal class CloseGuardHelper private constructor(private val impl: CloseGuardImpl) {
    /**
     * Initializes the instance with a warning that the caller should have explicitly called the
     * `closeMethodName` method instead of relying on finalization.
     *
     * @param closeMethodName non-null name of explicit termination method. Printed by warnIfOpen.
     * @throws NullPointerException if closeMethodName is null.
     */
    fun open(closeMethodName: String) {
        impl.open(closeMethodName)
    }

    /** Marks this CloseGuard instance as closed to avoid warnings on finalization. */
    fun close() {
        impl.close()
    }

    /**
     * Logs a warning if the caller did not properly cleanup by calling an explicit close method
     * before finalization.
     */
    fun warnIfOpen() {
        impl.warnIfOpen()
    }

    companion object {
        /**
         * Returns a [CloseGuardHelper] which defers to the platform close guard if it is available.
         */
        fun create(): CloseGuardHelper {
            if (Build.VERSION.SDK_INT >= 30) {
                return CloseGuardHelper(CloseGuardApi30Impl())
            }

            return CloseGuardHelper(CloseGuardNoOpImpl())
        }
    }
}

@RequiresApi(30)
private class CloseGuardApi30Impl : CloseGuardImpl {
    private val platformImpl = CloseGuard()

    override fun open(closeMethodName: String) {
        platformImpl.open(closeMethodName)
    }

    override fun close() {
        platformImpl.close()
    }

    override fun warnIfOpen() {
        platformImpl.warnIfOpen()
    }
}

private class CloseGuardNoOpImpl : CloseGuardImpl {
    override fun open(closeMethodName: String) {}

    override fun close() {}

    override fun warnIfOpen() {}
}

private interface CloseGuardImpl {
    fun open(closeMethodName: String)

    fun close()

    fun warnIfOpen()
}
