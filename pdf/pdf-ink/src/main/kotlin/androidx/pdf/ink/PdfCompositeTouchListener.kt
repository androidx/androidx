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

/**
 * A composite [View.OnTouchListener] that delegates touch events to a chain of listeners in order.
 *
 * This allows multiple independent touch handling strategies to be attached to a single view. The
 * listeners are executed sequentially.
 */
internal class PdfCompositeTouchListener(private vararg val listeners: View.OnTouchListener) :
    View.OnTouchListener {
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        for (listener in listeners) {
            if (listener.onTouch(view, event)) {
                return true // Event consumed by this listener
            }
        }
        return false
    }
}
