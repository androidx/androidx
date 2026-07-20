/*
 * Copyright 2017 The Android Open Source Project
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

@file:JvmName("PackageManagerHelper")

package androidx.work.impl.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.work.Logger

private val TAG = Logger.tagWithPrefix("PackageManagerHelper")
private val manifestDefaultCache = HashMap<ComponentName, Boolean>()

/** Uses [PackageManager] to enable/disable a service component defined in manifest */
public fun setServiceEnabled(context: Context, klazz: Class<*>, enabled: Boolean) {
    context.packageManager.setComponentEnabled(
        ComponentName(context, klazz),
        enabled,
        PackageManager::isServiceEnabled,
    )
}

/** Uses [PackageManager] to enable/disable a receiver component defined in manifest */
public fun setReceiverEnabled(context: Context, klazz: Class<*>, enabled: Boolean) {
    context.packageManager.setComponentEnabled(
        ComponentName(context, klazz),
        enabled,
        PackageManager::isReceiverEnabled,
    )
}

private fun PackageManager.setComponentEnabled(
    componentName: ComponentName,
    enabled: Boolean,
    isComponentEnabled: PackageManager.(ComponentName) -> Boolean,
) {
    try {
        if (enabled == isComponentEnabled(componentName)) {
            return
        }
        setComponentEnabledSetting(
            componentName,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        Logger.get()
            .debug(TAG, "${componentName.className} ${if (enabled) "enabled" else "disabled"}")
    } catch (exception: Exception) {
        Logger.get()
            .debug(
                TAG,
                "${componentName.className} could not be ${if (enabled) "enabled" else "disabled"}",
                exception,
            )
    }
}

private fun PackageManager.isServiceEnabled(componentName: ComponentName): Boolean =
    isComponentEnabled(componentName) { name, flags -> getServiceInfo(name, flags) }

private fun PackageManager.isReceiverEnabled(componentName: ComponentName): Boolean =
    isComponentEnabled(componentName) { name, flags -> getReceiverInfo(name, flags) }

/**
 * Checks whether a component is effectively enabled.
 *
 * If [PackageManager.getComponentEnabledSetting] returns
 * [PackageManager.COMPONENT_ENABLED_STATE_DEFAULT], this queries the manifest via [getInfo] to
 * check if the component is enabled by default, caching the result in [manifestDefaultCache].
 */
private inline fun PackageManager.isComponentEnabled(
    componentName: ComponentName,
    getInfo: PackageManager.(ComponentName, Int) -> ComponentInfo,
): Boolean {
    val setting = getComponentEnabledSetting(componentName)
    when (setting) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> {
            synchronized(manifestDefaultCache) {
                val cached = manifestDefaultCache[componentName]
                if (cached != null) {
                    return cached
                }
            }
            val enabled =
                try {
                    val flags =
                        if (Build.VERSION.SDK_INT >= 24) {
                            PackageManager.MATCH_DISABLED_COMPONENTS
                        } else {
                            @Suppress("DEPRECATION") PackageManager.GET_DISABLED_COMPONENTS
                        }
                    getInfo(componentName, flags).enabled
                } catch (e: PackageManager.NameNotFoundException) {
                    Logger.get()
                        .warning(
                            TAG,
                            "${componentName.className} could not be found in manifest",
                            e,
                        )
                    false
                }
            synchronized(manifestDefaultCache) { manifestDefaultCache[componentName] = enabled }
            return enabled
        }
        else -> return false
    }
}

@VisibleForTesting
internal fun clearManifestDefaultCache() {
    synchronized(manifestDefaultCache) { manifestDefaultCache.clear() }
}
