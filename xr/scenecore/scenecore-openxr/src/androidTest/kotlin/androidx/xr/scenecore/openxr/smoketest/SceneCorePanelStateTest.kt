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

package androidx.xr.scenecore.openxr.smoketest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 29)
class SceneCorePanelStateTest {

    private val panel1 =
        SceneCorePanelSpec(
            id = 1,
            position = Vector3(1f, 2f, 3f),
            color = 0,
            sizeInPixels = IntSize2d(100, 100),
        )

    @Test
    fun pinPanel_addsAnchorAndAttachesPanelAtOrigin() {
        val anchor = SceneCoreAnchorSpec(id = 10, position = panel1.position)

        val updated = SceneCoreSceneSpec(panels = listOf(panel1)).pinPanel(panel1.id, anchor)

        assertThat(updated.anchors).containsExactly(anchor)
        assertThat(updated.panels.single().anchorId).isEqualTo(10)
        assertThat(updated.panels.single().position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun withNextParent_cyclesAcrossUsableAnchorsAndActivitySpace() {
        val anchor1 = SceneCoreAnchorSpec(id = 10, position = Vector3(1f, 0f, 0f))
        val anchor2 = SceneCoreAnchorSpec(id = 20, position = Vector3(2f, 0f, 0f))
        val anchor3 = SceneCoreAnchorSpec(id = 30, position = Vector3(3f, 0f, 0f))
        val failedIds = setOf(20)
        var scene =
            SceneCoreSceneSpec(panels = listOf(panel1), anchors = listOf(anchor1, anchor2, anchor3))

        scene = scene.withNextParent(panel1.id, failedIds)
        assertThat(scene.panels.single().anchorId).isEqualTo(10)
        assertThat(scene.panels.single().position).isEqualTo(Vector3.Zero)

        // Skips failed anchor 20 and moves to anchor 30.
        scene = scene.withNextParent(panel1.id, failedIds)
        assertThat(scene.panels.single().anchorId).isEqualTo(30)
        assertThat(scene.panels.single().position).isEqualTo(Vector3.Zero)

        // Wraps back to the activity space at DEFAULT_POSITION.
        scene = scene.withNextParent(panel1.id, failedIds)
        assertThat(scene.panels.single().anchorId).isNull()
        assertThat(scene.panels.single().position).isEqualTo(SceneCorePanelSpec.DEFAULT_POSITION)
    }

    @Test
    fun releasePanels_restoresPanelToAnchorCreationPositionPlusOffset() {
        val anchor = SceneCoreAnchorSpec(id = 10, position = Vector3(1f, 2f, 3f))
        val anchoredPanel = panel1.copy(position = Vector3(0.5f, -0.5f, 1f), anchorId = 10)
        val scene = SceneCoreSceneSpec(panels = listOf(anchoredPanel), anchors = listOf(anchor))

        val released = scene.releasePanels(setOf(10))

        assertThat(released.panels.single().anchorId).isNull()
        assertThat(released.panels.single().position).isEqualTo(Vector3(1.5f, 1.5f, 4f))
    }

    @Test
    fun withoutAnchor_removesAnchorAndReturnsItsPanelsToDefaultPosition() {
        val anchor = SceneCoreAnchorSpec(id = 10, position = Vector3(1f, 2f, 3f))
        val anchoredPanel = panel1.copy(position = Vector3.Zero, anchorId = 10)
        val scene = SceneCoreSceneSpec(panels = listOf(anchoredPanel), anchors = listOf(anchor))

        val updated = scene.withoutAnchor(10)

        assertThat(updated.anchors).isEmpty()
        assertThat(updated.panels.single().anchorId).isNull()
        assertThat(updated.panels.single().position).isEqualTo(SceneCorePanelSpec.DEFAULT_POSITION)
    }

    @Test
    fun failedAnchorIds_includesOnlyFailedAndDisposedAnchors() {
        val status =
            SceneCoreStatus(
                anchors =
                    mapOf(
                        1 to SceneCoreAnchorStatus(state = SceneCoreAnchorState.ANCHORED),
                        2 to SceneCoreAnchorStatus(state = SceneCoreAnchorState.ERROR),
                        3 to SceneCoreAnchorStatus(state = SceneCoreAnchorState.DISPOSED),
                        4 to SceneCoreAnchorStatus(state = SceneCoreAnchorState.FAILED),
                    )
            )

        assertThat(status.failedAnchorIds).containsExactly(3, 4)
    }
}
