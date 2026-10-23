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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.ui.unit.LayoutDirection

/** Converts a Compose UI [LayoutDirection] to a [RemoteLayoutDirection]. */
internal fun LayoutDirection.toRemoteLayoutDirection(): RemoteLayoutDirection =
    when (this) {
        LayoutDirection.Ltr -> RemoteLayoutDirection.Ltr
        LayoutDirection.Rtl -> RemoteLayoutDirection.Rtl
    }

/** Converts a [RemoteLayoutDirection] to a Compose UI [LayoutDirection]. */
internal fun RemoteLayoutDirection.toComposeLayoutDirection(): LayoutDirection =
    when (this) {
        RemoteLayoutDirection.Ltr -> LayoutDirection.Ltr
        RemoteLayoutDirection.Rtl -> LayoutDirection.Rtl
    }
