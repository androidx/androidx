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

import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable

@Composable
fun NotificationListLayout(
    notificationList: RemoteNotificationList,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteColumn(modifier = modifier) {
        RemoteText(text = RemoteString("Remote Notifications (Dynamic Size):"))

        val possibleSizes = IntArray(notificationList.items.size + 1) { it }
        RemoteStateLayout(notificationList.size, *possibleSizes) { currentSize ->
            RemoteColumn {
                notificationList.items.forEachIndexed { index, item ->
                    if (index < currentSize) {
                        RemoteColumn {
                            RemoteText(text = RemoteString("--- Slot $index ---"))
                            RemoteRow {
                                RemoteText(text = RemoteString("Title = "))
                                RemoteText(text = item.title)
                            }
                            RemoteRow {
                                RemoteText(text = RemoteString("Text = "))
                                RemoteText(text = item.text)
                            }
                        }
                    }
                }
            }
        }
    }
}
