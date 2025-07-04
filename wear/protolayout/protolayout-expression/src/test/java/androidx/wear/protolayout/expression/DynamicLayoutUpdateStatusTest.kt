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

package androidx.wear.protolayout.expression

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DynamicLayoutUpdateStatusTest {

    @Test
    fun layoutUpdateStatus_hasCorrectKey() {
        val layoutUpdateStatus = PlatformEventSources.layoutUpdateStatus()

        assertThat(layoutUpdateStatus.toDynamicInt32Proto().stateSource.sourceKey)
            .isEqualTo(PlatformEventSources.Keys.LAYOUT_UPDATE_STATUS.key)
        assertThat(layoutUpdateStatus.toDynamicInt32Proto().stateSource.sourceNamespace)
            .isEqualTo(PlatformEventSources.Keys.LAYOUT_UPDATE_STATUS.namespace)
    }

    @Test
    fun toDynamicInt32Proto_delegatesToImpl() {
        val dynamicInt32 =
            DynamicBuilders.DynamicInt32.constant(PlatformEventSources.LAYOUT_UPDATE_WAITING)
        val dynamicLayoutUpdateStatus =
            PlatformEventSources.DynamicLayoutUpdateStatus.constant(
                PlatformEventSources.LAYOUT_UPDATE_WAITING
            )

        assertThat(dynamicLayoutUpdateStatus.toDynamicInt32Proto())
            .isEqualTo(dynamicInt32.toDynamicInt32Proto())
    }

    @Test
    fun toDynamicInt32ByteArray_delegatesToImpl() {
        val dynamicInt32 =
            DynamicBuilders.DynamicInt32.constant(PlatformEventSources.LAYOUT_UPDATE_WAITING)
        val dynamicLayoutUpdateStatus =
            PlatformEventSources.DynamicLayoutUpdateStatus.constant(
                PlatformEventSources.LAYOUT_UPDATE_WAITING
            )

        assertThat(dynamicLayoutUpdateStatus.toDynamicInt32ByteArray())
            .isEqualTo(dynamicInt32.toDynamicInt32ByteArray())
    }
}
