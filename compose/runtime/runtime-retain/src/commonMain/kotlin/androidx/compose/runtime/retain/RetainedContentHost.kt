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

package androidx.compose.runtime.retain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer

/**
 * `RetainedContentHost` is used to install a [RetainedValuesStore] around a block of [content]. The
 * installed `RetainedValuesStore` is managed such that the store will start to retain exited values
 * when [active] is false, and stop retaining exited values when [active] becomes true. See
 * [RetainedValuesStore.isRetainingExitedValues] for more information on this terminology.
 *
 * `RetainedContentHost` is designed as an out-of-the-box solution for managing content that's
 * controlled effectively by an if/else statement. The [content] provided to this lambda will render
 * when [active] is true, and be removed when [active] is false. If the content is hidden and then
 * shown again in this way, the installed RetainedValuesStore will restore all retained values from
 * the last time the content was shown.
 *
 * The managed RetainedValuesStore is _also_ retained. If this composable is removed while the
 * parent store is retaining its exited values, this store will be persisted so that it can be
 * restored in the future. If this composable is removed while its parent store is not retaining its
 * exited values, the store will be discarded and all its held values will be immediately retired.
 *
 * For this reason, when using this as a mechanism to retain values for content that is being shown
 * and hidden, this composable must be hoisted high enough so that it is not removed when the
 * content being retained is hidden.
 *
 * @param active Whether this host should compose its [content]. When this value is true, [content]
 *   will be rendered and the installed [RetainedValuesStore] will not retain exited values. When
 *   this value is false, [content] will stop being rendered and the installed [RetainedValuesStore]
 *   will collect and retain its exited values for future restoration.
 * @param content The content to render. Inside of this lambda, [LocalRetainedValuesStore] is set to
 *   the [RetainedValuesStore] managed by this composable.
 * @sample androidx.compose.runtime.retain.samples.retainedContentHostSample
 * @see retainControlledRetainedValuesStore
 */
@Composable
public fun RetainedContentHost(active: Boolean, content: @Composable () -> Unit) {
    val retainedValuesStore = retainControlledRetainedValuesStore()
    if (active) {
        CompositionLocalProvider(LocalRetainedValuesStore provides retainedValuesStore, content)

        // Match the isRetainingExitedValues state to the active parameter. This effect must come
        // AFTER the content to correctly capture values.
        val composer = currentComposer
        DisposableEffect(retainedValuesStore) {
            // Stop retaining exited values when we become active. Use the request count to only
            // look at our state and to ignore any parent-influenced requests.
            val cancellationHandle =
                if (retainedValuesStore.retainExitedValuesRequestsFromSelf > 0) {
                    composer.scheduleFrameEndCallback {
                        retainedValuesStore.stopRetainingExitedValues()
                    }
                } else {
                    null
                }

            onDispose {
                // Start retaining exited values when we deactivate
                cancellationHandle?.cancel()
                retainedValuesStore.startRetainingExitedValues()
            }
        }
    }
}
