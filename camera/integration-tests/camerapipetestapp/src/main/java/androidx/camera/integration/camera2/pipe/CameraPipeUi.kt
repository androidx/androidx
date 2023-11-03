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

package androidx.camera.integration.camera2.pipe

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CameraPipeUi private constructor(activity: Activity) {
    companion object {
        /**
         * Set the content view for the activity and then bind basic interactions together.
         */
        fun inflate(activity: Activity): CameraPipeUi {
            activity.setContentView(R.layout.activity_main)
            return CameraPipeUi(activity)
        }
    }

    val viewfinder: Viewfinder = activity.findViewById(R.id.viewfinder)
    val viewfinder2: Viewfinder = activity.findViewById(R.id.viewfinder_secondary)
    val viewfinderText: TextView = activity.findViewById(R.id.viewfinder_text)
    val viewfinderText2: TextView = activity.findViewById(R.id.viewfinder_secondary_text)
    val switchButton: ImageButton = activity.findViewById(R.id.switch_button)
    val captureButton: ImageButton = activity.findViewById(R.id.capture_button)
    val infoButton: ImageButton = activity.findViewById(R.id.info_button)
    val infoView: LinearLayout = activity.findViewById(R.id.info_content)
    val infoText: TextView = activity.findViewById(R.id.info_text)

    @Suppress("DEPRECATION")
    private val recordBackground: Drawable = activity.resources.getDrawable(
        R.drawable.theme_round_button_record
    )

    @Suppress("DEPRECATION")
    private val defaultBackground: Drawable = activity.resources.getDrawable(
        R.drawable.theme_round_button_default
    )

    init {
        val infoViewContainer: ScrollView = activity.findViewById(R.id.info_view)
        infoButton.setOnClickListener {
            if (infoViewContainer.visibility == View.VISIBLE) {
                infoViewContainer.visibility = View.INVISIBLE
            } else {
                infoViewContainer.visibility = View.VISIBLE
                infoViewContainer.isClickable = true
            }
        }
        infoView.setOnClickListener { infoViewContainer.visibility = View.INVISIBLE }
    }

    fun disableButton(button: ImageButton) {
        button.background = defaultBackground
        button.isEnabled = false
        button.alpha = 0.25f
    }

    fun enableButton(button: ImageButton) {
        button.background = defaultBackground
        button.isEnabled = true
        button.alpha = 1.0f
    }

    fun enableRecordingButton(button: ImageButton) {
        button.background = recordBackground
        button.isEnabled = true
        button.alpha = 1.0f
    }
}
