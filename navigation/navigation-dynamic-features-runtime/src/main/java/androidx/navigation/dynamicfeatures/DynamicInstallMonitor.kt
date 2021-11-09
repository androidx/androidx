/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import androidx.annotation.RestrictTo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

/**
 * Monitor installation progress of dynamic feature modules.
 * This class enables you to subscribe to the current installation state via [getStatus].
 * You also can perform various checks on installation state directly through this monitor.
 *
 * In order to enable installation and monitoring of progress you'll have to provide an instance
 * of this class to [DynamicExtras].
 */
public class DynamicInstallMonitor {

    /**
     * The occurred exception, if any.
     */
    public var exception: Exception? = null
        internal set

    /**
     * Get a LiveData of [SplitInstallSessionStatus] with updates on the installation progress.
     */
    public val status: LiveData<SplitInstallSessionState> = MutableLiveData()

    /**
     * Check whether an installation is required.
     *
     * If this returns `true`, you should observe the LiveData returned by
     * [status] for installation updates and handle them accordingly.
     *
     * @return `true` if installation is required, `false` otherwise.
     */
    public var isInstallRequired: Boolean = false
        internal set(installRequired) {
            field = installRequired
            if (installRequired) {
                isUsed = true
            }
        }

    /**
     * The session id from Play Core for this installation session.
     */
    public var sessionId: Int = 0
        internal set

    /**
     * The [SplitInstallManager] used to monitor the installation if any was set.
     * @hide
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public var splitInstallManager: SplitInstallManager? = null

    /**
     * `true` if the monitor has been used to request an install, else
     * `false`.
     * @hide
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    internal var isUsed = false
        private set

    /**
     * Cancel the current split installation session in the SplitInstallManager.
     */
    public fun cancelInstall() {
        val splitInstallManager = splitInstallManager
        if (splitInstallManager != null && sessionId != 0) {
            splitInstallManager.cancelInstall(sessionId)
        }
    }
}
