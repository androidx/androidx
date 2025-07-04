/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.InactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.UseCase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Deferred

class FakeUseCaseSurfaceManager(
    threads: UseCaseThreads,
    cameraPipe: CameraPipe =
        CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
    inactiveSurfaceCloser: InactiveSurfaceCloser = NoOpInactiveSurfaceCloser,
    useCases: List<UseCase> = emptyList(),
    private val isSurfaceSetupSuccessful: Deferred<Boolean>? = null,
) :
    UseCaseSurfaceManager(
        threads,
        cameraPipe,
        inactiveSurfaceCloser,
        SessionConfigAdapter(useCases = useCases),
    ) {
    override suspend fun awaitSetupCompletion(): Boolean {
        if (isSurfaceSetupSuccessful == null) {
            return super.awaitSetupCompletion()
        }
        return isSurfaceSetupSuccessful.await()
    }
}
