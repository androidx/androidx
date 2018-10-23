/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.ui.port

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import androidx.ui.CraneView
import androidx.ui.foundation.Key
import androidx.ui.painting.Image
import androidx.ui.painting.alignment.Alignment
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.widgets.basic.Align
import androidx.ui.widgets.basic.RawImage
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.gesturedetector.GestureDetector

class TouchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(CraneView(this, createTestMirrorImageWidget()))
    }

    private fun createTestMirrorImageWidget(): MirrorImageWidget {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        return MirrorImageWidget(Key.createKey("jetpack image widget!"), bitmap)
    }

    class MirrorImageWidget(key: Key, private val bitmap: Bitmap) : StatefulWidget(key) {
        override fun createState() =
            MirrorImageWidgetState(this, bitmap) as State<StatefulWidget>
    }

    class MirrorImageWidgetState(
        widget: MirrorImageWidget,
        private val bitmap: Bitmap
    ) : State<MirrorImageWidget>(widget) {

        var horizontalMirror = false
        var verticalMirror = false

        override fun build(context: BuildContext): Widget {
            val bitmap: Bitmap =
                if (horizontalMirror || verticalMirror) {
                    bitmap.mirror(horizontalMirror, verticalMirror)
                } else {
                    bitmap
                }

            return Align(
                key = Key.createKey("alignWidget"),
                alignment = Alignment.center,
                child = GestureDetector(
                    child = RawImage(
                        image = Image(bitmap),
                        key = Key.createKey("jetpack image")
                    ),
                    onHorizontalDragStart = {
                        setState { horizontalMirror = !horizontalMirror }
                    },
                    onVerticalDragStart = {
                        setState { verticalMirror = !verticalMirror }
                    },
                    excludeFromSemantics = true,
                    behavior = HitTestBehavior.TRANSLUCENT
                )
            )
        }

        private fun Bitmap.mirror(horizontal: Boolean, vertical: Boolean): Bitmap {
            val m = Matrix().apply {
                preScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
            }
            return Bitmap.createBitmap(this, 0, 0, width, height, m, false)
        }
    }
}
