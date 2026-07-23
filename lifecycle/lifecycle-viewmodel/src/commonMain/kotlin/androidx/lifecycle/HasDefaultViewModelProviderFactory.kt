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
import androidx.lifecycle.viewmodel.internal.DefaultViewModelProviderFactory

/**
 * An interface marking a [ViewModelStoreOwner] as having a default [ViewModelProvider.Factory] for
 * use with [ViewModelProvider].
 */
public interface HasDefaultViewModelProviderFactory {
    /**
     * The default [ViewModelProvider.Factory] to use when no custom [ViewModelProvider.Factory] is
     * provided to the [ViewModelProvider].
     */
    public val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = DefaultViewModelProviderFactory

    /**
     * The default [CreationExtras] to pass into [ViewModelProvider.Factory.create] when no
     * overriding [CreationExtras] are provided.
     */
    public val defaultViewModelCreationExtras: CreationExtras
        get() = CreationExtras.Empty
}
