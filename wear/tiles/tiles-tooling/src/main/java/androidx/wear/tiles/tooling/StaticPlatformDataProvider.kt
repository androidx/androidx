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

package androidx.wear.tiles.tooling

import androidx.wear.protolayout.expression.PlatformDataValues
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver
import java.util.concurrent.Executor

/** A [PlatformDataProvider] that provides [values] as static data. */
internal class StaticPlatformDataProvider(private val values: PlatformDataValues) :
    PlatformDataProvider {

    private var receiver: PlatformDataReceiver? = null

    override fun setReceiver(executor: Executor, receiver: PlatformDataReceiver) {
        this.receiver = receiver
        executor.execute { receiver.onData(values) }
    }

    override fun clearReceiver() {
        receiver?.onInvalidated(values.all.keys)
        receiver = null
    }
}
