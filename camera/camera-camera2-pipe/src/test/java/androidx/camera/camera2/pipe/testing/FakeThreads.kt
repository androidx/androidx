/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.os.Handler
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor

internal object FakeThreads {
    @Suppress("deprecation")
    val fakeHandler = { Handler() }
    val forTests = Threads(
        CoroutineScope(SupervisorJob() + CoroutineName("CXCP-TestScope") + Dispatchers.Default),
        blockingExecutor = Dispatchers.IO.asExecutor(),
        blockingDispatcher = Dispatchers.IO,
        backgroundExecutor = Dispatchers.Default.asExecutor(),
        backgroundDispatcher = Dispatchers.Default,
        lightweightExecutor = Dispatchers.Default.asExecutor(),
        lightweightDispatcher = Dispatchers.Default,
        camera2Handler = fakeHandler,
        camera2Executor = { Dispatchers.IO.asExecutor() }
    )

    fun fromExecutor(executor: Executor): Threads {
        return fromDispatcher(executor.asCoroutineDispatcher())
    }

    fun fromDispatcher(dispatcher: CoroutineDispatcher): Threads {
        val executor = dispatcher.asExecutor()

        @Suppress("deprecation")
        val fakeHandler = { Handler() }

        return Threads(
            CoroutineScope(dispatcher.plus(CoroutineName("CXCP-TestScope"))),
            blockingExecutor = executor,
            blockingDispatcher = dispatcher,
            backgroundExecutor = executor,
            backgroundDispatcher = dispatcher,
            lightweightExecutor = executor,
            lightweightDispatcher = dispatcher,
            camera2Handler = fakeHandler,
            camera2Executor = { dispatcher.asExecutor() }
        )
    }
}