/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ViewTreeHostDefaultKey
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.R

public actual val ViewModelStoreOwnerHostDefaultKey: HostDefaultKey<ViewModelStoreOwner?> =
    object : ViewTreeHostDefaultKey<ViewModelStoreOwner?> {
        override val tagKey: Int
            get() = R.id.view_tree_view_model_store_owner
    }
