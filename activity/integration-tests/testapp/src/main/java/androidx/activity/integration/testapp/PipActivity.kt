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

package androidx.activity.integration.testapp

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.trackPipAnimationHintView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class PipActivity : ComponentActivity() {

    private lateinit var moveButton: Button
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pip_activity)
        moveButton = findViewById(R.id.moveButton)
        textView = findViewById(R.id.textView)

        moveToRandomPosition()
        moveButton.setOnClickListener {
            moveToRandomPosition()
        }

        trackHintView()
    }

    override fun onPictureInPictureRequested(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26Impl.enterPictureInPictureMode(this)
        }
        return true
    }

    @ExperimentalCoroutinesApi
    private fun trackHintView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    trackPipAnimationHintView(moveButton)
                }
            }
        }
    }

    private fun moveToRandomPosition() {
        moveButton.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            randomPosition().forEach { rule -> addRule(rule) }
        }
    }

    private fun randomPosition(): List<Int> {
        return when ((0..4).random()) {
            0 -> listOf(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL)
            1 -> listOf(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_HORIZONTAL)
            2 -> listOf(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.CENTER_VERTICAL)
            3 -> listOf(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.CENTER_VERTICAL)
            4 -> listOf(RelativeLayout.CENTER_IN_PARENT)
            else -> throw IllegalArgumentException()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal object Api26Impl {
        fun enterPictureInPictureMode(activity: Activity) {
            activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }
}