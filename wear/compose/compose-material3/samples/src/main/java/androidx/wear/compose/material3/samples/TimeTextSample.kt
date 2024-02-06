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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults

@Sampled
@Composable
fun TimeTextClockOnly() {
    TimeText {
        time()
    }
}

@Sampled
@Composable
fun TimeTextWithStatus() {
    val leadingTextStyle = TimeTextDefaults.timeTextStyle(
        color = Color.Green
    )
    TimeText {
        text("ETA 12:48", leadingTextStyle)
        separator()
        time()
    }
}

@Sampled
@Composable
fun TimeTextWithIcon() {
    TimeText {
        time()
        separator()
        composable {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
