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

package androidx.lifecycle.viewmodel.compose

import android.os.Bundle
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateHandle.Companion.validateValue

/**
 * Inter-opt between [SavedStateHandle] and [rememberSaveable] so that any state holder that is
 * being saved via [rememberSaveable] can also be saved with [SavedStateHandle].
 */
@SavedStateHandleSaveableApi
fun <T : Any> SavedStateHandle.saveable(
    key: String,
    saver: Saver<T, out Any> = autoSaver(),
    init: () -> T,
): T {
    @Suppress("UNCHECKED_CAST")
    saver as Saver<T, Any>
    // value is restored using the SavedStateHandle or created via [init] lambda
    @Suppress("DEPRECATION") // Bundle.get has been deprecated in API 31
    val value = get<Bundle?>(key)?.get("value")?.let(saver::restore) ?: init()

    // Hook up saving the state to the SavedStateHandle
    setSavedStateProvider(key) {
        bundleOf("value" to with(saver) {
            SaverScope { validateValue(value) }.save(value)
        })
    }
    return value
}