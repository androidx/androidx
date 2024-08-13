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

import androidx.annotation.VisibleForTesting
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

/** Collection of threads and scope(s) that have been configured and tuned. */
public class UseCaseThreads(
    public val scope: CoroutineScope,
    public val backgroundExecutor: Executor,
    public val backgroundDispatcher: CoroutineDispatcher,
) {
    public val sequentialExecutor: Executor =
        CameraXExecutors.newSequentialExecutor(backgroundExecutor)
    private val sequentialDispatcher = sequentialExecutor.asCoroutineDispatcher()
    public var sequentialScope: CoroutineScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob() + sequentialDispatcher)
        @VisibleForTesting set
}
