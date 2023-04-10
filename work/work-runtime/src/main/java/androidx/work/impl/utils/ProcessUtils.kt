/*
 * Copyright 2020 The Android Open Source Project
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
@file:JvmName("ProcessUtils")

package androidx.work.impl.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.WorkManager

private val TAG = Logger.tagWithPrefix("ProcessUtils")

/**
 * @return `true` when `WorkManager` is running in the configured app process.
 */
fun isDefaultProcess(context: Context, configuration: Configuration): Boolean {
    val processName = getProcessName(context)
    return if (!configuration.defaultProcessName.isNullOrEmpty()) {
        processName == configuration.defaultProcessName
    } else {
        processName == context.applicationInfo.processName
    }
}

/**
 * @return The name of the active process.
 */
@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
private fun getProcessName(context: Context): String? {
    if (Build.VERSION.SDK_INT >= 28) return Api28Impl.processName

    // Try using ActivityThread to determine the current process name.
    try {
        val activityThread = Class.forName(
            "android.app.ActivityThread",
            false,
            WorkManager::class.java.classLoader
        )
        val packageName = if (Build.VERSION.SDK_INT >= 18) {
            val currentProcessName = activityThread.getDeclaredMethod("currentProcessName")
            currentProcessName.isAccessible = true
            currentProcessName.invoke(null)!!
        } else {
            val getActivityThread = activityThread.getDeclaredMethod("currentActivityThread")
            getActivityThread.isAccessible = true
            val getProcessName = activityThread.getDeclaredMethod("getProcessName")
            getProcessName.isAccessible = true
            getProcessName.invoke(getActivityThread.invoke(null))!!
        }
        if (packageName is String) return packageName
    } catch (exception: Throwable) {
        Logger.get().debug(TAG, "Unable to check ActivityThread for processName", exception)
    }

    // Fallback to the most expensive way
    val pid = Process.myPid()
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.runningAppProcesses?.find { process -> process.pid == pid }?.processName
}

@RequiresApi(28)
private object Api28Impl {
    @get:DoNotInline
    val processName: String
        get() = Application.getProcessName()
}
