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
package androidx.xr.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.ArrayMap
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.xr.projected.platform.IEngagementModeCallback
import androidx.xr.projected.platform.IEngagementModeService
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.math.min

/**
 * Client for connecting to a service for getting engagement mode.
 *
 * This client discovers and binds to a system service that implements the
 * [EngagementModeClient.SERVICE_ACTION] action.
 */
internal class EngagementModeClient(context: Context, private val mHandler: Handler) {
    private val mContext: Context = context.getApplicationContext()
    private val mLock = Any()

    @GuardedBy("mLock") private val mUpdateCallbacks = ArrayMap<Consumer<Int>, Executor>()

    @GuardedBy("mLock") private var mService: IEngagementModeService? = null

    @GuardedBy("mLock") private var mIsBound = false

    @GuardedBy("mLock") private var mReconnectDelayMs: Long = INITIAL_RECONNECT_DELAY_MS

    @GuardedBy("mLock") private var mCurrentEngagementModeFlags: Int? = null

    /** Returns the current engagement mode flags. */
    fun getEngagementModeFlags(): Int? {
        synchronized(mLock) {
            return mCurrentEngagementModeFlags
        }
    }

    /**
     * Adds a callback to be invoked when the engagement mode changes.
     *
     * @param executor the executor on which the callback will be invoked.
     * @param callback the callback to be invoked.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun addUpdateCallback(executor: Executor, callback: Consumer<Int>) {
        var shouldConnect = false
        var currentFlags: Int? = null
        synchronized(mLock) {
            val wasEmpty = mUpdateCallbacks.isEmpty()
            if (mUpdateCallbacks.put(callback, executor) == null && wasEmpty) {
                shouldConnect = true
            } else {
                mCurrentEngagementModeFlags?.let {
                    // If the service was already called update the current engagement mode for the
                    // new callback.
                    currentFlags = it
                }
            }
        }
        if (shouldConnect) {
            connectToService() // Connect to service outside of lock to prevent deadlock
        }
        currentFlags?.let { executor.execute { callback.accept(it) } }
    }

    /**
     * Removes a callback that was previously added.
     *
     * @param callback the callback to be removed.
     */
    fun removeUpdateCallback(callback: Consumer<Int>) {
        synchronized(mLock) {
            if (mUpdateCallbacks.remove(callback) != null && mUpdateCallbacks.isEmpty()) {
                // Last listener is gone, cancel any pending reconnect attempts.
                mHandler.removeCallbacksAndMessages(null)

                if (mIsBound) {
                    mContext.unbindService(mConnection)
                    mService = null
                    mIsBound = false
                }
                // Revert to the null on disconnect to ensure that no stale value is reported while
                // the service is down.
                mCurrentEngagementModeFlags = null
            }
        }
    }

    private fun connectToService() {
        val intent = Intent(SERVICE_ACTION)
        val pm = mContext.packageManager
        val resolveInfo = pm.resolveService(intent, PackageManager.MATCH_ALL)
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Log.i(TAG, "No EngagementModeService found")
            return
        }
        val packageName = resolveInfo.serviceInfo.packageName
        if (!verifyProviderIsSystemApp(packageName)) {
            Log.e(TAG, "EngagementModeService is not a system app on " + packageName)
            return
        }
        intent.setPackage(packageName)

        try {
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException binding to EngagementModeService", e)
        }
    }

    private val mConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
                synchronized(mLock) {
                    // Check if all listeners were removed before binding is done. If so, unbind
                    // immediately and clean up to prevent a leak.
                    if (mUpdateCallbacks.isEmpty()) {
                        mContext.unbindService(mConnection)
                        mService = null
                        mIsBound = false
                        return
                    }

                    // Set service as bound.
                    mService = IEngagementModeService.Stub.asInterface(service)
                    mIsBound = true

                    // Reset the reconnect delay on a successful connection.
                    mReconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
                    mService?.let {
                        try {
                            it.registerCallback(mCallback)
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Error interacting with EngagementModeService on connect", e)
                            // If registration fails, the connection is useless. Clean up.
                            mService = null
                            mIsBound = false
                        }
                    }
                }
            }

            override fun onServiceDisconnected(className: ComponentName?) {
                var callbacksToNotify: MutableMap<Consumer<Int>, Executor>? = null
                synchronized(mLock) {
                    mService = null
                    mIsBound = false

                    // Revert to the null on disconnect to ensure that no stale value is reported
                    // while the service is down.
                    mCurrentEngagementModeFlags = null

                    // Only attempt to reconnect if there are still active listeners. This
                    // prevents zombie reconnects if all listeners have unregistered while the
                    // service was down.
                    if (!mUpdateCallbacks.isEmpty()) {
                        // Service crashed or was killed. Try to reconnect after a delay.
                        mHandler.postDelayed(Runnable { connectToService() }, mReconnectDelayMs)
                        // Increase the delay for the next attempt.
                        mReconnectDelayMs =
                            min(mReconnectDelayMs * RECONNECT_MULTIPLIER, MAX_RECONNECT_DELAY_MS)
                    }
                }
            }
        }

    private val mCallback: IEngagementModeCallback.Stub =
        object : IEngagementModeCallback.Stub() {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onEngagementModeChanged(engagementModeFlags: Int) {
                val callbacksToNotify: MutableMap<Consumer<Int>, Executor>?
                // Always state that audio is engaged for now.
                val updatedFlags = engagementModeFlags or ENGAGEMENT_MODE_FLAG_AUDIO_ON
                // Check if the engagement mode flag has been updated. If it has copy the callbacks
                // so that they can be called outside the lock.
                synchronized(mLock) {
                    if (mCurrentEngagementModeFlags == updatedFlags) {
                        return
                    }
                    mCurrentEngagementModeFlags = updatedFlags
                    callbacksToNotify = ArrayMap(mUpdateCallbacks)
                }
                callbacksToNotify?.forEach { (callback: Consumer<Int>, executor: Executor) ->
                    executor.execute { callback.accept(updatedFlags) }
                }
            }

            override fun getInterfaceVersion(): Int = VERSION
        }

    private fun verifyProviderIsSystemApp(packageName: String): Boolean {
        try {
            val pm = mContext.getPackageManager()
            val packageInfo = pm.getPackageInfo(packageName, 0)
            val applicationInfo = packageInfo.applicationInfo ?: return false
            return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not find package info for $packageName", e)
            return false
        }
    }

    companion object {
        const val SERVICE_ACTION: String = "androidx.xr.projected.ACTION_ENGAGEMENT_BIND"

        /**
         * A flag indicating the engagement mode includes a visual presentation. When this flag is
         * set, it means the user can visually see the app UI on visible window.
         */
        const val ENGAGEMENT_MODE_FLAG_VISUALS_ON: Int =
            IEngagementModeService.ENGAGEMENT_STATE_FLAG_VISUALS_ON

        /**
         * A flag indicating the engagement mode includes an audio presentation. This can be set
         * with or without [ENGAGEMENT_MODE_FLAG_VISUALS_ON]. When set without, it signifies an
         * audio-only experience.
         */
        const val ENGAGEMENT_MODE_FLAG_AUDIO_ON: Int =
            IEngagementModeService.ENGAGEMENT_STATE_FLAG_AUDIO_ON

        private const val TAG = "EngagementModeClient"
        @VisibleForTesting const val INITIAL_RECONNECT_DELAY_MS: Long = 10L
        private const val MAX_RECONNECT_DELAY_MS = 5000L
        private const val RECONNECT_MULTIPLIER = 10
    }
}
