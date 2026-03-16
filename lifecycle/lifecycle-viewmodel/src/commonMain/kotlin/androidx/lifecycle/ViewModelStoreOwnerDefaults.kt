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

@file:JvmName("ViewModelStoreOwnerDefaults")

package androidx.lifecycle

import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.internal.DefaultViewModelProviderFactory
import kotlin.jvm.JvmName

/**
 * Returns the appropriate default [ViewModelProvider.Factory] to use.
 *
 * If the provided [ViewModelStoreOwner] implements [HasDefaultViewModelProviderFactory], this will
 * return its [HasDefaultViewModelProviderFactory.defaultViewModelProviderFactory]. Otherwise, or if
 * [ViewModelStoreOwner] is `null`, it falls back to the standard [DefaultViewModelProviderFactory].
 */
@get:JvmName("getViewModelProviderFactory")
public val ViewModelStoreOwner?.defaultViewModelProviderFactory: ViewModelProvider.Factory
    get() {
        return if (this is HasDefaultViewModelProviderFactory) {
            this.defaultViewModelProviderFactory
        } else {
            DefaultViewModelProviderFactory
        }
    }

/**
 * Returns the appropriate default [CreationExtras] to use.
 *
 * If the provided [ViewModelStoreOwner] implements [HasDefaultViewModelProviderFactory], this will
 * return its [HasDefaultViewModelProviderFactory.defaultViewModelCreationExtras]. Otherwise, or if
 * [ViewModelStoreOwner] is `null`, it falls back to [CreationExtras.Empty].
 */
@get:JvmName("getViewModelCreationExtras")
public val ViewModelStoreOwner?.defaultViewModelCreationExtras: CreationExtras
    get() {
        return if (this is HasDefaultViewModelProviderFactory) {
            this.defaultViewModelCreationExtras
        } else {
            CreationExtras.Empty
        }
    }
