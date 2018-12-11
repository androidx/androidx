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
import androidx.ui.CraneView
import androidx.ui.foundation.Key
import androidx.ui.material.InkRipple
import androidx.ui.material.InkWell
import androidx.ui.material.material.Material
import androidx.ui.material.material.MaterialType
import androidx.ui.painting.Color
import androidx.ui.painting.Image
import androidx.ui.widgets.basic.RawImage

class RippleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        setContentView(
            CraneView(
                this,
                Material(
                    type = MaterialType.TRANSPARENCY,
                    child = InkWell(
                        splashFactory = InkRipple.SplashFactory,
                        splashColor = Color(android.graphics.Color.LTGRAY).withAlpha(125),
                        highlightColor = Color(android.graphics.Color.GRAY).withAlpha(125),
                        onTap = {},
                        child = RawImage(
                            key = Key.createKey("Test 2"),
                            image = Image(bitmap)
                        )
                    )
                )
            )
        )
    }
}
