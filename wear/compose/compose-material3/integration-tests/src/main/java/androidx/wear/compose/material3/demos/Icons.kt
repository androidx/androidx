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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon

@Composable
internal fun StandardIcon(size: Dp) {
    Icon(
        Icons.Filled.Favorite,
        contentDescription = "Favorite icon",
        modifier = Modifier.size(size)
    )
}

@Composable
internal fun AvatarIcon() {
    Icon(
        Icons.Filled.AccountCircle,
        contentDescription = "Account",
        modifier = Modifier.size(ButtonDefaults.LargeIconSize)
    )
}
