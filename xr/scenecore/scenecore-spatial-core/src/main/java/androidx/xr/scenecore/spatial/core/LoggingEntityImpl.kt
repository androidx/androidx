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

package androidx.xr.scenecore.spatial.core

import android.content.Context
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.LoggingEntity
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.impl.BaseEntity
import java.util.concurrent.Executor

/** Implementation of a RealityCore Entity that logs its function calls. */
// TODO: b/441103135 - Revaluate existence of LoggingEntity.
internal class LoggingEntityImpl(context: Context) : BaseEntity(context), LoggingEntity {

    init {
        XrLog.info { "Creating LoggingEntity." }
    }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        val pose = super<BaseEntity>.getPose(relativeTo)
        XrLog.info { "Getting Logging Entity pose: $pose relativeTo: $relativeTo" }
        return pose
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        XrLog.info { "Setting Logging Entity pose to: $pose relativeTo: $relativeTo" }
        super<BaseEntity>.setPose(pose, relativeTo)
    }

    override val activitySpacePose: Pose
        get() {
            XrLog.info { "Getting Logging Entity activitySpacePose." }
            return Pose()
        }

    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        XrLog.info {
            "Transforming pose $pose to be relative to the destination ScenePose: $destination"
        }
        return Pose()
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @Suppress("RestrictTo")
    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult {
        XrLog.info {
            "Hit testing Logging Entity with origin: $origin direction: $direction hitTestFilter: $hitTestFilter"
        }
        return HitTestResult(
            Vector3(),
            Vector3(),
            HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
            1f,
        )
    }

    override fun addChild(child: Entity) {
        XrLog.info { "Adding child Entity: $child" }
        super.addChild(child)
    }

    override fun addChildren(children: List<Entity>) {
        XrLog.info { "Adding child Entities: $children" }
        super.addChildren(children)
    }

    override var parent: Entity?
        get() {
            XrLog.info { "Getting Logging Entity parent: ${super.parent}" }
            return super.parent
        }
        set(value) {
            if (value !is LoggingEntityImpl) {
                XrLog.error { "Parent of a LoggingEntity must be a Logging entity" }
                return
            }
            XrLog.info { "Setting Logging Entity parent to: $value" }
            super.parent = value
        }

    override val children: List<Entity>
        get() {
            XrLog.info { "Getting Logging Entity children: ${super.children}" }
            return super.children
        }

    override fun addInputEventListener(executor: Executor?, listener: InputEventListener) {
        XrLog.info { "Add input consumer $listener executor $executor" }
    }

    override fun removeInputEventListener(listener: InputEventListener) {
        XrLog.info { "Remove input consumer $listener" }
    }

    override fun dispose() {
        XrLog.info { "dispose" }
    }
}
