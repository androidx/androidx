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
import android.os.Bundle
import android.os.Handler
import androidx.ui.foundation.ContainingView
import androidx.ui.foundation.Key
import androidx.ui.painting.Image
import androidx.ui.painting.alignment.Alignment
import androidx.ui.widgets.basic.Align
import androidx.ui.widgets.basic.RawImage
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget

class ContainingViewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        setContentView(ContainingView(this, AlignedImageWidget(bitmap)))
    }

    class AlignedImageWidget(
        private val bitmap: Bitmap
    ) : StatefulWidget(Key.createKey("container")) {

        override fun createState() = object : State<StatefulWidget>(this) {

            private var alignTop = true

            override fun initState() {
                super.initState()
                changeAlignDelayed()
            }

            private fun changeAlignDelayed() {
                Handler().postDelayed({
                    setState {
                        alignTop = !alignTop
                    }
                    changeAlignDelayed()
                }, 700)
            }

            override fun build(context: BuildContext): Widget {
                return Align(
                    key = Key.createKey("align"),
                    alignment = if (alignTop) Alignment.topLeft else Alignment.bottomRight,
                    child = RawImage(
                        image = Image(bitmap),
                        key = Key.createKey("image")
                    )
                )
            }
        }
    }
}
