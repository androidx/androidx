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
import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl
import androidx.wear.protolayout.expression.pipeline.StateStore
import androidx.wear.protolayout.proto.LayoutElementProto.Layout
import androidx.wear.protolayout.proto.ResourceProto.Resources
import androidx.wear.protolayout.renderer.common.RenderingArtifact
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.background
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.boundingBoxRatio
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.colorStop
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.dpProp
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.expandedBox
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.fingerprintedLayout
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.linearGradient
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.modifiers
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.toBrush
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutHelpers.toOffset
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewGroupMutation
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.ViewProperties
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** Tests for [ProtoLayoutInflater] in Kotlin. */
@RunWith(AndroidJUnit4::class)
class ProtoLayoutInflaterKtTest {
    val context = getApplicationContext<Context>()
    val screenWidthPx = context.resources.displayMetrics.widthPixels
    val screenHeightPx = context.resources.displayMetrics.heightPixels
    val stateStore = StateStore(emptyMap())
    val dataPipeline: ProtoLayoutDynamicDataPipeline =
        ProtoLayoutDynamicDataPipeline(
            /* platformDataProviders= */ emptyMap(),
            stateStore,
            FixedQuotaManagerImpl(Int.MAX_VALUE),
            FixedQuotaManagerImpl(Int.MAX_VALUE),
        )

    @Test
    fun inflate_boxWithLinearGradientBackground() {
        val boxElement = expandedBox {
            modifiers = modifiers {
                background = background {
                    brush =
                        linearGradient(colorStop(Color.RED, 0f), colorStop(Color.BLUE, 0.5f))
                            .toBrush
                }
            }
        }
        val layout = fingerprintedLayout(boxElement)

        val rootLayout = newRenderer(layout).inflate()

        assertThat(rootLayout.childCount).isEqualTo(1)
        val boxView = rootLayout.getChildAt(0)
        assertThat(boxView.background).isNotNull()
        assertThat(boxView.background).isInstanceOf(BackgroundDrawable::class.java)
        val backgroundDrawable = boxView.background as BackgroundDrawable
        with(backgroundDrawable.linearGradientHelper!!) {
            assertThat(colors).isEqualTo(intArrayOf(Color.RED, Color.BLUE))
            assertThat(colorOffsets).isEqualTo(floatArrayOf(0f, 0.5f))
        }
    }

    @Test
    fun inflate_boxWithLinearGradientBackground_withBoundingBoxRatioPoints() {
        val boxElement = expandedBox {
            modifiers = modifiers {
                background = background {
                    brush =
                        linearGradient(colorStop(Color.RED), colorStop(Color.BLUE)) {
                                startX = boundingBoxRatio(0.1f).toOffset
                                startY = boundingBoxRatio(0.2f).toOffset
                                endX = boundingBoxRatio(0.3f).toOffset
                                endY = boundingBoxRatio(0.4f).toOffset
                            }
                            .toBrush
                }
            }
        }
        val layout = fingerprintedLayout(boxElement)

        val rootLayout = newRenderer(layout).inflate()

        assertThat(rootLayout.childCount).isEqualTo(1)
        val boxView = rootLayout.getChildAt(0)
        assertThat(boxView.background).isInstanceOf(BackgroundDrawable::class.java)
        val backgroundDrawable = boxView.background as BackgroundDrawable
        with(backgroundDrawable.linearGradientHelper!!) {
            assertThat(startX).isEqualTo(0.1f * screenWidthPx)
            assertThat(startY).isEqualTo(0.2f * screenHeightPx)
            assertThat(endX).isEqualTo(0.3f * screenWidthPx)
            assertThat(endY).isEqualTo(0.4f * screenHeightPx)
        }
    }

    @Test
    fun inflate_boxWithLinearGradientBackground_withOffsetDpPoints() {
        val boxElement = expandedBox {
            modifiers = modifiers {
                background = background {
                    brush =
                        linearGradient(colorStop(Color.RED), colorStop(Color.BLUE)) {
                                startX = 10f.dpProp.toOffset
                                startY = 20f.dpProp.toOffset
                                endX = 30f.dpProp.toOffset
                                endY = 40f.dpProp.toOffset
                            }
                            .toBrush
                }
            }
        }
        val layout = fingerprintedLayout(boxElement)

        val rootLayout = newRenderer(layout).inflate()

        assertThat(rootLayout.childCount).isEqualTo(1)
        val boxView = rootLayout.getChildAt(0)
        assertThat(boxView.background).isInstanceOf(BackgroundDrawable::class.java)
        val backgroundDrawable = boxView.background as BackgroundDrawable
        with(backgroundDrawable.linearGradientHelper!!) {
            assertThat(startX).isEqualTo(dpToPx(context, 10f))
            assertThat(startY).isEqualTo(dpToPx(context, 20f))
            assertThat(endX).isEqualTo(dpToPx(context, 30f))
            assertThat(endY).isEqualTo(dpToPx(context, 40f))
        }
    }

    private fun localResourceResolvers() =
        StandardResourceResolvers.forLocalApp(
                Resources.newBuilder().build(),
                context,
                ContextCompat.getMainExecutor(context),
                /* animationEnabled= */ true,
            )
            .build()

    private fun inflaterConfig(
        layout: Layout,
        block: ProtoLayoutInflater.Config.Builder.() -> Unit = {},
    ) =
        ProtoLayoutInflater.Config.Builder(context, layout, localResourceResolvers())
            .setClickableIdExtra(CLICKABLE_ID_EXTRA)
            .setLoadActionListener {}
            .setLoadActionExecutor(ContextCompat.getMainExecutor(context))
            .setDynamicDataPipeline(dataPipeline)
            .apply(block)
            .build()

    private fun newRenderer(layout: Layout): TestRenderer {
        return TestRenderer(context, inflaterConfig(layout), dataPipeline)
    }

    private class TestRenderer(
        val context: Context,
        rendererConfig: ProtoLayoutInflater.Config,
        val dataPipeline: ProtoLayoutDynamicDataPipeline,
    ) {
        val inflater: ProtoLayoutInflater = ProtoLayoutInflater(rendererConfig)

        fun inflate(): FrameLayout {
            val rootLayout = FrameLayout(context)
            // This needs to be an attached view to test animations in data pipeline.
            Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(rootLayout)
            inflater.inflate(rootLayout)?.updateDynamicDataPipeline(/* isReattaching= */ false)
            shadowOf(Looper.getMainLooper()).idle()
            doLayout(rootLayout)
            return rootLayout
        }

        fun computeMutation(
            renderedMetadata: RenderedMetadata,
            targetLayout: Layout,
        ): ViewGroupMutation? =
            inflater.computeMutation(renderedMetadata, targetLayout, ViewProperties.EMPTY)

        fun applyMutation(parent: ViewGroup, mutation: ViewGroupMutation): Boolean {
            try {
                val applyMutationFuture: ListenableFuture<RenderingArtifact> =
                    inflater.applyMutation(parent, mutation)
                shadowOf(Looper.getMainLooper()).idle()
                applyMutationFuture.get()
                doLayout(parent)
            } catch (ex: Exception) {
                return false
            }
            return true
        }

        val dataPipelineSize: Int
            get() = dataPipeline.size()

        /**
         * Run a layout pass etc. This is required for basically everything that tries to make
         * assertions about width/height, or relative placement.
         */
        fun doLayout(rootLayout: View) {
            val screenWidthPx = context.resources.displayMetrics.widthPixels
            val screenHeightPx = context.resources.displayMetrics.heightPixels
            rootLayout.measure(
                MeasureSpec.makeMeasureSpec(screenWidthPx, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(screenHeightPx, MeasureSpec.EXACTLY),
            )
            rootLayout.layout(
                /* l= */ 0,
                /* t= */ 0,
                /* r= */ screenWidthPx,
                /* b= */ screenHeightPx,
            )
        }
    }

    private companion object {
        const val CLICKABLE_ID_EXTRA = "clickable_id_extra"

        fun dpToPx(context: Context, dp: Float) = (dp * context.resources.displayMetrics.density)
    }
}
