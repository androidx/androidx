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

package androidx.pdf.ink

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.pdf.ink.view.AnnotationToolbar

/**
 * A [View.OnTouchListener] responsible for detecting touches outside of the toolbar.
 *
 * If a popup on [AnnotationToolbar] is displayed, this listener consumes the touch event and
 * dismisses the popup, ensuring that the touch does not pass through to underlying views
 * (preventing accidental drawing or scrolling).
 */
internal class PopupDismissalTouchListener(private val annotationToolbar: AnnotationToolbar) :
    View.OnTouchListener {

    private var downX = 0f
    private var downY = 0f
    private var isDismissing = false
    private var touchSlop = ViewConfiguration.get(annotationToolbar.context).scaledTouchSlop

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view == null || event == null) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (annotationToolbar.isConfigPopupVisible) {
                    annotationToolbar.dismissPopups()
                    isDismissing = true
                    downX = event.x
                    downY = event.y
                    return true // Consume DOWN to prevent immediate drawing
                }
                isDismissing = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDismissing) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val distanceSquared = dx * dx + dy * dy

                    // If movement is within slop (jitter), consume it so no dot is drawn.
                    // If movement exceeds slop (drag), return false to let downstream handle it.
                    if (distanceSquared < touchSlop * touchSlop) {
                        return true
                    } else {
                        // If we're passing the event downstream, let it handle the ACTION_UP and
                        // ACTION_CANCEL events too.
                        isDismissing = false
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isDismissing) {
                    isDismissing = false
                    // Consume the UP event to complete the "swallow" of the tap
                    return true
                }
            }
        }
        return false
    }
}
