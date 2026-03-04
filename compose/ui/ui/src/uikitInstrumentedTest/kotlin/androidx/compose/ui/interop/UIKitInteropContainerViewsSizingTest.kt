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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.measureFittingSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIStackView
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class UIKitInteropContainerViewsSizingTest {

    @Test
    fun testUIStackViewVerticalFixedWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeSize = DpSize.Zero
        val boxSize = DpSize(400.dp, 400.dp)
        val fixedWidth = 120.dp

        val factory = {
            val l1 = UILabel().apply { text = "TEXT"; numberOfLines = 1 }
            val l2 = UILabel().apply { text = List(100) { "TEXT" }.joinToString(" "); numberOfLines = 0 }

            UIStackView().apply {
                axis = UILayoutConstraintAxisVertical
                spacing = 8.0
                addArrangedSubview(l1)
                addArrangedSubview(l2)
            }
        }

        setContent {
            Box(Modifier.size(boxSize), contentAlignment = Alignment.Center) {
                UIKitView(
                    factory = factory,
                    properties = UIKitInteropProperties(placedAsOverlay = overlay),
                    modifier = Modifier
                        .width(fixedWidth)
                        .onGloballyPositioned {
                            composeSize = it.boundsInRoot().size.toDpSize(density)
                        }
                )
            }
        }

        val expectedSize = factory()
            .also { it.translatesAutoresizingMaskIntoConstraints = false }
            .measureFittingSize(
                fixedWidth = fixedWidth,
                maxWidth = boxSize.width,
                maxHeight = boxSize.height
            )

        assertEquals(expectedSize, composeSize)
    }

    @Test
    fun testAspectRatioSquareFixedWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeSize = DpSize.Zero
        val boxSize = DpSize(300.dp, 300.dp)
        val fixedWidth = 120.dp

        val factory = {
            UIView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                // width == height (square)
                NSLayoutConstraint.activateConstraints(
                    listOf(widthAnchor.constraintEqualToAnchor(heightAnchor))
                )
                backgroundColor = UIColor.redColor
            }
        }

        setContent {
            Box(Modifier.size(boxSize), contentAlignment = Alignment.Center) {
                UIKitView(
                    factory = factory,
                    properties = UIKitInteropProperties(placedAsOverlay = overlay),
                    modifier = Modifier
                        .width(fixedWidth)
                        .onGloballyPositioned {
                            composeSize = it.boundsInRoot().size.toDpSize(density)
                        }
                )
            }
        }

        assertEquals(DpSize(width = fixedWidth, height = fixedWidth), composeSize)
    }

    @Test
    fun testAspectRatioRectangleFixedHeight() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeSize = DpSize.Zero
        val boxSize = DpSize(300.dp, 300.dp)
        val fixedHeight = 120.dp
        val aspectRatio = 2.0f

        val factory = {
            UIView().apply {
                translatesAutoresizingMaskIntoConstraints = false
               // backgroundColor = UIColor.blueColor
                NSLayoutConstraint.activateConstraints(
                    listOf(
                        widthAnchor.constraintEqualToAnchor(
                            heightAnchor,
                            multiplier = aspectRatio.toDouble()
                        ),
                    )
                )
            }
        }

        setContent {
            Box(Modifier
                .background(Color.Red)
                .size(boxSize), contentAlignment = Alignment.Center) {
                UIKitView(
                    factory = factory,
                    properties = UIKitInteropProperties(placedAsOverlay = false),
                    modifier = Modifier
                        .background(Color.Green)
                        .height(fixedHeight)
                        .onGloballyPositioned {
                            composeSize = it.boundsInRoot().size.toDpSize(density)
                        }
                )
            }
        }

        assertEquals(
            DpSize(width = fixedHeight.times(aspectRatio), height = fixedHeight),
            composeSize
        )
    }
}