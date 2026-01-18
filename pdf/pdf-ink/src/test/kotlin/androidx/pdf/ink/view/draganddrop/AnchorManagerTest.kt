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

package androidx.pdf.ink.view.draganddrop

import android.content.Context
import android.view.View
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnchorManagerTest {

    private lateinit var context: Context
    private lateinit var leftAnchor: View
    private lateinit var rightAnchor: View
    private lateinit var bottomAnchor: View
    private lateinit var anchorManager: AnchorManager

    private val ALPHA_ACTIVE = 0.8f
    private val ALPHA_INACTIVE = 0.3f

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        leftAnchor = View(context)
        rightAnchor = View(context)
        bottomAnchor = View(context)

        // Give anchors some size and position for distance calculations
        // Left: (0, 500), Right: (1000, 500), Bottom: (500, 1000)
        leftAnchor.layout(0, 450, 100, 550)
        rightAnchor.layout(900, 450, 1000, 550)
        bottomAnchor.layout(450, 950, 550, 1050)

        anchorManager = AnchorManager(leftAnchor, rightAnchor, bottomAnchor)
    }

    @Test
    fun showAnchors_makesAllAnchorsVisibleAndInactive() {
        anchorManager.showAnchors()

        assertThat(leftAnchor.visibility).isEqualTo(View.VISIBLE)
        assertThat(rightAnchor.visibility).isEqualTo(View.VISIBLE)
        assertThat(bottomAnchor.visibility).isEqualTo(View.VISIBLE)

        assertThat(leftAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
        assertThat(rightAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
        assertThat(bottomAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
    }

    @Test
    fun hideAnchors_makesAllAnchorsGone() {
        anchorManager.showAnchors() // First make them visible
        anchorManager.hideAnchors()

        assertThat(leftAnchor.visibility).isEqualTo(View.GONE)
        assertThat(rightAnchor.visibility).isEqualTo(View.GONE)
        assertThat(bottomAnchor.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun updateHighlighting_nearLeft_highlightsLeftAndReturnsStart() {
        // Position "toolbar" center near left anchor (50, 500)
        val state =
            anchorManager.updateHighlightingAndGetClosest(
                currentX = 0f,
                currentY = 450f,
                viewWidth = 100,
                viewHeight = 100,
            )

        assertThat(state).isEqualTo(DOCK_STATE_START)
        assertThat(leftAnchor.alpha).isEqualTo(ALPHA_ACTIVE)
        assertThat(rightAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
        assertThat(bottomAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
    }

    @Test
    fun updateHighlighting_nearBottom_highlightsBottomAndReturnsBottom() {
        // Position "toolbar" center near bottom anchor (500, 1000)
        val state =
            anchorManager.updateHighlightingAndGetClosest(
                currentX = 450f,
                currentY = 950f,
                viewWidth = 100,
                viewHeight = 100,
            )

        assertThat(state).isEqualTo(DOCK_STATE_BOTTOM)
        assertThat(bottomAnchor.alpha).isEqualTo(ALPHA_ACTIVE)
        assertThat(leftAnchor.alpha).isEqualTo(ALPHA_INACTIVE)
    }

    @Test
    fun getAnchorView_returnsCorrectViewForState() {
        assertThat(anchorManager.getAnchorView(DOCK_STATE_START)).isEqualTo(leftAnchor)
        assertThat(anchorManager.getAnchorView(DOCK_STATE_END)).isEqualTo(rightAnchor)
        assertThat(anchorManager.getAnchorView(DOCK_STATE_BOTTOM)).isEqualTo(bottomAnchor)
    }
}
