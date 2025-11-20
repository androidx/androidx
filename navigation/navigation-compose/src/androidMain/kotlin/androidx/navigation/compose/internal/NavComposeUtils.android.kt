/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation.compose.internal

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.flow.Flow

internal actual typealias BackEventCompat = androidx.activity.BackEventCompat

@Composable
internal actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) {
    PredictiveBackHandler(enabled, onBack)
}

internal actual fun randomUUID(): String = UUID.randomUUID().toString()

/**
 * Class WeakReference encapsulates weak reference to an object, which could be used to either
 * retrieve a strong reference to an object, or return null, if object was already destroyed by the
 * memory manager.
 */
internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    private val weakReference = WeakReference(reference)

    actual fun get(): T? = weakReference.get()

    actual fun clear() = weakReference.clear()
}
