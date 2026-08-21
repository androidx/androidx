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

package androidx.xr.compose.subspace.animation.follow

import androidx.annotation.RestrictTo
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import kotlinx.coroutines.withContext

/**
 * This is the implementation for TightFollowing which is accessible through the public interface as
 * FollowMode.tight
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object TightFollowMode : FollowMode() {
    override suspend fun start(
        session: Session,
        trailingEntity: CoreGroupEntity,
        target: FollowTarget,
        dimensions: TrackedDimensions,
    ) {
        var isInitialized: Boolean = false

        if (target is FollowTargetFlow) {
            withContext(dispatcherOverride) {
                target.poseUpdates(session).collect { pose ->
                    trailingEntity.poseInMeters = pose
                    if (!isInitialized) {
                        trailingEntity.enabled = true
                        isInitialized = true
                    }
                }
            }
        }
    }
}
