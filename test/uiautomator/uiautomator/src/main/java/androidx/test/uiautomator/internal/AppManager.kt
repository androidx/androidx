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

package androidx.test.uiautomator.internal

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Manages operations related to starting and stopping an app. */
internal class AppManager(private val context: Context) {

    private val packageManager = context.packageManager
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun startApp(packageName: String) {
        var intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        }
        if (intent == null) {
            intent = Intent(Intent.ACTION_MAIN).apply { setPackage(packageName) }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                intent.setComponent(
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name,
                    )
                )
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)
    }

    fun startActivity(packageName: String, activityName: String) {
        val intent =
            Intent().apply {
                setComponent(ComponentName(packageName, activityName))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        context.startActivity(intent)
    }

    fun startActivity(clazz: Class<*>) {
        val intent =
            Intent().apply {
                setClass(instrumentation.targetContext.applicationContext, clazz)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        context.startActivity(intent)
    }

    fun startIntent(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun clearAppData() {
        activityManager.clearApplicationUserData()
    }
}
