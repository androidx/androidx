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

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import javax.swing.JPanel
import org.junit.Test

class MouseApplicationTest {

    // TODO: check why useDelay is needed here. Rendering should be dispatched in between the
    //   100 yield calls in the `awaitIdle`
    @Test
    fun `interop in lazy list`() = runApplicationTest(useDelay = true) {
        lateinit var density: Density

        val currentlyVisible = mutableSetOf<Int>()
        val scrollState = LazyListState()

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 250.dp, height = 250.dp)
            ) {
                density = LocalDensity.current

                LazyColumn(Modifier.fillMaxSize(), scrollState) {
                    items(100) { index ->
                        SwingPanel(
                            factory = {
                                object : JPanel() {
                                    override fun addNotify() {
                                        super.addNotify()
                                        currentlyVisible += index
                                    }

                                    override fun removeNotify() {
                                        super.removeNotify()
                                        currentlyVisible -= index
                                    }
                                }
                            },
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            }
        }

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(0, 1, 2)

        // Use [dispatchRawDelta] instead of [window.sendMouseWheelEvent] because
        // [wheelRotation] is platform dependant (multiplied by dynamic internal coefficient)
        scrollState.dispatchRawDelta(with(density) { 1000.dp.toPx() })

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(10, 11, 12)

        scrollState.dispatchRawDelta(with(density) { -1000.dp.toPx() })

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(0, 1, 2)
    }
}
