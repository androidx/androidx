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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.lifecycle

import androidx.annotation.RestrictTo
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/**
 * A [ViewModelProvider.Factory] that creates [ViewModel]s with access to a [SavedStateHandle] in
 * their constructor.
 */
public expect class SavedStateViewModelFactory : ViewModelProvider.Factory {

    /**
     * Constructs this factory.
     *
     * When using this factory, the component scoping the [SavedStateHandle] must first invoke
     * [enableSavedStateHandles].
     *
     * @see createSavedStateHandle
     */
    public constructor()

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T
}
