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
@file:JvmName("ViewModelProviderGetKt")

package androidx.lifecycle

import androidx.annotation.MainThread
import kotlin.jvm.JvmName

/**
 * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or an
 * activity), associated with this `ViewModelProvider`.
 *
 * @see ViewModelProvider.get(Class)
 */
@MainThread public inline fun <reified VM : ViewModel> ViewModelProvider.get(): VM = get(VM::class)

/**
 * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or an
 * activity), associated with this `ViewModelProvider` and the given [key].
 *
 * @param key The key to use to identify the ViewModel.
 * @see ViewModelProvider.get(String, Class)
 */
@MainThread
@Suppress("TypeParameterName") // Using 'VM' to match the existing ViewModelProvider API.
public inline fun <reified VM : ViewModel> ViewModelProvider.get(key: String): VM =
    get(key, modelClass = VM::class)
