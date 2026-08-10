/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.pip.contentpip

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

/** An internal translucent Activity that serves as the PiP container in a separate task. */
internal class ContentPipInternalActivity : ComponentActivity() {
    private var callback: ContentPipCallback? = null
    private var hasRequestedEnterPip = false
    private var isStopped = false
    @RequiresApi(Build.VERSION_CODES.O)
    private var pictureInPictureParams: PictureInPictureParams =
        PictureInPictureParams.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyZeroTransitions()

        val cb = ContentPipManager.consumeHandoff()
        if (cb == null) {
            finishAndRemoveTask()
            return
        }

        callback = cb
        cb.onAttachContentPip(this)

        // Register the finish hook so the main activity can "pull back" the player
        ContentPipManager.setFinishProxyHook { finishAndRemoveTask() }

        // Move task to back immediately so it doesn't interrupt the user's current gesture
        moveTaskToBack(true)
    }

    override fun onStart() {
        super.onStart()
        if (!hasRequestedEnterPip) {
            hasRequestedEnterPip = true
            // Trigger PiP from the backgrounded state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val entered = enterPictureInPictureMode(pictureInPictureParams)
                if (!entered) {
                    // The enterPictureInPictureMode attempt failed, finish content PiP process
                    // as if PiP is closed.
                    ContentPipManager.onTeardown(true)
                    finishAndRemoveTask()
                    return
                }
            } else {
                @Suppress("DEPRECATION") enterPictureInPictureMode()
            }
        }
        isStopped = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun setPictureInPictureParams(params: PictureInPictureParams) {
        super.setPictureInPictureParams(params)
        pictureInPictureParams = params
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        @Suppress("DEPRECATION")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            // Expanding, teardown the task
            ContentPipManager.onTeardown(isStopped)
            finishAndRemoveTask()
        }
    }

    override fun onStop() {
        super.onStop()
        isStopped = true
    }

    private fun applyZeroTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
    }
}
