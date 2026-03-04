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

package androidx.xr.scenecore.testapp.surfaceinteraction

import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.InputEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class SphericalVideoInputHandler(val parent: Entity, val radius: Float, player: ExoPlayer) :
    ClickVideoInputHandler(player) {
    private var draggingPointer: InputEvent.Pointer? = null
    private var dragStartHitRot = Quaternion.Identity
    private var dragStartParentRot = Quaternion.Identity

    override fun canDrag(): Boolean {
        return true
    }

    override fun onDragStart(
        pointerType: InputEvent.Pointer,
        downOrigin: Vector3,
        downDirection: Vector3,
    ) {
        if (draggingPointer != null) return
        val hitRot = hitRot(downOrigin, downDirection)
        if (hitRot == null) return
        dragStartHitRot = hitRot
        dragStartParentRot = parent.getPose().rotation
        draggingPointer = pointerType
    }

    override fun onDragMove(pointerType: InputEvent.Pointer, origin: Vector3, direction: Vector3) {
        if (draggingPointer != pointerType) return
        val hitRot = hitRot(origin, direction)
        if (hitRot == null) return
        val parentPose = parent.getPose()
        parent.setPose(
            Pose(
                parentPose.translation,
                Quaternion.fromRotation(dragStartHitRot, hitRot) * dragStartParentRot,
            )
        )
    }

    override fun onDragEnd(pointerType: InputEvent.Pointer, origin: Vector3, direction: Vector3) {
        if (draggingPointer != pointerType) return
        draggingPointer = null
    }

    override fun onDragCanceled(pointerType: InputEvent.Pointer) {
        if (draggingPointer != pointerType) return
        draggingPointer = null
    }

    private fun hitRot(originWorld: Vector3, directionWorld: Vector3): Quaternion? {
        val originLocal = originWorld - parent.getPose().translation
        val directionLocal = directionWorld.toNormalized()

        // Coefficients for the quadratic equation at^2 + bt + c = 0
        // a = direction · direction (which is 1.0 if direction is normalized)
        val a = directionLocal.dot(directionLocal)
        // b = 2 * (direction · oc)
        val b = 2.0f * directionLocal.dot(originLocal)
        // c = oc · oc - radius^2
        val c = originLocal.dot(originLocal) - radius * radius

        // Calculate the discriminant of the quadratic equation.
        // discriminant = b^2 - 4ac
        val discriminant = b * b - 4.0f * a * c

        // If the discriminant is negative, the ray does not intersect the sphere.
        if (discriminant < 0) return null

        // Calculate the two potential solutions (distances 't') for the intersection.
        val sqrtDiscriminant = sqrt(discriminant)
        val t1 = (-b - sqrtDiscriminant) / (2.0f * a)
        val t2 = (-b + sqrtDiscriminant) / (2.0f * a)

        // return "farthest" point, prefer inner face of the sphere
        val t =
            if (t1 >= 0 && t2 >= 0) max(t1, t2)
            else if (t1 >= 0) t1 // origin is inside the sphere
            else if (t2 >= 0) t2 // origin is inside the sphere
            else return null // Both intersection are behind the ray

        val hit = originLocal + directionLocal * t
        // Ignore hitting north or south pole
        if (radius - abs(hit.y) < 1e-6f) return null

        return Quaternion.fromLookTowards(Vector3(hit.x, 0F, hit.z), Vector3.Up)
    }
}
