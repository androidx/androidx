/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import platform.UIKit.UILabel

val LazyColumnWithInteropViewsExample = Screen.Example("LazyColumn with interop views") {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(100) {
            if (it % 2 == 0) {
                UIKitView(
                    factory = {
                        val view = UILabel()
                        view.text = "UILabel $it"
                        view
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
            } else {
                Text("Text $it", Modifier.height(40.dp))
            }
        }
    }
}