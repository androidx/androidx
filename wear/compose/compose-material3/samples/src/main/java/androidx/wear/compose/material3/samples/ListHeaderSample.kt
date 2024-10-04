/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun ListHeaderSample() {
    ListHeader { Text("Header") }
}

@Sampled
@Composable
fun ListSubHeaderSample() {
    ListSubHeader { Text("SubHeader") }
}

@Sampled
@Composable
fun ListSubHeaderWithIconSample() {
    ListSubHeader(
        label = { Text(text = "SubHeader") },
        icon = { Icon(imageVector = Icons.Outlined.Home, "home") }
    )
}
