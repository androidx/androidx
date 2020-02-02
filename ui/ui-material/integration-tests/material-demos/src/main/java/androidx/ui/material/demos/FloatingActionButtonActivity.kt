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

package androidx.ui.material.demos

import android.util.Log
import androidx.compose.Composable
import androidx.ui.graphics.imageFromResource
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.Arrangement
import androidx.ui.material.FloatingActionButton

class FloatingActionButtonActivity : MaterialDemoActivity() {

    @Composable
    override fun materialContent() {
        val icon = imageFromResource(resources, R.drawable.ic_favorite)
        Center {
            val onClick: () -> Unit = { Log.e("FABDemo", "onClick") }
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceEvenly) {
                FloatingActionButton(icon = icon, onClick = onClick,
                    modifier = LayoutGravity.Center)
                FloatingActionButton(
                    text = "EXTENDED",
                    onClick = onClick,
                    modifier = LayoutGravity.Center
                )
                FloatingActionButton(
                    icon = icon, text = "ADD TO FAVS",
                    onClick = onClick,
                    modifier = LayoutGravity.Center
                )
            }
        }
    }
}
