/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import android.annotation.SuppressLint
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectableModifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("NewApi")
@MediumTest
@RunWith(AndroidJUnit4::class)
class MagnifierTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun magnifier_inspectorValue_whenSupported() {
        val sourceCenterLambda: Density.() -> Offset = { Offset(42f, 42f) }
        val magnifierCenterLambda: Density.() -> Offset = { Offset(42f, 42f) }
        val modifier = Modifier.magnifier(
            sourceCenter = sourceCenterLambda,
            magnifierCenter = magnifierCenterLambda
        ).findInspectableValue()!!
        assertThat(modifier.nameFallback).isEqualTo("magnifier")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.toList()).containsExactly(
            ValueElement("sourceCenter", sourceCenterLambda),
            ValueElement("magnifierCenter", magnifierCenterLambda),
            ValueElement("zoom", Float.NaN),
            ValueElement("size", DpSize.Unspecified),
            ValueElement("cornerRadius", Dp.Unspecified),
            ValueElement("elevation", Dp.Unspecified),
            ValueElement("clippingEnabled", true),
        )
    }

    @SdkSuppress(maxSdkVersion = 27)
    @Test
    fun magnifier_inspectorValue_whenNotSupported() {
        val sourceCenterLambda: Density.() -> Offset = { Offset(42f, 42f) }
        val magnifierCenterLambda: Density.() -> Offset = { Offset(42f, 42f) }
        val modifier = Modifier.magnifier(
            sourceCenter = sourceCenterLambda,
            magnifierCenter = magnifierCenterLambda
        ).findInspectableValue()!!
        assertThat(modifier.nameFallback).isEqualTo("magnifier (not supported)")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.toList()).containsExactly(
            ValueElement("sourceCenter", sourceCenterLambda),
            ValueElement("magnifierCenter", magnifierCenterLambda),
            ValueElement("zoom", Float.NaN),
            ValueElement("size", DpSize.Unspecified),
            ValueElement("cornerRadius", Dp.Unspecified),
            ValueElement("elevation", Dp.Unspecified),
            ValueElement("clippingEnabled", true),
        )
    }

    @SdkSuppress(maxSdkVersion = 27)
    @Test
    fun magnifier_returnsEmptyModifier_whenNotSupported() {
        val modifier = Modifier.magnifier(sourceCenter = { Offset.Zero })
        val elements: List<Modifier.Element> =
            modifier.foldIn(emptyList()) { elements, element -> elements + element }

        // Modifier.magnifier doesn't have its own modifier class, so instead of checking for the
        // absence of the actual modifier we just check that the only modifier returned is the
        // InspectableValue (which actually has two elements).
        assertThat(elements).hasSize(2)
        assertThat(elements.first()).isInstanceOf(InspectableValue::class.java)
        assertThat(elements.last()).isInstanceOf(InspectableModifier.End::class.java)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_recreatesMagnifier_whenDensityChanged() {
        val magnifierFactory = CountingPlatformMagnifierFactory()
        var density by mutableStateOf(Density(1f))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(
                    Modifier.magnifier(
                        sourceCenter = { Offset.Zero },
                        magnifierCenter = { Offset.Unspecified },
                        zoom = Float.NaN,
                        onSizeChanged = null,
                        platformMagnifierFactory = magnifierFactory
                    )
                )
            }
        }

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }

        density = Density(density.density * 2)

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_recreatesMagnifier_whenConfigurationChanged() {
        val magnifierFactory = CountingPlatformMagnifierFactory()
        var elevation by mutableStateOf(1.dp)
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    elevation = elevation,
                    onSizeChanged = null,
                    platformMagnifierFactory = magnifierFactory
                )
            )
        }

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }

        elevation *= 2

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_recreatesMagnifier_whenConfigurationChangedToText() {
        val magnifierFactory = CountingPlatformMagnifierFactory()
        var useTextDefault by mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    useTextDefault = useTextDefault,
                    onSizeChanged = null,
                    platformMagnifierFactory = magnifierFactory
                )
            )
        }

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }

        useTextDefault = true

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_recreatesMagnifier_whenCannotUpdateZoom() {
        val magnifierFactory = CountingPlatformMagnifierFactory()
        var zoom by mutableStateOf(1f)
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = zoom,
                    onSizeChanged = null,
                    platformMagnifierFactory = magnifierFactory
                )
            )
        }

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }

        zoom += 2

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_doesNotRecreateMagnifier_whenCanUpdateZoom() {
        val magnifierFactory = CountingPlatformMagnifierFactory(canUpdateZoom = true)
        var zoom by mutableStateOf(1f)
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = zoom,
                    onSizeChanged = null,
                    platformMagnifierFactory = magnifierFactory

                )
            )
        }

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }

        zoom += 2

        rule.runOnIdle {
            assertThat(magnifierFactory.creationCount).isEqualTo(1)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_updatesContent_whenLayerRedrawn() {
        var drawTrigger by mutableStateOf(0)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier
                    // If the node has zero size, it won't draw and this test won't work.
                    .fillMaxSize()
                    .drawBehind {
                        // Read this state to trigger re-draw when it changes.
                        drawCircle(Color.Black, radius = drawTrigger.toFloat())
                    }
                    .magnifier(
                        sourceCenter = { Offset.Zero },
                        magnifierCenter = { Offset.Unspecified },
                        zoom = Float.NaN,
                        onSizeChanged = null,
                        platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                    )
            )
        }

        rule.runOnIdle {
            // This will always happen once right away, since there's always an immediate draw pass.
            assertThat(platformMagnifier.contentUpdateCount).isEqualTo(1)
        }

        drawTrigger++

        rule.runOnIdle {
            assertThat(platformMagnifier.contentUpdateCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_doesNotUpdateProperties_whenLayerRedrawn() {
        var drawTrigger by mutableStateOf(0)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier
                    // If the node has zero size, it won't draw and this test won't work.
                    .fillMaxSize()
                    .drawBehind {
                        // Read this state to trigger re-draw when it changes.
                        drawCircle(Color.Black, radius = drawTrigger.toFloat())
                    }
                    .magnifier(
                        sourceCenter = { Offset.Zero },
                        magnifierCenter = { Offset.Unspecified },
                        zoom = Float.NaN,
                        onSizeChanged = null,
                        platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                    )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }

        drawTrigger++

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_updatesProperties_whenPlacementChanged() {
        var layoutOffset by mutableStateOf(IntOffset.Zero)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier
                    .offset { layoutOffset }
                    .magnifier(
                        sourceCenter = { Offset.Zero },
                        magnifierCenter = { Offset.Unspecified },
                        zoom = Float.NaN,
                        onSizeChanged = null,
                        platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                    )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }

        layoutOffset += IntOffset(10, 1)

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_updatesProperties_whenSourceCenterChanged() {
        var sourceCenter by mutableStateOf(Offset(1f, 1f))
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { sourceCenter },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    onSizeChanged = null,
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }

        sourceCenter += Offset(1f, 1f)

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_updatesProperties_whenMagnifierCenterChanged() {
        var magnifierCenter by mutableStateOf(Offset(1f, 1f))
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { magnifierCenter },
                    zoom = Float.NaN,
                    onSizeChanged = null,
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }

        magnifierCenter += Offset(1f, 1f)

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun platformMagnifierModifier_updatesProperties_whenZoomChanged() {
        var zoom by mutableStateOf(1f)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = zoom,
                    onSizeChanged = null,
                    platformMagnifierFactory = PlatformMagnifierFactory(
                        platformMagnifier,
                        canUpdateZoom = true
                    )
                )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }

        zoom += 1f

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_dismissesMagnifier_whenRemovedFromComposition() {
        var showMagnifier by mutableStateOf(true)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                if (showMagnifier) {
                    Modifier.magnifier(
                        sourceCenter = { Offset.Zero },
                        magnifierCenter = { Offset.Unspecified },
                        zoom = Float.NaN,
                        onSizeChanged = null,
                        platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                    )
                } else {
                    Modifier
                }
            )
        }

        val initialDismissCount = rule.runOnIdle { platformMagnifier.dismissCount }

        showMagnifier = false

        rule.runOnIdle {
            assertThat(platformMagnifier.dismissCount).isEqualTo(initialDismissCount + 1)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_dismissesMagnifier_whenCenterUnspecified() {
        // Show the magnifier initially and then hide it, to ensure that it's actually dismissed vs
        // just never shown.
        var sourceCenter by mutableStateOf(Offset.Zero)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { sourceCenter },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    onSizeChanged = null,
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
        }
        val initialDismissCount = rule.runOnIdle { platformMagnifier.dismissCount }

        // Now update with an unspecified sourceCenter to hide it.
        sourceCenter = Offset.Unspecified

        rule.runOnIdle {
            assertThat(platformMagnifier.propertyUpdateCount).isEqualTo(1)
            assertThat(platformMagnifier.dismissCount).isEqualTo(initialDismissCount + 1)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_dismissesMagnifier_whenMagnifierRecreated() {
        var elevation by mutableStateOf(1.dp)
        val platformMagnifier = CountingPlatformMagnifier()
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    elevation = elevation,
                    onSizeChanged = null,
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        val initialDismissCount = rule.runOnIdle { platformMagnifier.dismissCount }

        elevation += 1.dp

        rule.runOnIdle {
            assertThat(platformMagnifier.dismissCount).isEqualTo(initialDismissCount + 1)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_firesOnSizeChanged_initially() {
        val magnifierSize = IntSize(10, 11)
        val sizeEvents = mutableListOf<DpSize>()
        val platformMagnifier = CountingPlatformMagnifier().apply {
            size = magnifierSize
        }
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    onSizeChanged = { sizeEvents += it },
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            assertThat(sizeEvents).containsExactly(
                with(rule.density) {
                    magnifierSize.toSize().toDpSize()
                }
            )
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_firesOnSizeChanged_initially_whenSourceCenterUnspecified() {
        val magnifierSize = IntSize(10, 11)
        val sizeEvents = mutableListOf<DpSize>()
        val platformMagnifier = CountingPlatformMagnifier().apply {
            size = magnifierSize
        }
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Unspecified },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    onSizeChanged = { sizeEvents += it },
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            assertThat(sizeEvents).containsExactly(
                with(rule.density) {
                    magnifierSize.toSize().toDpSize()
                }
            )
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_firesOnSizeChanged_whenNewSizeRequested() {
        val size1 = IntSize(10, 11)
        val size2 = size1 * 2
        var magnifierSize by mutableStateOf(size1)
        val magnifierDpSize by derivedStateOf {
            with(rule.density) {
                magnifierSize.toSize().toDpSize()
            }
        }
        val sizeEvents = mutableListOf<DpSize>()
        val platformMagnifier = CountingPlatformMagnifier().apply {
            size = magnifierSize
        }
        rule.setContent {
            Box(
                Modifier.magnifier(
                    sourceCenter = { Offset.Zero },
                    magnifierCenter = { Offset.Unspecified },
                    zoom = Float.NaN,
                    size = magnifierDpSize,
                    onSizeChanged = { sizeEvents += it },
                    platformMagnifierFactory = PlatformMagnifierFactory(platformMagnifier)
                )
            )
        }

        rule.runOnIdle {
            // Need to update the fake magnifier so it reports the right size when asked…
            platformMagnifier.size = size2
            // …and update the mutable state to trigger a recomposition.
            magnifierSize = size2
        }

        rule.runOnIdle {
            assertThat(sizeEvents).containsExactlyElementsIn(
                listOf(size1, size2).map {
                    with(rule.density) { it.toSize().toDpSize() }
                }
            ).inOrder()
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun platformMagnifierModifier_reportsSemantics() {
        var magnifierOffset by mutableStateOf(Offset.Zero)
        rule.setContent {
            Box(Modifier.magnifier(sourceCenter = { magnifierOffset }))
        }
        val getPosition = rule.onNode(keyIsDefined(MagnifierPositionInRoot))
            .fetchSemanticsNode()
            .config[MagnifierPositionInRoot]

        rule.runOnIdle {
            assertThat(getPosition()).isEqualTo(magnifierOffset)
        }

        // Move the modifier, same function should return new value.
        magnifierOffset = Offset(42f, 24f)

        rule.runOnIdle {
            assertThat(getPosition()).isEqualTo(magnifierOffset)
        }
    }

    private fun PlatformMagnifierFactory(
        platformMagnifier: PlatformMagnifier,
        canUpdateZoom: Boolean = false
    ) = object : PlatformMagnifierFactory {
        override val canUpdateZoom: Boolean = canUpdateZoom
        override fun create(
            view: View,
            useTextDefault: Boolean,
            size: DpSize,
            cornerRadius: Dp,
            elevation: Dp,
            clippingEnabled: Boolean,
            density: Density,
            initialZoom: Float
        ): PlatformMagnifier {
            return platformMagnifier
        }
    }

    private fun Modifier.findInspectableValue(): InspectableValue? =
        foldIn<InspectableValue?>(null) { acc, element -> acc ?: element as? InspectableValue }

    private class CountingPlatformMagnifierFactory(
        override val canUpdateZoom: Boolean = false
    ) : PlatformMagnifierFactory {
        var creationCount = 0

        override fun create(
            view: View,
            useTextDefault: Boolean,
            size: DpSize,
            cornerRadius: Dp,
            elevation: Dp,
            clippingEnabled: Boolean,
            density: Density,
            initialZoom: Float
        ): PlatformMagnifier {
            creationCount++
            return NoopPlatformMagnifier
        }
    }

    private object NoopPlatformMagnifier : PlatformMagnifier {
        override val size: IntSize = IntSize.Zero

        override fun updateContent() {
        }

        override fun update(
            sourceCenter: Offset,
            magnifierCenter: Offset,
            zoom: Float
        ) {
        }

        override fun dismiss() {
        }
    }

    private class CountingPlatformMagnifier : PlatformMagnifier {
        var contentUpdateCount = 0
        var propertyUpdateCount = 0
        var dismissCount = 0

        override var size: IntSize = IntSize.Zero

        override fun updateContent() {
            contentUpdateCount++
        }

        override fun update(
            sourceCenter: Offset,
            magnifierCenter: Offset,
            zoom: Float
        ) {
            propertyUpdateCount++
        }

        override fun dismiss() {
            dismissCount++
        }
    }
}
