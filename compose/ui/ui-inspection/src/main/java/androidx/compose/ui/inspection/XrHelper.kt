/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import java.lang.reflect.Method

private const val VIEW_INSPECTOR_CLASS = "com.android.tools.agent.appinspection.ViewLayoutInspector"
private const val VIEW_XR_HELPER_CLASS = "com.android.tools.agent.appinspection.XrHelper"
private const val GET_XR_VIEWS_METHOD = "getXrViews"

class XrHelper(private val environment: InspectorEnvironment) {
    private var viewHelper: ViewXrHelper? = null

    /** Get all the views from XR by delegating to the View Inspector. */
    fun getXrViews(): List<View> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return emptyList()
        }
        return try {
            val helper = findHelperAndGetViewsMethod() ?: return emptyList()
            helper.execute()
        } catch (ex: Exception) {
            Log.w(SPAM_LOG_TAG, "An error happened trying to find XR views", ex)
            emptyList()
        }
    }

    private fun findHelperAndGetViewsMethod(): ViewXrHelper? {
        viewHelper?.let {
            return it
        }
        val artTooling = environment.artTooling()
        val inspectors = artTooling.findInstances(Inspector::class.java)
        val inspector = inspectors.find { it.javaClass.name == VIEW_INSPECTOR_CLASS } ?: return null
        val xrHelperClass = inspector.javaClass.classLoader!!.loadClass(VIEW_XR_HELPER_CLASS)
        val getViewsMethod = xrHelperClass.getDeclaredMethod(GET_XR_VIEWS_METHOD)
        val helper = artTooling.findInstances(xrHelperClass).firstOrNull() ?: return null
        viewHelper = ViewXrHelper(helper, getViewsMethod)
        return viewHelper
    }

    private data class ViewXrHelper(private val helper: Any, private val getViewsMethod: Method) {
        @RequiresApi(Build.VERSION_CODES.R)
        @Suppress("UNCHECKED_CAST")
        @SuppressLint("BanUncheckedReflection")
        fun execute(): List<View> = getViewsMethod.invoke(helper) as List<View>
    }
}
