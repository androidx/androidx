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

package androidx.ui.framework.demos

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.ui.core.vectorgraphics.adoptVectorGraphic
import androidx.ui.core.vectorgraphics.Group
import androidx.ui.core.vectorgraphics.Path
import androidx.ui.core.vectorgraphics.Vector
import androidx.ui.core.vectorgraphics.PathBuilder
import androidx.ui.core.vectorgraphics.PathDelegate
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.registerAdapter
import androidx.compose.setViewContent
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Px
import androidx.ui.core.vectorgraphics.compat.VectorResource
import androidx.ui.graphics.Color

class VectorGraphicsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setViewContent {
            composer.registerAdapter { parent, child ->
                adoptVectorGraphic(parent, child)
            }

            LinearLayout(orientation = LinearLayout.VERTICAL) {
                // TODO Make composition of components with Android Views automatically wire the appropriate ambients
                ContextAmbient.Provider(value = this@VectorGraphicsActivity) {
                    VectorResource(resId = androidx.ui.framework.demos.R.drawable.ic_crane)
                    vectorShape()
                }
            }
        }
    }

    @Composable
    fun vectorShape() {
        val viewportWidth = 300.0f
        val viewportHeight = 300.0f
        Vector(
            name = "vectorShape",
            defaultWidth = Px(300.0f),
            defaultHeight = Px(300.0f),
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        ) {
            Group(
                scaleX = 0.75f,
                scaleY = 0.75f,
                rotation = 45.0f,
                pivotX = (viewportWidth / 2),
                pivotY = (viewportHeight / 2)
            ) {
                backgroundPath(vectorWidth = viewportWidth, vectorHeight = viewportHeight)
                stripePath(vectorWidth = viewportWidth, vectorHeight = viewportHeight)
                Group(
                    translationX = 50.0f,
                    translationY = 50.0f,
                    pivotX = (viewportWidth / 2),
                    pivotY = (viewportHeight / 2),
                    rotation = 25.0f
                ) {
                    val pathData = PathDelegate {
                        moveTo(viewportWidth / 2 - 100, viewportHeight / 2 - 100)
                        horizontalLineToRelative(200.0f)
                        verticalLineToRelative(200.0f)
                        horizontalLineToRelative(-200.0f)
                        close()
                    }
                    Path(fill = Color.Magenta, pathData = pathData)
                }
            }
        }
    }

    @Composable
    fun backgroundPath(vectorWidth: Float, vectorHeight: Float) {
        val background = PathDelegate {
            horizontalLineTo(vectorWidth)
            verticalLineTo(vectorHeight)
            horizontalLineTo(0.0f)
            close()
        }

        Path(fill = Color.Cyan, pathData = background)
    }

    @Composable
    fun stripePath(vectorWidth: Float, vectorHeight: Float) {
        val stripeDelegate = PathDelegate {
            stripe(vectorWidth, vectorHeight, 10)
        }

        Path(stroke = Color.Blue, pathData = stripeDelegate)
    }

    private fun PathBuilder.stripe(vectorWidth: Float, vectorHeight: Float, numLines: Int) {
        val stepSize = vectorWidth / numLines
        var currentStep = stepSize
        for (i in 1..numLines) {
            moveTo(currentStep, 0.0f)
            verticalLineTo(vectorHeight)
            currentStep += stepSize
        }
    }
}
