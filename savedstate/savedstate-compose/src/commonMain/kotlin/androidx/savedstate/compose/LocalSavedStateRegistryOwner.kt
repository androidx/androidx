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

@file:JvmName("LocalSavedStateRegistryOwnerKt")
@file:JvmMultifileClass
@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released.

package androidx.savedstate.compose

import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithHostDefaultOf
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/** The CompositionLocal containing the current [SavedStateRegistryOwner]. */
public val LocalSavedStateRegistryOwner: ProvidableCompositionLocal<SavedStateRegistryOwner> =
    compositionLocalWithHostDefaultOf(SavedStateRegistryOwnerHostDefaultKey)

/**
 * A [HostDefaultKey] used to retrieve the [SavedStateRegistryOwner] provided by the current hosting
 * environment.
 *
 * This key allows the composition to access the host's [SavedStateRegistryOwner] through a
 * decoupled mechanism, typically used by [compositionLocalWithHostDefaultOf].
 *
 * @see HostDefaultKey
 * @see compositionLocalWithHostDefaultOf
 */
public expect val SavedStateRegistryOwnerHostDefaultKey: HostDefaultKey<SavedStateRegistryOwner>
