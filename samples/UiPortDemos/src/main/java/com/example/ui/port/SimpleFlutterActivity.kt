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
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.ui.CraneView
import android.widget.ImageView
import androidx.ui.foundation.Key
import androidx.ui.painting.Image
import androidx.ui.widgets.basic.RawImage
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.view.createViewWidget

class SimpleFlutterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(CraneView(this,
                ViewCompatTestWidget(Key.createKey("ViewCompatTestWidget"))))
    }

    class ViewCompatTestWidget(key: Key) : StatelessWidget(key) {

        override fun build(context: BuildContext): Widget {
            return createViewWidget(Key.createKey("testViewCompat")) {
                context: Context ->
                    val imageView = ImageView(context)
                    imageView.setImageResource(R.drawable.test)
                    imageView
            }
        }
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
        private var bitmap: Bitmap
    ) : State<MirrorImageWidget>(widget) {
        init {
            fun mirrorImageDelayed() {
                Handler(Looper.getMainLooper()).postDelayed({
                    setState {
                        val prevBitmap = bitmap
                        bitmap = bitmap.mirror()
                        prevBitmap.recycle()
                    }
                    mirrorImageDelayed()
                }, 700 /* 700 milliseconds */)
            }
            mirrorImageDelayed()
        }

        override fun build(context: BuildContext): Widget {
            return RawImage(
                    image = Image(bitmap),
                    key = Key.createKey("jetpack image")
            )
        }

        private fun Bitmap.mirror(): Bitmap {
            val m = Matrix().apply {
                preScale(-1f, 1f)
            }
            return Bitmap.createBitmap(this, 0, 0, width, height, m, false)
        }
    }
}
