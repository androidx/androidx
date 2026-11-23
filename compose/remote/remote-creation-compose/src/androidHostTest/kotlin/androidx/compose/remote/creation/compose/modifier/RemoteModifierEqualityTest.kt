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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteModifierEqualityTest {

    private fun assertStructurallyEqual(a: Any, b: Any) {
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun paddingAndOffsetModifiers_structuralEquality() {
        assertStructurallyEqual(RemoteModifier.padding(16.rdp), RemoteModifier.padding(16.rdp))
        assertStructurallyEqual(RemoteModifier.padding(16f.rf), RemoteModifier.padding(16f.rf))
        assertThat(RemoteModifier.padding(16.rdp)).isNotEqualTo(RemoteModifier.padding(8.rdp))

        assertStructurallyEqual(
            RemoteModifier.offset(x = 4.rdp, y = 8.rdp),
            RemoteModifier.offset(x = 4.rdp, y = 8.rdp),
        )
        assertThat(RemoteModifier.offset(x = 4.rdp, y = 8.rdp))
            .isNotEqualTo(RemoteModifier.offset(x = 4.rdp, y = 12.rdp))
    }

    @Test
    fun sizeAndConstraintModifiers_structuralEquality() {
        assertStructurallyEqual(
            RemoteModifier.size(48.rdp, 32.rdp),
            RemoteModifier.size(48.rdp, 32.rdp),
        )
        assertStructurallyEqual(
            RemoteModifier.fillMaxSize(0.5f.rf),
            RemoteModifier.fillMaxSize(0.5f.rf),
        )
        assertStructurallyEqual(RemoteModifier.wrapContentSize(), RemoteModifier.wrapContentSize())
        assertStructurallyEqual(
            RemoteModifier.widthIn(min = 10.rdp, max = 100.rdp),
            RemoteModifier.widthIn(min = 10.rdp, max = 100.rdp),
        )
        assertStructurallyEqual(
            RemoteModifier.heightIn(min = 10.rdp, max = 100.rdp),
            RemoteModifier.heightIn(min = 10.rdp, max = 100.rdp),
        )
        assertStructurallyEqual(
            RemoteModifier.defaultMinSize(minWidth = 24.rdp, minHeight = 24.rdp),
            RemoteModifier.defaultMinSize(minWidth = 24.rdp, minHeight = 24.rdp),
        )
        assertThat(RemoteModifier.width(10.rdp)).isNotEqualTo(RemoteModifier.width(20.rdp))
    }

    @Test
    fun visualModifiers_structuralEquality() {
        val color = RemoteColor(0xFF112233.toInt())
        assertStructurallyEqual(RemoteModifier.background(color), RemoteModifier.background(color))
        assertStructurallyEqual(
            RemoteModifier.background(painterRemoteColor(color)),
            RemoteModifier.background(painterRemoteColor(color)),
        )
        assertThat(RemoteModifier.background(painterRemoteColor(color)))
            .isNotEqualTo(
                RemoteModifier.background(painterRemoteColor(RemoteColor(0xFF445566.toInt())))
            )
        assertStructurallyEqual(
            RemoteModifier.background(RemoteBrush.solidColor(color)),
            RemoteModifier.background(RemoteBrush.solidColor(color)),
        )
        assertStructurallyEqual(
            RemoteModifier.border(
                width = 2.rdp,
                color = RemoteColor(0xFF000000.toInt()),
                shape = RemoteRoundedCornerShape(8.rdp),
            ),
            RemoteModifier.border(
                width = 2.rdp,
                color = RemoteColor(0xFF000000.toInt()),
                shape = RemoteRoundedCornerShape(8.rdp),
            ),
        )
        assertStructurallyEqual(
            RemoteModifier.clip(RemoteCircleShape),
            RemoteModifier.clip(RemoteCircleShape),
        )
        assertStructurallyEqual(RemoteModifier.alpha(0.5f.rf), RemoteModifier.alpha(0.5f.rf))
        assertStructurallyEqual(RemoteModifier.rotate(45f.rf), RemoteModifier.rotate(45f.rf))
        assertStructurallyEqual(RemoteModifier.scale(2f.rf), RemoteModifier.scale(2f.rf))
        assertStructurallyEqual(RemoteModifier.zIndex(3f.rf), RemoteModifier.zIndex(3f.rf))
        assertStructurallyEqual(RemoteModifier.visibility(1.ri), RemoteModifier.visibility(1.ri))
        assertStructurallyEqual(
            RemoteModifier.graphicsLayer(
                alpha = 0.8f.rf,
                renderEffect = RemoteBlurEffect(8f, 12f, RemoteTileMode.Mirror),
            ),
            RemoteModifier.graphicsLayer(
                alpha = 0.8f.rf,
                renderEffect = RemoteBlurEffect(8f, 12f, RemoteTileMode.Mirror),
            ),
        )
        assertThat(RemoteModifier.graphicsLayer(renderEffect = RemoteBlurEffect(8f, 12f)))
            .isNotEqualTo(RemoteModifier.graphicsLayer(renderEffect = RemoteBlurEffect(4f, 12f)))
    }

    @Test
    fun actionAndTouchModifiers_structuralEquality() {
        val state = MutableRemoteInt(0)
        assertStructurallyEqual(
            RemoteModifier.clickable(
                onClick = valueChangeAction(state, 1.ri),
                role = RemoteRole.Button,
            ),
            RemoteModifier.clickable(
                onClick = valueChangeAction(state, 1.ri),
                role = RemoteRole.Button,
            ),
        )
        assertStructurallyEqual(
            RemoteModifier.clickable(
                onClick = combinedAction(hostAction("tap".rs), hostAction(id = 42))
            ),
            RemoteModifier.clickable(
                onClick = combinedAction(hostAction("tap".rs), hostAction(id = 42))
            ),
        )
        assertThat(RemoteModifier.clickable(hostAction("a".rs)))
            .isNotEqualTo(RemoteModifier.clickable(hostAction("b".rs)))

        assertStructurallyEqual(
            RemoteModifier.onTouchDown(hostAction("down".rs)),
            RemoteModifier.onTouchDown(hostAction("down".rs)),
        )
        assertStructurallyEqual(
            RemoteModifier.onTouchUp(hostAction("up".rs)),
            RemoteModifier.onTouchUp(hostAction("up".rs)),
        )
        assertStructurallyEqual(
            RemoteModifier.onTouchCancel(hostAction("cancel".rs)),
            RemoteModifier.onTouchCancel(hostAction("cancel".rs)),
        )
    }

    @Test
    fun animationScrollAndLayoutElementModifiers_structuralEquality() {
        assertStructurallyEqual(
            RemoteModifier.sharedElement(key = 7, spec = remoteTween(250)),
            RemoteModifier.sharedElement(key = 7, spec = remoteTween(250)),
        )
        assertStructurallyEqual(
            RemoteModifier.animateEnterExit(animationId = 3),
            RemoteModifier.animateEnterExit(animationId = 3),
        )
        assertStructurallyEqual(
            RemoteModifier.animationSpec(animationId = 5, enabled = true),
            RemoteModifier.animationSpec(animationId = 5, enabled = true),
        )
        assertThat(RemoteModifier.animationSpec(animationId = 5, enabled = true))
            .isNotEqualTo(RemoteModifier.animationSpec(animationId = 5, enabled = false))

        assertStructurallyEqual(AlignByBaselineModifier, AlignByBaselineModifier)
        assertStructurallyEqual(
            CollapsiblePriorityModifier(orientation = 1, priority = 2f.rf),
            CollapsiblePriorityModifier(orientation = 1, priority = 2f.rf),
        )

        val scrollState = RemoteScrollState(notches = 4)
        assertStructurallyEqual(
            RemoteModifier.verticalScroll(scrollState),
            RemoteModifier.verticalScroll(scrollState),
        )
        assertStructurallyEqual(
            RemoteModifier.horizontalScroll(scrollState),
            RemoteModifier.horizontalScroll(scrollState),
        )
    }

    @Test
    fun chainedModifiersAndDynamicPaddingValues_structuralEquality() {
        val chainA = RemoteModifier.padding(8.rdp).clip(RemoteCircleShape).alpha(0.5f.rf)
        val chainB = RemoteModifier.padding(8.rdp).clip(RemoteCircleShape).alpha(0.5f.rf)
        assertStructurallyEqual(chainA, chainB)

        val dynamicDp = RemoteDp(RemoteFloat.createNamedRemoteFloat("pad", 4f))
        val paddingValues = RemotePaddingValues(all = dynamicDp)
        assertThat(paddingValues).isEqualTo(paddingValues)
    }
}
