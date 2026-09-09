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

package androidx.compose.remote.creation.compose.vector

import android.content.Context
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.R
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteAnimatedVectorPainterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun parseAllAvdSamples_successfullyLoaded() {
        val samples =
            listOf(
                R.drawable.small_animated_vector,
                R.drawable.target_duplicated,
                R.drawable.avd_complex,
                R.drawable.ic_hourglass_animated,
                R.drawable.avd_heart_fill,
                R.drawable.avd_heart_empty,
                R.drawable.open_on_phone_animation,
                R.drawable.wear_one_handed_gesture_primary_indicator_animation,
            )

        for (sampleResId in samples) {
            val avd = RemoteAnimatedVector.fromXml(context.resources, sampleResId)
            assertThat(avd).isNotNull()
            assertThat(avd.width).isGreaterThan(0f)
            assertThat(avd.height).isGreaterThan(0f)
            assertThat(avd.viewportWidth).isGreaterThan(0f)
            assertThat(avd.viewportHeight).isGreaterThan(0f)
            assertThat(avd.targets).isNotEmpty()
        }
    }

    @Test
    fun evaluate_targetDuplicated_strokeColorAndWidthInterpolation() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.target_duplicated)

        // Initial state at progress = 0
        val snap0 = avd.evaluate(0f)
        val path0 = snap0.rootGroup.children.filterIsInstance<RemoteEvaluatedPath>().first()
        assertThat(path0.strokeColor).isEqualTo(0xFFFF0000.toInt())
        assertThat(path0.strokeWidth).isEqualTo(1f)

        // Intermediate state at progress = 0.5
        val snapMid = avd.evaluate(0.5f)
        val pathMid = snapMid.rootGroup.children.filterIsInstance<RemoteEvaluatedPath>().first()
        assertThat(pathMid.strokeWidth).isGreaterThan(1f)
        assertThat(pathMid.strokeWidth).isLessThan(4f)

        // Final state at progress = 1.0
        val snap1 = avd.evaluate(1f)
        val path1 = snap1.rootGroup.children.filterIsInstance<RemoteEvaluatedPath>().first()
        assertThat(path1.strokeColor).isEqualTo(0xFF0000FF.toInt())
        assertThat(path1.strokeWidth).isEqualTo(4f)
    }

    @Test
    fun evaluate_avdComplex_fillColorInterpolation() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.avd_complex)

        val snap0 = avd.evaluate(0f)
        val path0 = snap0.rootGroup.children.filterIsInstance<RemoteEvaluatedPath>().first()
        assertThat(path0.fillColor).isEqualTo(0xFFFF1744.toInt())

        val snap1 = avd.evaluate(1f)
        val path1 = snap1.rootGroup.children.filterIsInstance<RemoteEvaluatedPath>().first()
        assertThat(path1.fillColor).isEqualTo(0xFF00E5FF.toInt())
    }

    @Test
    fun evaluate_icHourglassAnimated_rotationAndClipMorphing() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.ic_hourglass_animated)

        val snap0 = avd.evaluate(0f)
        val frameGroup0 =
            snap0.rootGroup.children.filterIsInstance<RemoteEvaluatedGroup>().first {
                it.name == "hourglass_frame"
            }
        assertThat(frameGroup0.rotation).isEqualTo(0f)

        val snap1 = avd.evaluate(1f)
        val frameGroup1 =
            snap1.rootGroup.children.filterIsInstance<RemoteEvaluatedGroup>().first {
                it.name == "hourglass_frame"
            }
        assertThat(frameGroup1.rotation).isEqualTo(180f)

        // Clip path for mask_sand should morph between progress 0.333 and 1.0
        val fillOutlines0 =
            snap0.rootGroup.children.filterIsInstance<RemoteEvaluatedGroup>().first {
                it.name == "fill_outlines"
            }
        val pivotGroup0 = fillOutlines0.children.filterIsInstance<RemoteEvaluatedGroup>().first()

        val fillOutlines1 =
            snap1.rootGroup.children.filterIsInstance<RemoteEvaluatedGroup>().first {
                it.name == "fill_outlines"
            }
        val pivotGroup1 = fillOutlines1.children.filterIsInstance<RemoteEvaluatedGroup>().first()

        assertThat(pivotGroup0.clipPathData).isNotEmpty()
        assertThat(pivotGroup1.clipPathData).isNotEmpty()
        assertThat(pivotGroup0.clipPathData).isNotEqualTo(pivotGroup1.clipPathData)
    }

    @Test
    fun toImageVector_createsValidComposeImageVector() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.ic_hourglass_animated)
        val snapshot = avd.evaluate(0.5f)
        val imageVector = snapshot.toImageVector()

        assertThat(imageVector.name).isEqualTo(avd.name)
        assertThat(imageVector.viewportWidth).isEqualTo(avd.viewportWidth)
        assertThat(imageVector.viewportHeight).isEqualTo(avd.viewportHeight)
        assertThat(imageVector.root.size).isEqualTo(snapshot.rootGroup.children.size)
    }

    @Test
    fun painterRemoteAnimatedVector_withDefaultProgress_createsPainterWithIntrinsicSize() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.target_duplicated)

        val painterDefault = painterRemoteAnimatedVector(avd)
        val intrinsicSizeDefault = requireNotNull(painterDefault.intrinsicSize)
        assertThat(intrinsicSizeDefault.width.constantValueOrNull).isEqualTo(avd.width)
        assertThat(intrinsicSizeDefault.height.constantValueOrNull).isEqualTo(avd.height)

        val painterRemoteFloat = painterRemoteAnimatedVector(avd, progress = 0.5f.rf)
        val intrinsicSizeRemoteFloat = requireNotNull(painterRemoteFloat.intrinsicSize)
        assertThat(intrinsicSizeRemoteFloat.width.constantValueOrNull).isEqualTo(avd.width)
        assertThat(intrinsicSizeRemoteFloat.height.constantValueOrNull).isEqualTo(avd.height)
    }

    @Test
    fun painterRemoteAnimatedVector_fromResId_createsPainter() {
        val painter =
            painterRemoteAnimatedVector(
                context,
                R.drawable.small_animated_vector,
                progress = 0.5f.rf,
            )
        val intrinsicSize = requireNotNull(painter.intrinsicSize)
        assertThat(intrinsicSize.width.constantValueOrNull).isEqualTo(960f)
        assertThat(intrinsicSize.height.constantValueOrNull).isEqualTo(960f)
    }

    @Test
    fun defaultProgress_animatesContinuallyBasedOnAnimationTime() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.small_animated_vector)
        val painter = RemoteAnimatedVectorPainter(avd)

        assertThat(painter.progress.hasConstantValue).isFalse()
    }

    @Test
    fun multiStagePathMorphing_withDynamicProgress_rendersChainedSegments() {
        val avd =
            RemoteAnimatedVector.fromXml(
                context.resources,
                R.drawable.wear_one_handed_gesture_primary_indicator_animation,
            )
        val painter = RemoteAnimatedVectorPainter(avd)
        assertThat(painter).isNotNull()
        assertThat(painter.intrinsicSize.width.constantValueOrNull).isEqualTo(36f)
        assertThat(painter.intrinsicSize.height.constantValueOrNull).isEqualTo(36f)

        // Verify that the multi-stage path is deconstructed into a group with 4 chained segments
        val root = painter.root
        assertThat(root.numChildren).isGreaterThan(0)
        val rgGroup = root[0] as RemoteGroupComponent
        val n1t0Group = rgGroup[0] as RemoteGroupComponent
        val l0Group = n1t0Group[0] as RemoteGroupComponent
        val pathGroup = l0Group[0] as RemoteGroupComponent
        assertThat(pathGroup.name).isEqualTo("_R_G_L_0_G_D_0_P_0")
        // Wear animation has 4 pathData objectAnimators chained together
        assertThat(pathGroup.numChildren).isEqualTo(4)

        for (i in 0 until 4) {
            val seg = pathGroup[i] as RemotePathComponent
            assertThat(seg.name).isEqualTo("_R_G_L_0_G_D_0_P_0_seg$i")
            assertThat(seg.pathData).isNotEmpty()
            assertThat(seg.targetPathData).isNotNull()
            assertThat(seg.targetPathData).isNotEmpty()
            assertThat(seg.pathTween.hasConstantValue).isFalse()
            assertThat(seg.strokeAlpha.hasConstantValue).isFalse()
        }
    }

    @Test
    fun threeStagePathMorphing_withDynamicProgress_rendersThreeChainedSegments() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.avd_three_segments)
        val painter = RemoteAnimatedVectorPainter(avd)
        assertThat(painter).isNotNull()
        assertThat(painter.intrinsicSize.width.constantValueOrNull).isEqualTo(48f)
        assertThat(painter.intrinsicSize.height.constantValueOrNull).isEqualTo(48f)
        assertThat(avd.totalDuration).isEqualTo(300)

        val root = painter.root
        assertThat(root.numChildren).isEqualTo(1)
        val rootGroup = root[0] as RemoteGroupComponent
        assertThat(rootGroup.name).isEqualTo("root_group")

        val pathGroup = rootGroup[0] as RemoteGroupComponent
        assertThat(pathGroup.name).isEqualTo("shape_path")
        assertThat(pathGroup.numChildren).isEqualTo(3)

        for (i in 0 until 3) {
            val seg = pathGroup[i] as RemotePathComponent
            assertThat(seg.name).isEqualTo("shape_path_seg$i")
            assertThat(seg.pathData).isNotEmpty()
            assertThat(seg.targetPathData).isNotNull()
            assertThat(seg.targetPathData).isNotEmpty()
            assertThat(seg.pathTween.hasConstantValue).isFalse()
            assertThat(seg.strokeAlpha.hasConstantValue).isFalse()
            assertThat(seg.fillAlpha.hasConstantValue).isFalse()
        }
    }

    @Test
    fun threeStagePathMorphing_withConstantProgress_stillGeneratesDynamicChainedSegments() {
        val avd = RemoteAnimatedVector.fromXml(context.resources, R.drawable.avd_three_segments)
        val painter = RemoteAnimatedVectorPainter(avd, 0.5f.rf)
        val root = painter.root[0] as RemoteGroupComponent
        val pathGroup = root[0] as RemoteGroupComponent
        assertThat(pathGroup.numChildren).isEqualTo(3)
        for (i in 0 until 3) {
            val seg = pathGroup[i] as RemotePathComponent
            assertThat(seg.name).isEqualTo("shape_path_seg$i")
            assertThat(seg.pathData).isNotEmpty()
        }
        val seg0 = pathGroup[0] as RemotePathComponent
        assertThat(seg0.pathTween.constantValueOrNull).isEqualTo(1f)
        val seg1 = pathGroup[1] as RemotePathComponent
        assertThat(seg1.pathTween.constantValueOrNull).isWithin(0.01f).of(0.5f)
        val seg2 = pathGroup[2] as RemotePathComponent
        assertThat(seg2.pathTween.constantValueOrNull).isEqualTo(0f)
    }
}
