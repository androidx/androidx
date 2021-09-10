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

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.Navigator
import androidx.navigation.dynamicfeatures.DynamicGraphNavigator.DynamicNavGraph
import androidx.navigation.get
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

/**
 * Install manager for dynamic features.
 *
 * Enables installation of dynamic features for both installed and instant apps.
 */
public open class DynamicInstallManager(
    private val context: Context,
    private val splitInstallManager: SplitInstallManager
) {

    internal companion object {
        internal fun terminateLiveData(
            status: MutableLiveData<SplitInstallSessionState>
        ) {
            // Best effort leak prevention, will only work for active observers
            check(!status.hasActiveObservers()) {
                "This DynamicInstallMonitor will not " +
                    "emit any more status updates. You should remove all " +
                    "Observers after null has been emitted."
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun performInstall(
        backStackEntry: NavBackStackEntry,
        extras: DynamicExtras?,
        moduleName: String
    ): NavDestination? {
        if (extras?.installMonitor != null) {
            requestInstall(moduleName, extras.installMonitor)
            return null
        } else {
            val progressArgs = Bundle().apply {
                putInt(Constants.DESTINATION_ID, backStackEntry.destination.id)
                putBundle(Constants.DESTINATION_ARGS, backStackEntry.arguments)
            }
            val dynamicNavGraph = DynamicNavGraph.getOrThrow(backStackEntry.destination)
            val navigator: Navigator<*> =
                dynamicNavGraph.navigatorProvider[dynamicNavGraph.navigatorName]
            if (navigator is DynamicGraphNavigator) {
                navigator.navigateToProgressDestination(dynamicNavGraph, progressArgs)
                return null
            } else {
                throw IllegalStateException(
                    "You must use a DynamicNavGraph to perform a module installation."
                )
            }
        }
    }

    /**
     * @param module The module to install.
     * @return Whether the requested module needs installation.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun needsInstall(module: String): Boolean {
        return !splitInstallManager.installedModules.contains(module)
    }

    private fun requestInstall(
        module: String,
        installMonitor: DynamicInstallMonitor
    ) {
        check(!installMonitor.isUsed) {
            // We don't want an installMonitor in an undefined state or used by another install
            "You must pass in a fresh DynamicInstallMonitor " +
                "in DynamicExtras every time you call navigate()."
        }

        val status = installMonitor.status as MutableLiveData<SplitInstallSessionState>
        installMonitor.isInstallRequired = true

        val request = SplitInstallRequest
            .newBuilder()
            .addModule(module)
            .build()

        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { sessionId ->
                installMonitor.sessionId = sessionId
                installMonitor.splitInstallManager = splitInstallManager
                if (sessionId == 0) {
                    // The feature is already installed, emit synthetic INSTALLED state.
                    status.value = SplitInstallSessionState.create(
                        sessionId,
                        SplitInstallSessionStatus.INSTALLED,
                        SplitInstallErrorCode.NO_ERROR,
                        /* bytesDownloaded */ 0,
                        /* totalBytesToDownload */ 0,
                        listOf(module),
                        emptyList()
                    )
                    terminateLiveData(status)
                } else {
                    val listener = SplitInstallListenerWrapper(
                        context, status,
                        installMonitor
                    )
                    splitInstallManager.registerListener(listener)
                }
            }
            .addOnFailureListener { exception ->
                Log.i(
                    "DynamicInstallManager",
                    "Error requesting install of $module: ${exception.message}"
                )
                installMonitor.exception = exception
                status.value = SplitInstallSessionState.create(
                    /* sessionId */ 0,
                    SplitInstallSessionStatus.FAILED,
                    if (exception is SplitInstallException)
                        exception.errorCode
                    else
                        SplitInstallErrorCode.INTERNAL_ERROR,
                    /* bytesDownloaded */ 0,
                    /* totalBytesToDownload */ 0,
                    listOf(module),
                    emptyList()
                )
                terminateLiveData(status)
            }
    }

    private class SplitInstallListenerWrapper(
        private val context: Context,
        private val status: MutableLiveData<SplitInstallSessionState>,
        private val installMonitor: DynamicInstallMonitor
    ) : SplitInstallStateUpdatedListener {

        override fun onStateUpdate(
            splitInstallSessionState: SplitInstallSessionState
        ) {
            if (splitInstallSessionState.sessionId() == installMonitor.sessionId) {
                if (splitInstallSessionState.status() == SplitInstallSessionStatus.INSTALLED) {
                    SplitCompat.install(context)
                    // Enable immediate usage of dynamic feature modules in an instant app context.
                    SplitInstallHelper.updateAppInfo(context)
                }
                status.value = splitInstallSessionState
                if (splitInstallSessionState.hasTerminalStatus()) {
                    installMonitor.splitInstallManager!!.unregisterListener(this)
                    terminateLiveData(status)
                }
            }
        }
    }
}
