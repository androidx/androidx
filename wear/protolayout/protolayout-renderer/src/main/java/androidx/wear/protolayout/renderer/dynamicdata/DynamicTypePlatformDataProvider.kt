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
package androidx.wear.protolayout.renderer.dynamicdata

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.UiThread
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.expression.PlatformDataKey
import androidx.wear.protolayout.expression.PlatformDataValues
import androidx.wear.protolayout.expression.PlatformEventSources.DynamicLayoutUpdateStatus
import androidx.wear.protolayout.expression.PlatformEventSources.LayoutUpdateStatus
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver
import java.util.concurrent.Executor

/** A [PlatformDataProvider] that provides a dynamic type. */
@RestrictTo(Scope.LIBRARY_GROUP)
public class DynamicTypePlatformDataProvider<RawT, T : DynamicType>
private constructor(
    private val key: PlatformDataKey<T>,
    initialValue: RawT,
    private val transform: (RawT) -> DynamicDataValue<T>,
) : PlatformDataProvider {

    private var receiver: PlatformDataReceiver? = null
    private var executor: Executor? = null

    private var updatesEnabled: Boolean = true

    @set:UiThread
    @get:UiThread
    public var value: RawT = initialValue
        /** Sets and notifies a new value */
        set(value) {
            field = value
            notifyReceiver()
        }

    /** Sets whether this consumer can send updates on the registered receiver. */
    @UiThread
    public fun setUpdatesEnabled(updatesEnabled: Boolean) {
        if (this.updatesEnabled == updatesEnabled) {
            // Avoid a pointless update
            return
        }
        this.updatesEnabled = updatesEnabled
        notifyReceiver()
    }

    /** Registers the given receiver for receiving updates. */
    @UiThread
    override fun setReceiver(executor: Executor, receiver: PlatformDataReceiver) {
        this.executor = executor
        this.receiver = receiver
        // Send the first value to the receiver, so that initial layout has the correct state.
        notifyReceiver()
    }

    /** Clears the registered receiver. */
    @UiThread
    override fun clearReceiver() {
        this.executor = null
        this.receiver = null
    }

    /** Notifies the receiver if updates are enabled. */
    private fun notifyReceiver() {
        if (!updatesEnabled) {
            return
        }

        val data = PlatformDataValues.of(key, transform(value))
        receiver?.let { executor?.execute { it.onData(data) } }
    }

    public companion object {

        /** Creates a new [PlatformDataProvider] for a [DynamicBool]. */
        @JvmStatic
        public fun forDynamicBool(
            key: PlatformDataKey<DynamicBool>,
            initialValue: Boolean,
        ): DynamicTypePlatformDataProvider<Boolean, DynamicBool> =
            DynamicTypePlatformDataProvider(key, initialValue) { DynamicDataValue.fromBool(it) }

        /** Creates a new [PlatformDataProvider] for a [DynamicInt32]. */
        @JvmStatic
        public fun forDynamicInt32(
            key: PlatformDataKey<DynamicInt32>,
            initialValue: Int,
        ): DynamicTypePlatformDataProvider<Int, DynamicInt32> =
            DynamicTypePlatformDataProvider(key, initialValue) { DynamicDataValue.fromInt(it) }

        /** Creates a new [PlatformDataProvider] for a [DynamicLayoutUpdateStatus]. */
        @JvmStatic
        public fun forDynamicLayoutUpdateStatus(
            key: PlatformDataKey<DynamicLayoutUpdateStatus>,
            @LayoutUpdateStatus initialValue: Int,
        ): DynamicTypePlatformDataProvider<Int, DynamicLayoutUpdateStatus> =
            DynamicTypePlatformDataProvider(key, initialValue) {
                DynamicLayoutUpdateStatus.dynamicDataValueOf(it)
            }
    }
}
