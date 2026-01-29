/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.demos.widget

import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Test the Widget via an embedded RemoteView. */
class ListRemoteViewActivity : ComponentActivity() {
    // Enable Talkback
    // adb shell settings put secure enabled_accessibility_services
    // com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService

    // Disable Talkback
    // adb shell settings delete secure enabled_accessibility_services

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val bytes = listWidget(applicationContext, "ListRemoteViewActivity")

            val frameLayout =
                FrameLayout(this@ListRemoteViewActivity).apply {
                    setBackgroundColor(Color.LightGray.toArgb())
                    setPadding(20, 200, 20, 200)
                }

            val widget = RemoteViews(DrawInstructions(bytes))
            val widgetView = widget.apply(this@ListRemoteViewActivity, frameLayout)
            frameLayout.addView(widgetView)

            setContentView(frameLayout)
        }
    }
}
