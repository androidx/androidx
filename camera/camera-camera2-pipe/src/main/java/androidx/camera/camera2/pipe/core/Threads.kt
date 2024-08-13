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

package androidx.camera.camera2.pipe.core

import android.os.Handler
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * This collection pre-configured executors, dispatchers, and scopes that are used throughout this
 * library.
 */
public class Threads(
    public val globalScope: CoroutineScope,
    public val blockingExecutor: Executor,
    public val blockingDispatcher: CoroutineDispatcher,
    public val backgroundExecutor: Executor,
    public val backgroundDispatcher: CoroutineDispatcher,
    public val lightweightExecutor: Executor,
    public val lightweightDispatcher: CoroutineDispatcher,
    camera2Handler: () -> Handler,
    camera2Executor: () -> Executor
) {
    private val _camera2Handler = lazy { camera2Handler() }
    private val _camera2Executor = lazy { camera2Executor() }

    public val camera2Handler: Handler
        get() = _camera2Handler.value

    public val camera2Executor: Executor
        get() = _camera2Executor.value
}
