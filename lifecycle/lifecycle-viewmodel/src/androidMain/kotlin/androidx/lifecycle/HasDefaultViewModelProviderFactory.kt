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
package androidx.lifecycle

import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Interface that marks a [ViewModelStoreOwner] as having a default
 * [ViewModelProvider.Factory] for use with [ViewModelProvider].
 */
public interface HasDefaultViewModelProviderFactory {
    /**
     * Returns the default [ViewModelProvider.Factory] that should be
     * used when no custom `Factory` is provided to the
     * [ViewModelProvider] constructors.
     */
    public val defaultViewModelProviderFactory: ViewModelProvider.Factory

    /**
     * Returns the default [CreationExtras] that should be passed into
     * [ViewModelProvider.Factory.create] when no overriding
     * [CreationExtras] were passed to the [ViewModelProvider] constructors.
     */
    public val defaultViewModelCreationExtras: CreationExtras
        get() = CreationExtras.Empty
}
