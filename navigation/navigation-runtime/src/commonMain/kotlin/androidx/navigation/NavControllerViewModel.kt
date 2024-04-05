/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlin.jvm.JvmStatic

/**
 * NavControllerViewModel is the always up to date view of the NavController's
 * non configuration state
 */
internal expect class NavControllerViewModel : ViewModel, NavViewModelStoreProvider {
    fun clear(backStackEntryId: String)

    companion object {
        @JvmStatic
        fun getInstance(viewModelStore: ViewModelStore): NavControllerViewModel
    }
}
