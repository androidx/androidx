/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.Semantics
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import androidx.ui.material.ripple.BoundedRipple
import androidx.ui.material.ripple.CurrentRippleTheme
import androidx.ui.material.ripple.RippleColorCallback
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.material.ripple.RippleEffectFactory
import androidx.ui.material.ripple.RippleSurfaceOwner
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.material.surface.Card
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.vectormath64.Matrix4
import com.google.r4a.composer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class RippleEffectTest : AndroidUiTestRunner() {

    @Test
    fun rippleEffectMatrixHasOffsetFromSurface() {
        val latch = CountDownLatch(1)
        var matrix: Matrix4? = null

        val factory = object : RippleEffectFactory() {
            override fun create(
                rippleSurface: RippleSurfaceOwner,
                coordinates: LayoutCoordinates,
                touchPosition: PxPosition,
                color: Color,
                density: Density,
                shape: BoxShape,
                finalRadius: Px?,
                containedInkWell: Boolean,
                boundsCallback: ((LayoutCoordinates) -> PxBounds)?,
                clippingBorderRadius: BorderRadius?,
                onRemoved: (() -> Unit)?
            ): RippleEffect {
                return object : RippleEffect(rippleSurface, coordinates, color, onRemoved) {

                    init {
                        rippleSurface.addEffect(this)
                    }

                    override fun drawEffect(canvas: Canvas, transform: Matrix4) {
                        matrix = transform
                        latch.countDown()
                    }
                }
            }
        }
        val colorCallback: RippleColorCallback = { Color(0) }

        val padding = 10.dp
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <CurrentRippleTheme.Provider value=RippleTheme(factory, colorCallback)>
                        <Card>
                            <Padding padding=EdgeInsets(padding)>
                                <Semantics testTag="ripple">
                                    <BoundedRipple>
                                        <Container>
                                        </Container>
                                    </BoundedRipple>
                                </Semantics>
                            </Padding>
                        </Card>
                    </CurrentRippleTheme.Provider>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("ripple")
            .doClick()

        // wait for drawEffect to be called
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // verify matrix contains the expected padding
        assertNotNull(matrix)
        val paddingFloat = withDensity(density) { padding.toIntPx().value.toFloat() }
        val expectedMatrix = Matrix4.translationValues(
            paddingFloat,
            paddingFloat,
            0f
        )
        assertEquals(expectedMatrix, matrix)
    }
}