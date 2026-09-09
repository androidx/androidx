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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec.ANIMATION
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.modifiers.AnimateSpecModifier as CreationAnimateSpecModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnimateSpecModifierTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var fakeBuffer: TestRemoteComposeBuffer

    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    @Before
    fun setUp() {
        fakeBuffer = TestRemoteComposeBuffer()

        val platform = AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                _ ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
    }

    @Test
    fun testSharedElementDefault() {
        val modifier = RemoteModifier.sharedElement(key = 42)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(42, 300.0, 1, 300.0, 1, 0, 1)")
    }

    @Test
    fun testSharedElementCustomSpec() {
        val modifier =
            RemoteModifier.sharedElement(
                key = 99,
                spec = remoteTween(durationMillis = 600, easing = RemoteEasing.Decelerate),
                enter = RemoteEnterTransition.SlideInTop,
                exit = RemoteExitTransition.SlideOutBottom,
            )
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(99, 600.0, 3, 600.0, 3, 4, 5)")
    }

    @Test
    fun testSharedBounds() {
        val modifier = RemoteModifier.sharedBounds(key = 77)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(77, 300.0, 1, 300.0, 1, 0, 1)")
    }

    @Test
    fun testAnimateEnterExit() {
        val modifier =
            RemoteModifier.animateEnterExit(
                enter = RemoteEnterTransition.SlideInLeft,
                exit = RemoteExitTransition.SlideOutRight,
                spec = remoteTween(durationMillis = 400),
            )
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        // animateEnterExit uses animationId = 0 (pure enter/exit visibility transition)
        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(0, 400.0, 1, 400.0, 1, 2, 3)")
    }

    @Test
    fun testAnimationSpecWithRemoteTweenSpec() {
        val modifier =
            RemoteModifier.animationSpec(
                animationId = 12,
                motionSpec = remoteTween(durationMillis = 500, easing = RemoteEasing.Decelerate),
                enter = RemoteEnterTransition.Rotate,
                exit = RemoteExitTransition.Particle,
            )
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(12, 500.0, 3, 500.0, 3, 6, 7)")
    }

    @Test
    fun testAnimationSpecDefault() {
        val modifier = RemoteModifier.animationSpec()
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(-1, 300.0, 1, 300.0, 1, 0, 1)")
    }

    @Test
    fun testAnimationSpecIdOnly() {
        val modifier = RemoteModifier.animationSpec(100)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(100, 300.0, 1, 300.0, 1, 0, 1)")
    }

    @Test
    fun testAnimationSpecIdAndEnabled() {
        val modifierEnabled = RemoteModifier.animationSpec(100, true)
        val recEnabled = creationState.toRecordingModifier(modifierEnabled)
        (recEnabled.list[0] as CreationAnimateSpecModifier).write(creationState.document)

        val modifierDisabled = RemoteModifier.animationSpec(100, false)
        val recDisabled = creationState.toRecordingModifier(modifierDisabled)
        (recDisabled.list[0] as CreationAnimateSpecModifier).write(creationState.document)

        val modifierDisabledDefaultId = RemoteModifier.animationSpec(enabled = false)
        val recDisabledDefaultId = creationState.toRecordingModifier(modifierDisabledDefaultId)
        (recDisabledDefaultId.list[0] as CreationAnimateSpecModifier).write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addAnimationSpecModifier(100, 300.0, 1, 300.0, 1, 0, 1)",
                "addAnimationSpecModifier(0, 300.0, 1, 300.0, 1, 0, 1)",
                "addAnimationSpecModifier(0, 300.0, 1, 300.0, 1, 0, 1)",
            )
            .inOrder()
    }

    @Test
    fun testAnimationSpecLegacyExplicitDuration() {
        val modifier = RemoteModifier.animationSpec(animationId = 5, motionDuration = 250f)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0] as CreationAnimateSpecModifier
        element.write(creationState.document)

        assertThat(fakeBuffer.calls)
            .containsExactly("addAnimationSpecModifier(5, 250.0, 1, 250.0, 1, 0, 1)")
    }

    @Test
    fun testTransitionEquivalenceAndHelpers() {
        assertThat(remoteFadeIn()).isEqualTo(RemoteEnterTransition.FadeIn)
        assertThat(remoteFadeOut()).isEqualTo(RemoteExitTransition.FadeOut)
        assertThat(RemoteEnterTransition.FadeIn.hashCode())
            .isEqualTo(RemoteEnterTransition(ANIMATION.FADE_IN).hashCode())
        assertThat(RemoteEnterTransition.FadeIn).isEqualTo(RemoteEnterTransition(ANIMATION.FADE_IN))
        assertThat(RemoteExitTransition.FadeOut.hashCode())
            .isEqualTo(RemoteExitTransition(ANIMATION.FADE_OUT).hashCode())
        assertThat(RemoteExitTransition.FadeOut).isEqualTo(RemoteExitTransition(ANIMATION.FADE_OUT))
        assertThat(RemoteEnterTransition.SlideInLeft.toString())
            .isEqualTo("RemoteEnterTransition.SLIDE_LEFT")
        assertThat(RemoteExitTransition.SlideOutRight.toString())
            .isEqualTo("RemoteExitTransition.SLIDE_RIGHT")
    }
}
