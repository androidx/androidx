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

import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [RetainedValuesStore] in which [retain] values will be tracked in. Since a
 * RetainedValuesStore controls retention scenarios and signals when to start and end the retention
 * of objects removed from composition, a composition hierarchy may have several
 * RetainedValuesStores to introduce retention periods to specific pieces of content.
 *
 * The default implementation is a [ForgetfulRetainedValuesStore] that causes [retain] to behave the
 * same as [remember]. On Android, a lifecycle-aware [RetainedValuesStore] is installed at the root
 * of the composition that retains values across configuration changes.
 *
 * If this CompositionLocal is updated, all values previously returned by [retain] will be adopted
 * to the new store and will follow the new store's retention lifecycle.
 *
 * Always prefer [LocalRetainedValuesStoreProvider] to setting this local directly. This local is
 * exposed providable as an escape hatch for installing a platform- or library-specific
 * [LocalRetainedValuesStore] at the root of the hierarchy and for testing custom
 * [RetainedValuesStore] implementations. Stores installed through this local directly will NOT
 * receive the default calls into [RetainedValuesStore.onContentEnteredComposition] and
 * [RetainedValuesStore.onContentExitComposition] provided by [LocalRetainedValuesStoreProvider].
 *
 * @see LocalRetainedValuesStoreProvider
 */
public val LocalRetainedValuesStore: ProvidableCompositionLocal<RetainedValuesStore> =
    staticCompositionLocalOf {
        ForgetfulRetainedValuesStore
    }

/**
 * Installs the given [RetainedValuesStore] over the provided [content] such that all values
 * retained in the [content] lambda are owned by [store]. When this provider is removed from
 * composition (and the content is therefore removed with it), the store will be notified to start
 * retaining exited values so that it can persist all retained values at the time the content exits
 * composition.
 *
 * Note that most [RetainedValuesStore] implementations can only be provided in one location and
 * composition at a time. Attempting to install the same store twice may lead to an error.
 *
 * @param store The [RetainedValuesStore] to install as the [LocalRetainedValuesStore]
 * @param content The Composable content that the [store] will be installed for. This content block
 *   is invoked immediately in-place.
 * @sample androidx.compose.runtime.retain.samples.retainingCollapsingContentSample
 */
@Composable
public fun LocalRetainedValuesStoreProvider(
    store: RetainedValuesStore,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRetainedValuesStore provides store, content)

    // Important: This must come AFTER the content for the underlying RememberObservers to
    // dispatch in the correct order relative to retained values from the content block.
    val composer = currentComposer
    remember(store) { RetainContentPresenceIndicator(store, composer) }
        .apply {
            // Composer isn't guaranteed to stay the same between recompositions, make sure to
            // update the reference just in case
            this.composer = composer
        }
}

private class RetainContentPresenceIndicator(
    private val store: RetainedValuesStore,
    composer: Composer,
) : RememberObserver {

    // Backed by snapshot like rememberUpdatedState to ensure that writes happen at the end
    // of composition without relying on a SideEffect, which will have the wrong timing.
    var composer by mutableStateOf(composer)

    private var didEnterComposition = false
    private var enterCompositionCancellationHandle: CancellationHandle? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override fun onRemembered() {
        enterCompositionCancellationHandle =
            composer.scheduleFrameEndCallback {
                didEnterComposition = true
                store.onContentEnteredComposition()
            }
    }

    override fun onForgotten() {
        enterCompositionCancellationHandle?.cancel()
        if (didEnterComposition) {
            store.onContentExitComposition()
            didEnterComposition = false
        }
    }

    override fun onAbandoned() {
        enterCompositionCancellationHandle?.cancel()
    }
}
