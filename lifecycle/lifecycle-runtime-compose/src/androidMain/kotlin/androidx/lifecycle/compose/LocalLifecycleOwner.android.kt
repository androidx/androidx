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

@file:JvmName("LocalLifecycleOwnerKt")
@file:JvmMultifileClass
@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released.

package androidx.lifecycle.compose

import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ViewTreeHostDefaultKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.runtime.R

public actual val LifecycleOwnerHostDefaultKey: HostDefaultKey<LifecycleOwner> =
    object : ViewTreeHostDefaultKey<LifecycleOwner> {
        override val tagKey: Int
            get() = R.id.view_tree_lifecycle_owner
    }
