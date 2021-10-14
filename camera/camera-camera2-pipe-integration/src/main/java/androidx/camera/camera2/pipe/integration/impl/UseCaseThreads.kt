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

package androidx.camera.camera2.pipe.integration.impl

import androidx.annotation.RequiresApi
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor

/**
 * Collection of threads and scope(s) that have been configured and tuned.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class UseCaseThreads(
    val scope: CoroutineScope,

    val backgroundExecutor: Executor,
    val backgroundDispatcher: CoroutineDispatcher,
) {
    val sequentialExecutor = CameraXExecutors.newSequentialExecutor(backgroundExecutor)
    val sequentialDispatcher = sequentialExecutor.asCoroutineDispatcher()
    val sequentialScope = CoroutineScope(
        scope.coroutineContext + SupervisorJob() + sequentialDispatcher
    )
}
