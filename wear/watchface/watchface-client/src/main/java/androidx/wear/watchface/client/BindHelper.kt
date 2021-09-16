/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.client

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.annotation.RequiresApi

/** Wrapper around [Context.bindService]. */
internal class BindHelper {
    companion object {
        /**
         * Wrapper around [Context.bindService] which uses an immediate executor where possible for
         * [serviceConnection].
         *
         * @param context The [Context] on which to call [Context.bindService]
         * @param intent The [Intent] to pass to [Context.bindService]
         * @param serviceConnection The [ServiceConnection] to pass to [Context.bindService].
         * Note on API 29 and above this will be called on a binder thread, before that it will be
         * called on the UI thread.
         * @return The result of [Context.bindService]
         */
        fun bindService(
            context: Context,
            intent: Intent,
            serviceConnection: ServiceConnection
        ): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29 lets us specify an executor to avoid a round trip via the potentially
                // congested UI thread.
                BindHelper29.bindServiceWithImmediateExecutor(
                    context,
                    intent,
                    serviceConnection
                )
            } else {
                /** Note serviceConnection will be dispatched on the UI thread. */
                context.bindService(
                    intent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
                )
            }
        }
    }
}

private class BindHelper29 {
    @RequiresApi(Build.VERSION_CODES.Q)
    companion object {
        /** Note serviceConnection will be dispatched on a binder thread. */
        fun bindServiceWithImmediateExecutor(
            context: Context,
            intent: Intent,
            serviceConnection: ServiceConnection
        ) = context.bindService(
            intent,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
            { command -> command.run() },
            serviceConnection
        )
    }
}