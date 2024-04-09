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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A predictive back handler that does nothing when running in a desktop context, since predictive
 * back is only supported on Android.
 *
 * @param drawerState state of the drawer
 * @param content content of the rest of the UI
 */
@Composable
internal actual fun DrawerPredictiveBackHandler(
    drawerState: DrawerState,
    content: @Composable (DrawerPredictiveBackState) -> Unit
) {
    content(remember { DrawerPredictiveBackState() })
}
