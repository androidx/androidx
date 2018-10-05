/*
 * Copyright 2018 The Android Open Source Project
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

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.scheduler.binding

import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError

class _FrameCallbackEntry(
    val callback: FrameCallback,
    val rescheduling: Boolean = false
) {

    var debugStack: Any = Unit // TODO(Migration/Andrey): StackTrace originally. port it first

    init {
        assert {
            if (rescheduling) {
                assert {
                    if (debugCurrentCallbackStack == null) {
                        throw FlutterError("scheduleFrameCallback called with rescheduling true, " +
                                "but no callback is in scope.\n" +
                                "The 'rescheduling' argument should only be set to true if the " +
                                "callback is being reregistered from within the callback itself, " +
                                "and only then if the callback itself is entirely synchronous. " +
                                "If this is the initial registration of the callback, or if the " +
                                "callback is asynchronous, then do not use the 'rescheduling' " +
                                "argument."
                        )
                    }
                    true
                }
                debugStack = debugCurrentCallbackStack!!
            } else {
                // TODO(ianh): trim the frames from this library, so that the call to scheduleFrameCallback is the top one
                // TODO("Migration/Andrey: Needs StackTrace")
//                debugStack = StackTrace.current;
            }
            true
        }
    }

    companion object {
        var debugCurrentCallbackStack: Any? = null // TODO(Migration/Andrey): StackTrace originally
    }
}