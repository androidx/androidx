/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.runApplicationTest
import java.awt.Point
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals

class SwingPanelTest {
    /**
     * Test the positioning of a [SwingPanel] with offset.
     * See https://github.com/JetBrains/compose-multiplatform/issues/4005
     */
    @Test
    fun swingPanelWithOffset() = runApplicationTest {
        lateinit var panel: JPanel
        launchTestApplication {
            Window(
                onCloseRequest = {}
            ) {
                SwingPanel(
                    modifier = Modifier.size(100.dp).offset(50.dp, 50.dp),
                    factory = {
                        JPanel().also {
                            panel = it
                        }
                    }
                )
            }
        }
        awaitIdle()

        val locationInRootPane =
            SwingUtilities.convertPoint(panel, Point(0, 0), SwingUtilities.getRootPane(panel))
        assertEquals(expected = Point(50, 50), locationInRootPane)
    }
}