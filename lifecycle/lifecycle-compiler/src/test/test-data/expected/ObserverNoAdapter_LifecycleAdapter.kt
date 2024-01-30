/*
 * Copyright (C) 2017 The Android Open Source Project
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
package test.library

import androidx.lifecycle.GeneratedAdapter
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MethodCallsLogger
import javax.annotation.Generated

@Generated(value = ["androidx.lifecycle.LifecycleProcessor"])
class ObserverNoAdapter_LifecycleAdapter internal constructor(receiver: ObserverNoAdapter) :
    GeneratedAdapter {
    val receiver: ObserverNoAdapter = receiver

    override fun callMethods(
        owner: LifecycleOwner,
        event: Event,
        onAny: Boolean,
        logger: MethodCallsLogger
    ) {
        val hasLogger = logger != null
        if (onAny) {
            return
        }
        if (event === Event.ON_STOP) {
            if (!hasLogger || logger.approveCall("doOnStop", 1)) {
                receiver.doOnStop()
            }
            return
        }
    }
}
