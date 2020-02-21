/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout.test

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.test.filters.SmallTest
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirectionAmbient
import androidx.ui.core.LayoutModifier
import androidx.ui.core.ModifierScope
import androidx.ui.core.Ref
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutDirectionModifier
import androidx.ui.layout.MaxIntrinsicWidth
import androidx.ui.layout.Stack
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class LayoutDirectionModifierTest : LayoutTest() {

    @Test
    fun testModifiedLayoutDirection_inMeasureScope() {
        val latch = CountDownLatch(1)
        val layoutDirection = Ref<androidx.ui.core.LayoutDirection>()
        val children = @Composable {
            Layout(
                children = @Composable() {},
                modifier = LayoutDirectionModifier.Rtl
            ) { _, _ ->
                layoutDirection.value = this.layoutDirection
                latch.countDown()
                layout(0.ipx, 0.ipx) {}
            }
        }
        show {
            Providers(
                LayoutDirectionAmbient provides androidx.ui.core.LayoutDirection.Ltr,
                children = children
            )
            children()
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(androidx.ui.core.LayoutDirection.Rtl == layoutDirection.value)
    }

    @Test
    fun testModifiedLayoutDirection_inIntrinsicsMeasure() {
        val latch = CountDownLatch(1)
        var layoutDirection: androidx.ui.core.LayoutDirection? = null
        val children = @Composable {
            MaxIntrinsicWidth {
                Layout(
                    children = @Composable() {},
                    modifier = LayoutDirectionModifier.Rtl,
                    minIntrinsicWidthMeasureBlock = { _, _ -> 0.ipx },
                    minIntrinsicHeightMeasureBlock = { _, _ -> 0.ipx },
                    maxIntrinsicWidthMeasureBlock = { _, _ ->
                        layoutDirection = this.layoutDirection
                        latch.countDown()
                        0.ipx
                    },
                    maxIntrinsicHeightMeasureBlock = { _, _ -> 0.ipx }
                ) { _, _ ->
                    layout(0.ipx, 0.ipx) {}
                }
            }
        }

        show {
            Providers(
                LayoutDirectionAmbient provides androidx.ui.core.LayoutDirection.Ltr,
                children = children
            )
            children()
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(layoutDirection)
        assertTrue(androidx.ui.core.LayoutDirection.Rtl == layoutDirection!!)
    }

    @Test
    fun testModifiedLayoutDirection_insideModifier() {
        val latch = CountDownLatch(1)
        var layoutDirection: androidx.ui.core.LayoutDirection? = null
        val rtlAwareModifier = object : LayoutModifier {
            override fun ModifierScope.modifyPosition(
                childSize: IntPxSize,
                containerSize: IntPxSize
            ): IntPxPosition {
                layoutDirection = this.layoutDirection
                latch.countDown()
                return if (this.layoutDirection == androidx.ui.core.LayoutDirection.Ltr) {
                    IntPxPosition.Origin
                } else {
                    IntPxPosition(containerSize.width - childSize.width, 0.ipx)
                }
            }
        }
        val children = @Composable {
            Stack {
                Container(modifier = LayoutDirectionModifier.Rtl + rtlAwareModifier) {}
            }
        }

        show {
            Providers(
                LayoutDirectionAmbient provides androidx.ui.core.LayoutDirection.Ltr,
                children = children
            )
            children()
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(layoutDirection)
        assertTrue(androidx.ui.core.LayoutDirection.Rtl == layoutDirection!!)
    }

    @Test
    fun testRestoreLayoutDirection() {
        val latch = CountDownLatch(1)
        var layoutDirection: androidx.ui.core.LayoutDirection? = null
        val children = @Composable {
            Layout(
                children = @Composable() {},
                modifier = LayoutDirectionModifier.Rtl + LayoutDirectionModifier.Restore
            ) { _, _ ->
                layoutDirection = this.layoutDirection
                latch.countDown()
                layout(0.ipx, 0.ipx) {}
            }
        }

        show {
            Providers(
                LayoutDirectionAmbient provides androidx.ui.core.LayoutDirection.Ltr,
                children = children
            )
            children()
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(layoutDirection)
        assertTrue(androidx.ui.core.LayoutDirection.Ltr == layoutDirection!!)
    }
}