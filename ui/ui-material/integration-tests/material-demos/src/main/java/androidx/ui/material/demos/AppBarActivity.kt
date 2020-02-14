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

import androidx.compose.Composable
import androidx.ui.graphics.imageFromResource
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.material.samples.SimpleBottomAppBar
import androidx.ui.material.samples.SimpleTopAppBar

class AppBarActivity : MaterialDemoActivity() {

    private val favouriteImage by lazy { { imageFromResource(resources, R.drawable.ic_favorite) } }
    private val navigationImage by lazy { { imageFromResource(resources, R.drawable.ic_menu) } }

    @Composable
    override fun materialContent() {
        Column {
            SimpleTopAppBar(favouriteImage, navigationImage)
            Spacer(LayoutFlexible(1f))
            SimpleBottomAppBar(favouriteImage, navigationImage)
        }
    }
}
