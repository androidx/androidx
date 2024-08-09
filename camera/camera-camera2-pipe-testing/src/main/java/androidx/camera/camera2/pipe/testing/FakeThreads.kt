/*
 * Copyright 2022 The Android Open Source Project
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

public object FakeThreads {
    public fun fromDispatcher(dispatcher: CoroutineDispatcher): Threads {
        val scope = CoroutineScope(dispatcher + CoroutineName("CXCP-TestScope"))
        return create(scope, dispatcher)
    }

    public fun fromTestScope(scope: TestScope): Threads {
        val dispatcher = StandardTestDispatcher(scope.testScheduler, "CXCP-TestScope")
        return create(scope, dispatcher)
    }

    private fun create(scope: CoroutineScope, dispatcher: CoroutineDispatcher): Threads {
        val executor = dispatcher.asExecutor()

        @Suppress("deprecation") val fakeHandler = { Handler() }

        return Threads(
            scope,
            blockingExecutor = executor,
            blockingDispatcher = dispatcher,
            backgroundExecutor = executor,
            backgroundDispatcher = dispatcher,
            lightweightExecutor = executor,
            lightweightDispatcher = dispatcher,
            camera2Handler = fakeHandler,
            camera2Executor = { executor }
        )
    }
}
