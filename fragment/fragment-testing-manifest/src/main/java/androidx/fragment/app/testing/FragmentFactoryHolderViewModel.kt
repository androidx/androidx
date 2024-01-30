/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.fragment.app.testing

import androidx.activity.viewModels
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A view-model to hold a fragment factory.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FragmentFactoryHolderViewModel : ViewModel() {
    var fragmentFactory: FragmentFactory? = null

    override fun onCleared() {
        super.onCleared()
        fragmentFactory = null
    }

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        internal val FACTORY: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val viewModel =
                        FragmentFactoryHolderViewModel()
                    return viewModel as T
                }
            }

        fun getInstance(activity: FragmentActivity): FragmentFactoryHolderViewModel {
            val viewModel: FragmentFactoryHolderViewModel by activity.viewModels { FACTORY }
            return viewModel
        }
    }
}
