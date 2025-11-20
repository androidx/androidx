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

import android.app.Activity
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl
import androidx.wear.protolayout.expression.pipeline.StateStore
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.colorProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.colorStop
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.dpProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.fixedDynamicColor
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.fixedDynamicFloat
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.floatProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.linearGradient
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.prop
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.stateDynamicColor
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.stateDynamicFloat
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.toOffset
import com.google.common.truth.Truth.assertThat
import java.util.Optional
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class LinearGradientHelperTest {
    private val view = View(getApplicationContext())

    private val stateStore = StateStore(emptyMap())

    private val dataPipeline: ProtoLayoutDynamicDataPipeline =
        ProtoLayoutDynamicDataPipeline(
            /* platformDataProviders= */ emptyMap(),
            stateStore,
            FixedQuotaManagerImpl(Int.MAX_VALUE),
            FixedQuotaManagerImpl(Int.MAX_VALUE),
        )

    @Before
    fun setUp() {
        // View needs to be attached.
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
    }

    @Test
    fun constructor_withTooFewColors_throws() {
        val linearProto = linearGradient(colorStop(Color.RED, 0.7f))

        val e =
            assertFailsWith<IllegalArgumentException> {
                LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}
            }
        assertThat(e).hasMessageThat().contains("color count must be in the range")
    }

    @Test
    fun constructor_withTooManyColors_throws() {
        val linearProto =
            linearGradient(
                colorStop(Color.RED),
                colorStop(Color.GREEN),
                colorStop(Color.BLUE),
                colorStop(Color.YELLOW),
                colorStop(Color.MAGENTA),
                colorStop(Color.CYAN),
                colorStop(Color.WHITE),
                colorStop(Color.BLACK),
                colorStop(Color.GRAY),
                colorStop(Color.DKGRAY),
                colorStop(Color.LTGRAY),
                colorStop(Color.GRAY),
            )

        val e =
            assertFailsWith<IllegalArgumentException> {
                LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}
            }
        assertThat(e).hasMessageThat().contains("color count must be in the range")
    }

    @Test
    fun setterIsCalled_invalidates() {
        val linearProto =
            linearGradient(colorStop(Color.RED), colorStop(Color.GREEN), colorStop(Color.BLUE)) {
                endX = 100f.dpProp.toOffset
                endY = 200f.dpProp.toOffset
            }
        var invalidateCount = 0
        val grad =
            LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) { invalidateCount++ }
        shadowOf(Looper.getMainLooper()).idle()
        invalidateCount = 0 // Reset count after construction.

        grad.startX = 1f
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(invalidateCount).isEqualTo(1)
        grad.endY = 0f
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(invalidateCount).isEqualTo(2)
    }

    @Test
    fun shader_isNotNull() {
        val linearProto =
            linearGradient(
                colorStop(Color.RED, 0f),
                colorStop(Color.GREEN, 0.2f),
                colorStop(Color.BLUE, 0.99f),
            ) {
                endX = 100f.dpProp.toOffset
                endY = 200f.dpProp.toOffset
            }
        val grad = LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}

        assertThat(grad.shader).isNotNull()
    }

    @Test
    fun withDynamicColors() {
        val color0 = Color.RED
        val color1 = Color.BLUE
        val linearProto =
            linearGradient(
                colorStop(colorProp(Color.GREEN, fixedDynamicColor(color0))),
                colorStop(colorProp(Color.GREEN, fixedDynamicColor(color1))),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val grad = LinearGradientHelper(linearProto, view, Optional.of(pipelineMaker), POS_ID) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        assertThat(grad.colors[0]).isEqualTo(color0)
        assertThat(grad.colors[1]).isEqualTo(color1)
    }

    @Test
    fun withDynamicOffsets() {
        val offset1 = 0.5f
        val offset2 = 0.6f
        val color1 = Color.RED
        val color2 = Color.BLUE
        val linearProto =
            linearGradient(
                colorStop(colorProp(Color.GREEN), 0f.prop),
                colorStop(colorProp(color1), floatProp(0.1f, fixedDynamicFloat(offset1))),
                colorStop(colorProp(color2), floatProp(0.2f, fixedDynamicFloat(offset2))),
                colorStop(colorProp(Color.GREEN), 1f.prop),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        val grad = LinearGradientHelper(linearProto, view, Optional.of(pipelineMaker), POS_ID) {}
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)

        assertThat(grad.colorOffsets!![1]).isEqualTo(offset1)
        assertThat(grad.colorOffsets[2]).isEqualTo(offset2)
    }

    @Test
    fun withDynamicColors_invalidateWhenColorIsUpdated() {
        val color0Key = "color0"
        val color0 = Color.RED
        val linearProto =
            linearGradient(
                colorStop(colorProp(Color.GREEN, stateDynamicColor(color0Key))),
                colorStop(colorProp(Color.BLUE)),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        var invalidateCount = 0
        val grad =
            LinearGradientHelper(linearProto, view, Optional.of(pipelineMaker), POS_ID) {
                invalidateCount++
            }
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)
        shadowOf(Looper.getMainLooper()).idle()
        invalidateCount = 0 // Reset count after construction.

        // Update the color.
        stateStore.setAppStateEntryValuesProto(
            mapOf(
                AppDataKey<DynamicBuilders.DynamicColor>(color0Key) to
                    DynamicDataValue.newBuilder()
                        .setColorVal(FixedColor.newBuilder().setArgb(color0))
                        .build()
            )
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(grad.colors[0]).isEqualTo(color0)
        assertThat(invalidateCount).isEqualTo(1)
    }

    @Test
    fun withDynamicOffsets_invalidateWhenOffsetIsUpdated() {
        val offset2Key = "offset2"
        val offset2 = 0.5f
        val linearProto =
            linearGradient(
                colorStop(colorProp(Color.GREEN), 0f.prop),
                colorStop(colorProp(Color.RED), 0.1f.prop),
                colorStop(colorProp(Color.BLUE), floatProp(0.2f, stateDynamicFloat(offset2Key))),
                colorStop(colorProp(Color.GREEN), 1f.prop),
            )
        val pipelineMaker = dataPipeline.newPipelineMaker()
        var invalidateCount = 0
        val grad =
            LinearGradientHelper(linearProto, view, Optional.of(pipelineMaker), POS_ID) {
                invalidateCount++
            }
        pipelineMaker.commit(FrameLayout(getApplicationContext()), /* isReattaching= */ false)
        shadowOf(Looper.getMainLooper()).idle()
        invalidateCount = 0 // Reset count after construction.

        // Update the offset.
        stateStore.setAppStateEntryValuesProto(
            mapOf(
                AppDataKey<DynamicBuilders.DynamicFloat>(offset2Key) to
                    DynamicDataValue.newBuilder()
                        .setFloatVal(FixedFloat.newBuilder().setValue(offset2))
                        .build()
            )
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(grad.colorOffsets!![2]).isEqualTo(offset2)
        assertThat(invalidateCount).isEqualTo(1)
    }

    @Test
    fun init_defaultsToHorizontalGradient() {
        val linearProto =
            linearGradient(colorStop(Color.RED), colorStop(Color.GREEN), colorStop(Color.BLUE))
        val grad = LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(grad.startX).isEqualTo(0f)
        assertThat(grad.startY).isEqualTo(0f)
        assertThat(grad.endX).isEqualTo(view.width.toFloat())
        assertThat(grad.endY).isEqualTo(0f)
    }

    @Test
    fun init_staticColorStopsAreInitiallySorted() {
        val linearProto =
            linearGradient(
                colorStop(Color.RED, 0.7f),
                colorStop(Color.BLUE, 0.99f),
                colorStop(Color.GREEN, 0.2f),
            )
        val grad = LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}

        assertThat(grad.colors).isEqualTo(intArrayOf(Color.GREEN, Color.RED, Color.BLUE))
        assertThat(grad.colorOffsets).isEqualTo(floatArrayOf(0.2f, 0.7f, 0.99f))
    }

    @Test
    fun init_dynamicColorStopsAreNotSorted() {
        val linearProto =
            linearGradient(
                colorStop(colorProp(Color.RED), floatProp(0.7f, fixedDynamicFloat(0.8f))),
                colorStop(Color.BLUE, 0.99f),
                colorStop(Color.GREEN, 0.2f),
            )
        val grad = LinearGradientHelper(linearProto, view, Optional.empty(), POS_ID) {}

        assertThat(grad.colors).isEqualTo(intArrayOf(Color.RED, Color.BLUE, Color.GREEN))
        assertThat(grad.colorOffsets).isEqualTo(floatArrayOf(0.7f, 0.99f, 0.2f))
    }

    private companion object {
        const val POS_ID = "1.2.3.4"
    }
}
