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

import androidx.compose.runtime.Composable
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

internal actual class BackEventCompat {
    actual val touchX: Float = 0f
    actual val touchY: Float = 0f
    actual val progress: Float = 0f
    actual val swipeEdge: Int = -1

    init {
        implementedInJetBrainsFork()
    }
}

@Composable
internal actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) {
    implementedInJetBrainsFork()
}

@OptIn(ExperimentalStdlibApi::class)
internal actual fun randomUUID(): String {
    val bytes =
        Random.nextBytes(16).also {
            it[6] = it[6] and 0x0f // clear version
            it[6] = it[6] or 0x40 // set to version 4
            it[8] = it[8] and 0x3f // clear variant
            it[8] = it[8] or 0x80.toByte() // set to IETF variant
        }
    return buildString(capacity = 36) {
        append(bytes.toHexString(0, 4))
        append('-')
        append(bytes.toHexString(4, 6))
        append('-')
        append(bytes.toHexString(6, 8))
        append('-')
        append(bytes.toHexString(8, 10))
        append('-')
        append(bytes.toHexString(10))
    }
}

internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    actual fun get(): T? = implementedInJetBrainsFork()

    actual fun clear(): Unit = implementedInJetBrainsFork()
}
