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

package androidx.compose.remote.integration.view.demos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.remote.integration.view.demos.examples.calendarDayAgenda
import androidx.compose.remote.integration.view.demos.examples.readTodayEvents
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.core.content.ContextCompat

@SuppressLint("RestrictedApiAndroidX")
class CalendarDemoActivity : ComponentActivity() {

    private lateinit var frameLayout: FrameLayout

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showCalendar()
            } else {
                showPermissionButton()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frameLayout = FrameLayout(this)
        frameLayout.setBackgroundColor(Color.WHITE)
        setContentView(frameLayout)

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            showCalendar()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun showCalendar() {
        frameLayout.removeAllViews()
        val events = readTodayEvents(applicationContext)
        val doc = calendarDayAgenda(events)
        val buffer = doc.writer.buffer.buffer.cloneBytes()
        val remoteDoc = RemoteDocument(buffer)
        val player = RemoteComposePlayer(applicationContext)
        player.setDocument(remoteDoc)
        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        player.layoutParams = params
        frameLayout.addView(player)
    }

    private fun showPermissionButton() {
        frameLayout.removeAllViews()
        val button = Button(this)
        button.text = "Grant Calendar Permission"
        button.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        params.gravity = Gravity.CENTER
        button.layoutParams = params
        frameLayout.addView(button)
    }
}
