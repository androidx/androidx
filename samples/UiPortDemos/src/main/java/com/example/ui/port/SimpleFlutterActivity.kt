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
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.Key
import androidx.ui.painting.BlendMode
import androidx.ui.painting.BoxFit
import androidx.ui.painting.Color
import androidx.ui.painting.Image
import androidx.ui.painting.ImageRepeat
import androidx.ui.widgets.basic.RawImage

class SimpleFlutterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        val widget = RawImage(image = Image(bitmap),
                width = 100.0,
                height = 100.0,
                centerSlice = Rect.zero,
                color = Color(0),
                fit = BoxFit.fitHeight,
                key = Key.createKey("key"),
                repeat = ImageRepeat.noRepeat,
                scale = 1.0,
                colorBlendMode = BlendMode.color,
                matchTextDirection = false)
        setContentView(SimpleFlutterView(this, widget))
    }
}
