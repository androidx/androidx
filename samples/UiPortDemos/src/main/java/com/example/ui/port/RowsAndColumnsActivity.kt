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
import androidx.ui.CraneView
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.painting.Image
import androidx.ui.widgets.basic.Column
import androidx.ui.widgets.basic.Directionality
import androidx.ui.widgets.basic.RawImage
import androidx.ui.widgets.basic.Row
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.Widget

class RowsAndColumnsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            CraneView(this, LayoutTestWidget(
                Key.createKey("layout"),
                BitmapFactory.decodeResource(resources, R.drawable.test)))
        )
    }

    class LayoutTestWidget(key: Key, private val bitmap: Bitmap) : StatelessWidget(key) {
        override fun build(context: BuildContext): Widget {
            val columns = 3
            val rows = 10

            val columnWidgets = (0 until columns).map { columnIndex ->
                val images = (0 until rows).map { rowIndex ->
                    RawImage(
                        key = Key.createKey("image $columnIndex $rowIndex"),
                        image = Image(bitmap),
                        width = 100.0,
                        height = 100.0
                    )
                }
                Column(key = Key.createKey("column $columnIndex"), children = images)
            }

            return Directionality(key = Key.createKey("directionality"),
                textDirection = TextDirection.LTR,
                child = Row(key = Key.createKey("row"), children = columnWidgets)
            )
        }
    }
}