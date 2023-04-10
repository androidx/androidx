/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.testing.spec

import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.values.EntityValue

interface Session : BaseSession<Arguments, Output> {

    fun getRequiredEntityListener(): AppEntityListener<EntityValue>? = null

    companion object {
        @JvmStatic
        val DEFAULT: Session = object : Session {
            override fun onFinishAsync(arguments: Arguments) =
                Futures.immediateFuture(
                    ExecutionResult.Builder<Output>().build(),
                )
        }
    }
}
