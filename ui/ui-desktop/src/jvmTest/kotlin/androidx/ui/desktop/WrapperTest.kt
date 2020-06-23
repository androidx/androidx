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

package androidx.ui.desktop

import androidx.ui.foundation.Text

import java.awt.event.WindowEvent
import javax.swing.WindowConstants

import org.junit.Test

class WrapperTest {
    @Test
    fun wrapWindow() {
        val frame = SkiaWindow(width = 640, height = 480)

        frame.title = "Test"
        frame.setLocation(400, 400)
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        frame.setContent {
            Text("Simple")
        }
        frame.setVisible(true)

        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
    }
}