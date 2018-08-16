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

package androidx.ui.services

import androidx.ui.engine.window.AppLifecycleState
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel

/**
 * Platform channels used by the Flutter system.
 */
// TODO(Migration/Andrey): Port other channels
class SystemChannels {

    companion object {

        /**
         * A BroadcastChannel for lifecycle events.
         *
         * See also:
         *
         *  * [WidgetsBindingObserver.didChangeAppLifecycleState], which triggers
         *    whenever a message is received on this channel.
         */
        val lifecycle: BroadcastChannel<AppLifecycleState> = BroadcastChannel(Channel.CONFLATED)
    }
}