/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.projected.testing

import android.content.Context
import androidx.xr.scenecore.projected.IProjectedSceneCoreService
import androidx.xr.scenecore.projected.ProjectedSceneCoreServiceClient

public class FakeProjectedSceneCoreServiceClient : ProjectedSceneCoreServiceClient() {

    public var isBound: Boolean = false
        private set

    init {
        // a bit non-standard to initialize it non-null but massively increases usability in tests
        service = FakeProjectedSceneCoreService()
    }

    public val fakeService: FakeProjectedSceneCoreService
        get() = service as FakeProjectedSceneCoreService

    override suspend fun bindService(context: Context): IProjectedSceneCoreService {
        isBound = true
        if (service == null) {
            service = FakeProjectedSceneCoreService()
        }
        return service!!
    }

    override fun unbindService() {
        isBound = false
        service = null
    }
}
