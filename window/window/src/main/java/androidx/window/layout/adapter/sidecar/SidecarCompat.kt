/*
 * Copyright 2021 The Android Open Source Project
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
// Sidecar is deprecated but we still need to support it.
@file:Suppress("DEPRECATION")

package androidx.window.layout.adapter.sidecar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.core.content.OnConfigurationChangedProvider
import androidx.core.util.Consumer
import androidx.window.core.Version
import androidx.window.core.Version.Companion.parse
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.sidecar.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.layout.adapter.sidecar.SidecarWindowBackend.Companion.DEBUG
import androidx.window.sidecar.SidecarDeviceState
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarInterface
import androidx.window.sidecar.SidecarInterface.SidecarCallback
import androidx.window.sidecar.SidecarProvider
import androidx.window.sidecar.SidecarWindowLayoutInfo
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Extension interface compatibility wrapper for v0.1 sidecar.  */
internal class SidecarCompat @VisibleForTesting constructor(
    @get:VisibleForTesting
    val sidecar: SidecarInterface?,
    private val sidecarAdapter: SidecarAdapter
) : ExtensionInterfaceCompat {
    // Map of active listeners registered with #onWindowLayoutChangeListenerAdded() and not yet
    // removed by #onWindowLayoutChangeListenerRemoved().
    private val windowListenerRegisteredContexts = mutableMapOf<IBinder, Activity>()
    // Map of activities registered to their component callbacks so we can keep track and
    // remove when the activity is unregistered
    private val componentCallbackMap = mutableMapOf<Activity, Consumer<Configuration>>()
    private var extensionCallback: DistinctElementCallback? = null

    constructor(context: Context) : this(
        getSidecarCompat(context),
        SidecarAdapter()
    )

    override fun setExtensionCallback(extensionCallback: ExtensionCallbackInterface) {
        this.extensionCallback = DistinctElementCallback(extensionCallback)
        sidecar?.setSidecarCallback(
            DistinctElementSidecarCallback(
                sidecarAdapter,
                TranslatingCallback()
            )
        )
    }

    @VisibleForTesting
    fun getWindowLayoutInfo(activity: Activity): WindowLayoutInfo {
        val windowToken = getActivityWindowToken(activity) ?: return WindowLayoutInfo(emptyList())
        val windowLayoutInfo = sidecar?.getWindowLayoutInfo(windowToken)
        return sidecarAdapter.translate(
            windowLayoutInfo,
            sidecar?.deviceState ?: SidecarDeviceState()
        )
    }

    override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
        val windowToken = getActivityWindowToken(activity)
        if (windowToken != null) {
            register(windowToken, activity)
        } else {
            val attachAdapter = FirstAttachAdapter(this, activity)
            activity.window.decorView.addOnAttachStateChangeListener(attachAdapter)
        }
    }

    /**
     * Register an [IBinder] token and an [Activity] so that the given
     * [Activity] will receive updates when there is a new [WindowLayoutInfo].
     * @param windowToken for the given [Activity].
     * @param activity that is listening for changes of [WindowLayoutInfo]
     */
    fun register(windowToken: IBinder, activity: Activity) {
        windowListenerRegisteredContexts[windowToken] = activity
        sidecar?.onWindowLayoutChangeListenerAdded(windowToken)
        // Since SidecarDeviceState and SidecarWindowLayout are merged we trigger both
        // data streams.
        if (windowListenerRegisteredContexts.size == 1) {
            sidecar?.onDeviceStateListenersChanged(false)
        }
        extensionCallback?.onWindowLayoutChanged(activity, getWindowLayoutInfo(activity))
        registerConfigurationChangeListener(activity)
    }

    private fun registerConfigurationChangeListener(activity: Activity) {
        // Only register a component callback if we haven't already as register
        // may be called multiple times for the same activity
        if (componentCallbackMap[activity] == null && activity is OnConfigurationChangedProvider) {
            // Create a configuration change observer to send updated WindowLayoutInfo
            // when the configuration of the app changes: b/186647126
            val configChangeObserver = Consumer<Configuration> {
                extensionCallback?.onWindowLayoutChanged(
                    activity,
                    getWindowLayoutInfo(activity)
                )
            }
            componentCallbackMap[activity] = configChangeObserver
            activity.addOnConfigurationChangedListener(configChangeObserver)
        }
    }

    override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {
        val windowToken = getActivityWindowToken(activity) ?: return
        sidecar?.onWindowLayoutChangeListenerRemoved(windowToken)
        unregisterComponentCallback(activity)
        extensionCallback?.clearWindowLayoutInfo(activity)
        val isLast = windowListenerRegisteredContexts.size == 1
        windowListenerRegisteredContexts.remove(windowToken)
        if (isLast) {
            sidecar?.onDeviceStateListenersChanged(true)
        }
    }

    private fun unregisterComponentCallback(activity: Activity) {
        val configChangeObserver = componentCallbackMap[activity] ?: return
        if (activity is OnConfigurationChangedProvider) {
            activity.removeOnConfigurationChangedListener(configChangeObserver)
        }
        componentCallbackMap.remove(activity)
    }

    @SuppressLint("BanUncheckedReflection")
    override fun validateExtensionInterface(): Boolean {
        return try {
            // sidecar.setSidecarCallback(SidecarInterface.SidecarCallback);
            val methodSetSidecarCallback = sidecar?.javaClass?.getMethod(
                "setSidecarCallback",
                SidecarCallback::class.java
            )
            val rSetSidecarCallback = methodSetSidecarCallback?.returnType
            if (rSetSidecarCallback != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'setSidecarCallback': $rSetSidecarCallback"
                )
            }

            // DO NOT REMOVE SINCE THIS IS VALIDATING THE INTERFACE.
            // sidecar.getDeviceState()
            @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
            var tmpDeviceState = sidecar?.deviceState

            // sidecar.onDeviceStateListenersChanged(boolean);
            sidecar?.onDeviceStateListenersChanged(true /* isEmpty */)

            // sidecar.getWindowLayoutInfo(IBinder)
            val methodGetWindowLayoutInfo = sidecar?.javaClass
                ?.getMethod("getWindowLayoutInfo", IBinder::class.java)
            val rtGetWindowLayoutInfo = methodGetWindowLayoutInfo?.returnType
            if (rtGetWindowLayoutInfo != SidecarWindowLayoutInfo::class.java) {
                throw NoSuchMethodException(
                    "Illegal return type for 'getWindowLayoutInfo': $rtGetWindowLayoutInfo"
                )
            }

            // sidecar.onWindowLayoutChangeListenerAdded(IBinder);
            val methodRegisterWindowLayoutChangeListener = sidecar?.javaClass
                ?.getMethod("onWindowLayoutChangeListenerAdded", IBinder::class.java)
            val rtRegisterWindowLayoutChangeListener =
                methodRegisterWindowLayoutChangeListener?.returnType
            if (rtRegisterWindowLayoutChangeListener != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'onWindowLayoutChangeListenerAdded': " +
                        "$rtRegisterWindowLayoutChangeListener"
                )
            }

            // sidecar.onWindowLayoutChangeListenerRemoved(IBinder);
            val methodUnregisterWindowLayoutChangeListener = sidecar?.javaClass
                ?.getMethod("onWindowLayoutChangeListenerRemoved", IBinder::class.java)
            val rtUnregisterWindowLayoutChangeListener =
                methodUnregisterWindowLayoutChangeListener?.returnType
            if (rtUnregisterWindowLayoutChangeListener != Void.TYPE) {
                throw NoSuchMethodException(
                    "Illegal return type for 'onWindowLayoutChangeListenerRemoved': " +
                        "$rtUnregisterWindowLayoutChangeListener"
                )
            }

            // SidecarDeviceState constructor
            tmpDeviceState = SidecarDeviceState()

            // deviceState.posture
            // TODO(b/172620880): Workaround for Sidecar API implementation issue.
            try {
                tmpDeviceState.posture = SidecarDeviceState.POSTURE_OPENED
            } catch (error: NoSuchFieldError) {
                if (DEBUG) {
                    Log.w(
                        TAG,
                        "Sidecar implementation doesn't conform to primary interface version, " +
                            "continue to check for the secondary one ${Version.VERSION_0_1}, " +
                            "error: $error"
                    )
                }
                val methodSetPosture = SidecarDeviceState::class.java.getMethod(
                    "setPosture",
                    Int::class.javaPrimitiveType
                )
                methodSetPosture.invoke(tmpDeviceState, SidecarDeviceState.POSTURE_OPENED)
                val methodGetPosture = SidecarDeviceState::class.java.getMethod("getPosture")
                val posture = methodGetPosture.invoke(tmpDeviceState) as Int
                if (posture != SidecarDeviceState.POSTURE_OPENED) {
                    throw Exception("Invalid device posture getter/setter")
                }
            }

            // SidecarDisplayFeature constructor
            val displayFeature = SidecarDisplayFeature()

            // displayFeature.getRect()/setRect()
            val tmpRect = displayFeature.rect
            displayFeature.rect = tmpRect

            // displayFeature.getType()/setType()
            @Suppress("UNUSED_VARIABLE")
            val tmpType = displayFeature.type
            displayFeature.type = SidecarDisplayFeature.TYPE_FOLD

            // SidecarWindowLayoutInfo constructor
            val windowLayoutInfo = SidecarWindowLayoutInfo()

            // windowLayoutInfo.displayFeatures
            try {
                @Suppress("UNUSED_VARIABLE")
                val tmpDisplayFeatures = windowLayoutInfo.displayFeatures
                // TODO(b/172620880): Workaround for Sidecar API implementation issue.
            } catch (error: NoSuchFieldError) {
                if (DEBUG) {
                    Log.w(
                        TAG,
                        "Sidecar implementation doesn't conform to primary interface version, " +
                            "continue to check for the secondary one ${Version.VERSION_0_1}, " +
                            "error: $error"
                    )
                }
                val featureList: MutableList<SidecarDisplayFeature> = ArrayList()
                featureList.add(displayFeature)
                val methodSetFeatures = SidecarWindowLayoutInfo::class.java.getMethod(
                    "setDisplayFeatures", MutableList::class.java
                )
                methodSetFeatures.invoke(windowLayoutInfo, featureList)
                val methodGetFeatures = SidecarWindowLayoutInfo::class.java.getMethod(
                    "getDisplayFeatures"
                )
                @Suppress("UNCHECKED_CAST")
                val resultDisplayFeatures =
                    methodGetFeatures.invoke(windowLayoutInfo) as List<SidecarDisplayFeature>
                if (featureList != resultDisplayFeatures) {
                    throw Exception("Invalid display feature getter/setter")
                }
            }
            true
        } catch (t: Throwable) {
            if (DEBUG) {
                Log.e(
                    TAG,
                    "Sidecar implementation doesn't conform to interface version " +
                        "${Version.VERSION_0_1}, error: $t"
                )
            }
            false
        }
    }

    /**
     * An adapter that will run a callback when a window is attached and then be removed from the
     * listener set.
     */
    private class FirstAttachAdapter(
        private val sidecarCompat: SidecarCompat,
        activity: Activity
    ) : View.OnAttachStateChangeListener {
        private val activityWeakReference = WeakReference(activity)
        override fun onViewAttachedToWindow(view: View) {
            view.removeOnAttachStateChangeListener(this)
            val activity = activityWeakReference.get()
            val token = getActivityWindowToken(activity)
            if (activity == null) {
                if (DEBUG) {
                    Log.d(TAG, "Unable to register activity since activity is missing")
                }
                return
            }
            if (token == null) {
                if (DEBUG) {
                    Log.w(TAG, "Unable to register activity since the window token is missing")
                }
                return
            }
            sidecarCompat.register(token, activity)
        }

        override fun onViewDetachedFromWindow(view: View) {}
    }

    /**
     * A callback to translate from Sidecar classes to local classes.
     *
     * If you change the name of this class, you must update the proguard file.
     */
    internal inner class TranslatingCallback : SidecarCallback {
        override fun onDeviceStateChanged(newDeviceState: SidecarDeviceState) {
            windowListenerRegisteredContexts.values.forEach { activity ->
                val layoutInfo = getActivityWindowToken(activity)
                    ?.let { windowToken -> sidecar?.getWindowLayoutInfo(windowToken) }
                extensionCallback?.onWindowLayoutChanged(
                    activity,
                    sidecarAdapter.translate(layoutInfo, newDeviceState)
                )
            }
        }

        override fun onWindowLayoutChanged(
            windowToken: IBinder,
            newLayout: SidecarWindowLayoutInfo
        ) {
            val activity = windowListenerRegisteredContexts[windowToken]
            if (activity == null) {
                Log.w(
                    TAG,
                    "Unable to resolve activity from window token. Missing a call to " +
                        "#onWindowLayoutChangeListenerAdded()?"
                )
                return
            }
            val layoutInfo = sidecarAdapter.translate(
                newLayout,
                sidecar?.deviceState ?: SidecarDeviceState()
            )
            extensionCallback?.onWindowLayoutChanged(activity, layoutInfo)
        }
    }

    /**
     * A class to record the last calculated values from [SidecarInterface] and filter out
     * duplicates. This class uses [WindowLayoutInfo] as opposed to
     * [SidecarDisplayFeature] since the methods [Object.equals] and
     * [Object.hashCode] may not have been overridden.
     */
    private class DistinctElementCallback(
        private val callbackInterface: ExtensionCallbackInterface
    ) : ExtensionCallbackInterface {
        private val globalLock = ReentrantLock()

        /**
         * A map from [Activity] to the last computed [WindowLayoutInfo] for the
         * given activity. A [WeakHashMap] is used to avoid retaining the [Activity].
         */
        @GuardedBy("globalLock")
        private val activityWindowLayoutInfo = WeakHashMap<Activity, WindowLayoutInfo>()
        override fun onWindowLayoutChanged(
            activity: Activity,
            newLayout: WindowLayoutInfo
        ) {
            globalLock.withLock {
                val lastInfo = activityWindowLayoutInfo[activity]
                if (newLayout == lastInfo) {
                    return
                }
                activityWindowLayoutInfo.put(activity, newLayout)
            }
            callbackInterface.onWindowLayoutChanged(activity, newLayout)
        }

        fun clearWindowLayoutInfo(activity: Activity) {
            globalLock.withLock {
                activityWindowLayoutInfo[activity] = null
            }
        }
    }

    companion object {
        private const val TAG = "SidecarCompat"
        val sidecarVersion: Version?
            get() = try {
                val vendorVersion = SidecarProvider.getApiVersion()
                if (!TextUtils.isEmpty(vendorVersion)) parse(vendorVersion) else null
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Sidecar version not found")
                }
                null
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Sidecar")
                }
                null
            }

        internal fun getSidecarCompat(context: Context): SidecarInterface? {
            return SidecarProvider.getSidecarImpl(context.applicationContext)
        }

        /**
         * A utility method [Activity] to return an optional [IBinder] window token from an
         * [Activity].
         */
        internal fun getActivityWindowToken(activity: Activity?): IBinder? {
            return activity?.window?.attributes?.token
        }
    }
}
