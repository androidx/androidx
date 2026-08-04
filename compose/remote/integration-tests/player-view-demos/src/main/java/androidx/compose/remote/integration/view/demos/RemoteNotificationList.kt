/*
 * Copyright 2026 The Android Open Source Project
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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class NotificationItem(val title: RemoteString, val text: RemoteString)

class RemoteNotificationList(val size: RemoteInt, val items: List<NotificationItem>)

@Composable
fun rememberRemoteNotificationList(
    name: String,
    maxSize: Int,
    initialSize: Int = 3,
): RemoteNotificationList {
    val size = rememberNamedRemoteInt("${name}.size", initialSize)
    val items =
        List(maxSize) { index ->
            val title = rememberNamedRemoteString("${name}.${index}.title", "Default Title $index")
            val text = rememberNamedRemoteString("${name}.${index}.text", "Default Text $index")
            NotificationItem(title, text)
        }
    return remember(size, items) { RemoteNotificationList(size, items) }
}
