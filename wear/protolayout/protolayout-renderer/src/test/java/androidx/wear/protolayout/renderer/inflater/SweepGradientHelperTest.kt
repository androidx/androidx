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
package androidx.wear.protolayout.renderer.inflater

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Looper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl
import androidx.wear.protolayout.expression.pipeline.StateStore
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.colorProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.colorStop
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.degreesProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.fixedDynamicColor
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.fixedDynamicFloat
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.floatProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.prop
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.sweepGradient
import androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.CapPosition
import com.google.common.truth.Expect
import com.google.common.truth.Truth
import java.util.Optional
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SweepGradientHelperTest {
    @get:Rule val expect: Expect = Expect.create()

    private val testBounds: RectF = RectF(0f, 10f, 100f, 200f)

    private val stateStore = StateStore(emptyMap())

    private val dataPipeline: ProtoLayoutDynamicDataPipeline =
        ProtoLayoutDynamicDataPipeline(
            /* platformDataProviders= */ emptyMap(),
            stateStore,
            FixedQuotaManagerImpl(Int.MAX_VALUE),
            FixedQuotaManagerImpl(Int.MAX_VALUE),
        )

    @Test
    fun create_tooFewColors_throws() {
        val sweepProto = sweepGradient(colorStop(Color.RED, 0f))

        assertFailsWith<IllegalArgumentException> {
            SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun create_tooManyColors_throws() {
        val numColors = 50
        val sweepProto =
            sweepGradient() {
                for (i in 0..<numColors) {
                    addColorStops(colorStop(Color.RED + i, i.toFloat() / numColors))
                }
            }

        assertFailsWith<IllegalArgumentException> {
            SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}
        }
    }

    @Test
    fun create_missingOffsets_throws() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE),
                colorStop(Color.GREEN, 1f),
            )

        assertFailsWith<IllegalArgumentException> {
            SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}
        }
    }

    @Test
    fun getShader_invalidAngleSpan_throws() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE, 0.5f),
                colorStop(Color.GREEN, 1f),
            )
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        val startAngle = 10f
        val endAngle = startAngle + 400f

        assertFailsWith<IllegalArgumentException> {
            sgHelper.getShader(testBounds, startAngle, endAngle, 0f, CapPosition.NONE)
        }
    }

    @Test
    fun getColorAtSetOffsets() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE, 0.5f),
                colorStop(Color.GREEN, 1f),
            )
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED)
        expect.that(sgHelper.getColor(180f)).isEqualTo(Color.BLUE)
        expect.that(sgHelper.getColor(360f)).isEqualTo(Color.GREEN)
    }

    @Test
    fun unsortedStops_getColorAtSetOffsets() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.GREEN, 1f),
                colorStop(Color.BLUE, 0.5f),
            )
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED)
        expect.that(sgHelper.getColor(180f)).isEqualTo(Color.BLUE)
        expect.that(sgHelper.getColor(360f)).isEqualTo(Color.GREEN)
    }

    @Test
    fun getColor_customStartAndEndAngles() {
        val customStartAngle = 180f
        val customEndAngle = 720f
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE, 0.5f),
                colorStop(Color.GREEN, 1f),
            ) {
                startAngle = customStartAngle.degreesProp
                endAngle = customEndAngle.degreesProp
            }
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        // RED before and at startAngle.
        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED)
        expect.that(sgHelper.getColor(customStartAngle)).isEqualTo(Color.RED)
        // BLUE in the middle angle.
        expect
            .that(sgHelper.getColor((customStartAngle + customEndAngle) / 2f))
            .isEqualTo(Color.BLUE)
        // GREEN at the endAngle and after.
        expect.that(sgHelper.getColor(customEndAngle)).isEqualTo(Color.GREEN)
        expect.that(sgHelper.getColor(888f)).isEqualTo(Color.GREEN)
    }

    @Test
    fun getInterpolatedColor() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE, 0.5f),
                colorStop(Color.GREEN, 1f),
            )
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        val angle1 = 90f
        expect
            .that(sgHelper.getColor(angle1))
            .isEqualTo(sgHelper.interpolateColors(Color.RED, 0f, Color.BLUE, 180f, angle1))

        val angle2 = 213f
        expect
            .that(sgHelper.getColor(angle2))
            .isEqualTo(sgHelper.interpolateColors(Color.BLUE, 180f, Color.GREEN, 360f, angle2))
    }

    @Test
    fun getInterpolatedColor_noOffsets() {
        val sweepProto =
            sweepGradient(colorStop(Color.RED), colorStop(Color.BLUE), colorStop(Color.GREEN))

        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        val angle1 = 90f
        expect
            .that(sgHelper.getColor(angle1))
            .isEqualTo(sgHelper.interpolateColors(Color.RED, 0f, Color.BLUE, 180f, angle1))

        val angle2 = 213f
        expect
            .that(sgHelper.getColor(angle2))
            .isEqualTo(sgHelper.interpolateColors(Color.BLUE, 180f, Color.GREEN, 360f, angle2))
    }

    @Test
    fun getShader_shaderIsRotated() {
        val sweepProto =
            sweepGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.BLUE, 0.5f),
                colorStop(Color.GREEN, 1f),
            )
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}
        val rotationAngle = 63f
        val rotatedMatrix = Matrix()
        rotatedMatrix.postRotate(
            rotationAngle,
            (testBounds.left + testBounds.right) / 2f,
            (testBounds.top + testBounds.bottom) / 2f,
        )

        val generatedShader =
            sgHelper.getShader(testBounds, 180f, 360f, rotationAngle, CapPosition.NONE)
        Truth.assertThat(generatedShader).isInstanceOf(SweepGradient::class.java)
        val generatedMatrix = Matrix()
        generatedShader.getLocalMatrix(generatedMatrix)
        Truth.assertThat(rotatedMatrix).isEqualTo(generatedMatrix)
    }

    @Test
    fun getColor_colorSetToOpaque() {
        val color0 = 0x12666666
        val color90 = -0x2edcbaa
        val color180 = -0x9abcdf
        val noAlphaMask = 0x00FFFFFF
        val sweepProto =
            sweepGradient(colorStop(color0), colorStop(color90), colorStop(color180)) {
                endAngle = 180f.degreesProp
            }
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.empty()) {}

        val resolvedColor0 = sgHelper.getColor(0f)
        expect.that(Color.alpha(resolvedColor0)).isEqualTo(0xFF)
        expect.that(resolvedColor0 and noAlphaMask).isEqualTo(color0 and noAlphaMask)

        val resolvedColor90 = sgHelper.getColor(90f)
        expect.that(Color.alpha(resolvedColor90)).isEqualTo(0xFF)
        expect.that(resolvedColor90 and noAlphaMask).isEqualTo(color90 and noAlphaMask)

        val resolvedColor180 = sgHelper.getColor(180f)
        expect.that(Color.alpha(resolvedColor180)).isEqualTo(0xFF)
        expect.that(resolvedColor180 and noAlphaMask).isEqualTo(color180 and noAlphaMask)
    }

    @Test
    fun getColor_withDynamicColors() {
        val sweepProto =
            sweepGradient(
                colorStop(colorProp(Color.BLUE, fixedDynamicColor(Color.RED))),
                colorStop(colorProp(Color.BLUE, fixedDynamicColor(Color.GREEN))),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        expect.that(sgHelper.getColor(0f)).isEqualTo(Color.RED)
        expect.that(sgHelper.getColor(360f)).isEqualTo(Color.GREEN)
    }

    @Test
    fun getColor_withDynamicOffsets() {
        val offset0 = 0.5f
        val offset1 = 0.6f
        val color0 = Color.RED
        val color1 = Color.BLUE
        val sweepProto =
            sweepGradient(
                colorStop(colorProp(Color.GREEN), 0f.prop),
                colorStop(colorProp(color0), floatProp(0.1f, fixedDynamicFloat(offset0))),
                colorStop(colorProp(color1), floatProp(0.2f, fixedDynamicFloat(offset1))),
                colorStop(colorProp(Color.GREEN), 1f.prop),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        expect.that(sgHelper.getColor(offset0 * 360f)).isEqualTo(color0)
        expect.that(sgHelper.getColor(offset1 * 360f)).isEqualTo(color1)
    }

    @Test
    fun getColor_withDynamicEndAngleAndImplicitOffsets() {
        val endColor = Color.BLUE
        val sweepProto =
            sweepGradient(colorStop(colorProp(Color.RED)), colorStop(colorProp(endColor))) {
                endAngle = degreesProp(180f, fixedDynamicFloat(360f))
            }
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        expect.that(sgHelper.getColor(360f)).isEqualTo(endColor)
    }

    @Test
    fun getColor_withDynamicEndAngleAndExplicitOffsets() {
        val endColor = Color.BLUE
        val sweepProto =
            sweepGradient(
                colorStop(colorProp(Color.RED), 0.1f.prop),
                colorStop(colorProp(endColor), 1f.prop),
            ) {
                endAngle = degreesProp(155f, fixedDynamicFloat(499f))
            }
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        expect.that(sgHelper.getColor(499f)).isEqualTo(endColor)
    }

    @Test
    fun getColor_withDynamicStartAngleAndImplicitOffsets() {
        val startColor = Color.BLUE
        val sweepProto =
            sweepGradient(colorStop(colorProp(startColor)), colorStop(colorProp(Color.RED))) {
                startAngle = degreesProp(0f, fixedDynamicFloat(100f))
            }
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)
        shadowOf(Looper.getMainLooper()).idle()

        expect.that(sgHelper.getColor(100f)).isEqualTo(startColor)
    }

    @Test
    fun getColor_withDynamicStartAngleAndExplicitOffsets() {
        val startColor = Color.BLUE
        val startOffset = 0.1f
        val startAngleValue = 100f
        val sweepProto =
            sweepGradient(
                colorStop(colorProp(startColor), startOffset.prop),
                colorStop(colorProp(Color.RED), 1f.prop),
            ) {
                startAngle = degreesProp(0f, fixedDynamicFloat(startAngleValue))
            }
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val sgHelper = SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)
        shadowOf(Looper.getMainLooper()).idle()

        expect.that(sgHelper.getColor(startOffset * startAngleValue)).isEqualTo(startColor)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun update_callsInvalidateCallback() {
        val sweepProto =
            sweepGradient(
                colorStop(colorProp(Color.GREEN, fixedDynamicColor(Color.RED))),
                colorStop(colorProp(Color.BLUE)),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        var invalidateCount = 0
        val unused =
            SweepGradientHelper.create(sweepProto, POS_ID, Optional.of(pipelineMaker)) {
                invalidateCount++
            }
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)
        shadowOf(Looper.getMainLooper()).idle()

        expect.that(invalidateCount).isEqualTo(2) // 1 for the initialization, 1 for the update.
    }

    private companion object {
        const val POS_ID = "1.2.3.4"
    }
}
