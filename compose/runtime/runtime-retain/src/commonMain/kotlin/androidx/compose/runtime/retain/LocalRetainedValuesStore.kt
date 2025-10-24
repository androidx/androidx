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

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
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
 * RetainedValuesStores should be installed so that their tracked transiently removed content is
 * always removed from composition in the same frame (and by extension, all retained values leave
 * composition in the same frame). If the RetainedValuesStore starts retaining exited values and its
 * tracked content is removed in an arbitrary order across several recompositions, it may cause
 * retained values to be restored incorrectly if the retained values from different regions in the
 * composition have the same [currentCompositeKeyHashCode].
 */
public val LocalRetainedValuesStore: ProvidableCompositionLocal<RetainedValuesStore> =
    staticCompositionLocalOf {
        ForgetfulRetainedValuesStore
    }
