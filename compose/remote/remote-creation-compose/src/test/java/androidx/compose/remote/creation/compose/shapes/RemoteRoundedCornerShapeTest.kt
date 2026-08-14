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

package androidx.compose.remote.creation.compose.shapes

import android.graphics.Bitmap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteRoundedCornerShapeTest {
    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    private fun createRemoteDrawScope(
        width: Int = 100,
        height: Int = 50,
        fakeBuffer: TestRemoteComposeBuffer,
    ): Pair<RemoteDrawScope, RecordingCanvas> {
        val platform = AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
        val creationState =
            RemoteComposeCreationState(
                RemoteCreationDisplayInfo(width, height, 160, 1f),
                null,
                profile,
            )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        val remoteCanvas = RemoteCanvas(recordingCanvas)
        return RemoteDrawScope(remoteCanvas) to recordingCanvas
    }

    @Test
    fun copy_preservesValuesIfNotSpecified() {
        val shape = RemoteRoundedCornerShape(1.rdp, 2.rdp, 3.rdp, 4.rdp)
        val copied = shape.copy()

        assertTrue(haveSameInstances(shape, copied))
    }

    @Test
    fun copy_updatesSpecifiedValues() {
        val topEnd = 2.rdp
        val bottomEnd = 3.rdp
        val bottomStart = 4.rdp
        val shape = RemoteRoundedCornerShape(1.rdp, topEnd, bottomEnd, bottomStart)
        val topStartOverride = 10.rdp
        val copied = shape.copy(topStart = RemoteCornerSize(topStartOverride))

        assertEquals(topStartOverride, (copied.topStart as? RemoteDpCornerSize)?.size)

        assertEquals(topEnd, (copied.topEnd as? RemoteDpCornerSize)?.size)
        assertEquals(bottomEnd, (copied.bottomEnd as? RemoteDpCornerSize)?.size)
        assertEquals(bottomStart, (copied.bottomStart as? RemoteDpCornerSize)?.size)
    }

    @Test
    fun createOutline_uniformCorners() {
        val shape = RemoteRoundedCornerShape(10.rdp)
        val density = RemoteDensity(2f.rf, 1f.rf)
        val outline = shape.createOutline(RemoteSize(100f.rf, 50f.rf), density, LayoutDirection.Ltr)

        assertTrue(outline is RemoteOutline.Rounded)
        val rounded = outline as RemoteOutline.Rounded
        assertEquals(20f, rounded.topStart.constantValue)
        assertEquals(20f, rounded.topEnd.constantValue)
        assertEquals(20f, rounded.bottomEnd.constantValue)
        assertEquals(20f, rounded.bottomStart.constantValue)
    }

    @Test
    fun createOutline_withStrokeWidth() {
        val shape = RemoteRoundedCornerShape(10.rdp)
        val density = RemoteDensity(2f.rf, 1f.rf)
        val outline =
            shape.createOutline(
                size = RemoteSize(100f.rf, 50f.rf),
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                strokeWidth = 10f.rf,
            )

        assertTrue(outline is RemoteOutline.Rounded)
        val rounded = outline as RemoteOutline.Rounded
        assertEquals(5f, rounded.offset.x.constantValue)
        assertEquals(5f, rounded.offset.y.constantValue)
        assertEquals(90f, rounded.size?.width?.constantValue)
        assertEquals(40f, rounded.size?.height?.constantValue)
        assertEquals(15f, rounded.topStart.constantValue)
    }

    @Test
    fun drawOutline_rounded_withOffsetAndNullSize() {
        val fakeBuffer = TestRemoteComposeBuffer()
        val (drawScope, recordingCanvas) =
            createRemoteDrawScope(width = 100, height = 50, fakeBuffer = fakeBuffer)
        val outline =
            RemoteOutline.Rounded(
                topStart = 10f.rf,
                topEnd = 10f.rf,
                bottomEnd = 10f.rf,
                bottomStart = 10f.rf,
                offset = RemoteOffset(10f.rf, 20f.rf),
                size = null,
            )

        with(outline) { drawScope.drawOutline(RemotePaint()) }
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addComponentValue(42, 0)",
                "addComponentValue(43, 1)",
                "addPaint",
                "addAnimatedFloat(44) = ([42] 10.0 + )",
                "addAnimatedFloat(45) = ([43] 20.0 + )",
                "addDrawRoundRect(10.0, 20.0, ID(44), ID(45), 10.0, 10.0)",
            )
    }

    @Test
    fun drawOutline_rounded_withZeroOffsetAndNullSize() {
        val fakeBuffer = TestRemoteComposeBuffer()
        val (drawScope, recordingCanvas) =
            createRemoteDrawScope(width = 100, height = 50, fakeBuffer = fakeBuffer)
        val outline =
            RemoteOutline.Rounded(
                topStart = 10f.rf,
                topEnd = 10f.rf,
                bottomEnd = 10f.rf,
                bottomStart = 10f.rf,
                offset = RemoteOffset.Zero,
                size = null,
            )

        with(outline) { drawScope.drawOutline(RemotePaint()) }
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addComponentValue(42, 0)",
                "addComponentValue(43, 1)",
                "addPaint",
                "addDrawRoundRect(0.0, 0.0, ID(42), ID(43), 10.0, 10.0)",
            )
    }

    @Test
    fun drawOutline_rounded_withOffsetAndSize() {
        val fakeBuffer = TestRemoteComposeBuffer()
        val (drawScope, recordingCanvas) =
            createRemoteDrawScope(width = 100, height = 50, fakeBuffer = fakeBuffer)
        val outline =
            RemoteOutline.Rounded(
                topStart = 10f.rf,
                topEnd = 10f.rf,
                bottomEnd = 10f.rf,
                bottomStart = 10f.rf,
                offset = RemoteOffset(5f.rf, 5f.rf),
                size = RemoteSize(90f.rf, 40f.rf),
            )

        with(outline) { drawScope.drawOutline(RemotePaint()) }
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "addDrawRoundRect(5.0, 5.0, 95.0, 45.0, 10.0, 10.0)")
    }

    private fun haveSameInstances(
        shape1: RemoteCornerBasedShape,
        shape2: RemoteCornerBasedShape,
    ): Boolean {

        return shape1.topStart === shape2.topStart &&
            shape1.topEnd === shape2.topEnd &&
            shape1.bottomEnd === shape2.bottomEnd &&
            shape1.bottomStart === shape2.bottomStart
    }
}
