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

package androidx.xr.scenecore.samples.commontestview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Locale

class CommonTestView : LinearLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        Log.v(TAG, "Initializing the Common Test View")
        inflate(context, R.layout.common_test_layout, this)

        val buttonRecreateActivity: Button = findViewById(R.id.buttonRecreateActivity)
        buttonRecreateActivity.setOnClickListener {
            val owningActivity = context.getActivity()
            owningActivity?.let { activity ->
                ActivityCompat.recreate(activity)
                Log.i(TAG, "Activity ${activity.componentName} will be recreated")
            } ?: Log.e(TAG, "Could not retrieve activity to recreate for button")
        }

        val textApplicationName: TextView = findViewById(R.id.textApplicationName)
        textApplicationName.text = context.getAppName()

        // Displays a string similar to what's listed under Settings->About Phone->Build Number
        val textBuildFingerprint: TextView = findViewById(R.id.textBuildFingerprint)
        textBuildFingerprint.text = Build.FINGERPRINT

        val textBuildDate: TextView = findViewById(R.id.textBuildDate)
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
        textBuildDate.text = simpleDateFormat.format(Build.TIME)

        val textClNumber: TextView = findViewById(R.id.textClNumber)
        textClNumber.text = ""
    }

    // Recursively get the activity attached to this Context
    private fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

    private fun Context.getAppName(): String {
        return applicationInfo.loadLabel(packageManager).toString()
    }

    companion object {
        private val TAG = CommonTestView::class.java.simpleName
    }
}
