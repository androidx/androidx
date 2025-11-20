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

package androidx.xr.scenecore.spatial.core;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;

/** Utility functions for working with planes Poses. */
final class PlaneUtils {
    private PlaneUtils() {}

    /**
     * Gets the rotation relative to the plane to rotate the entity to be parallel to the plane.
     *
     * @param proposedRotation the initial rotation of the entity.
     * @param planeRotation the rotation of the plane.
     * @return the rotation of the panel rotated to be parallel to the plane relative to the plane.
     */
    static Quaternion rotateEntityToPlane(Quaternion proposedRotation, Quaternion planeRotation) {
        // The y-vector of the plane is the normal of the plane. We need to rotate the panel so that
        // the y-vector of the panel points along the plane and the z-vector is normal to the plane.
        // Otherwise the panel will be sticking out of the plane.

        // Create a rotation matrix from the quaternion of the plane to extract the normal.
        Matrix4 planeMatrix = Matrix4.fromQuaternion(planeRotation);
        // Create a rotation matrix from the quaternion for the proposed pose.
        Matrix4 proposedRotationMatrix = Matrix4.fromQuaternion(proposedRotation);

        // The z-vector of the panel should be the normal of the plane (which is the y-vector of the
        // plane) so that the panel will be facing out of the plane.
        float[] planeMatrixData = planeMatrix.getData();
        Vector3 zRotation =
                new Vector3(planeMatrixData[4], planeMatrixData[5], planeMatrixData[6])
                        .toNormalized();
        // Get the x-vector of the panel so that we can use it to create the y-vector that is in the
        // direction of the panel.
        float[] poseMatrixData = proposedRotationMatrix.getData();
        Vector3 poseVectorX =
                new Vector3(poseMatrixData[0], poseMatrixData[1], poseMatrixData[2]).toNormalized();
        // The y-vector is the cross product of the panel x-vector and the z-vector.
        Vector3 yRotation = zRotation.cross(poseVectorX).toNormalized();
        // The x-vector is the cross product of the y-vector and the z-vector so that they will all
        // be orthogonal.
        Vector3 xRotation = yRotation.cross(zRotation).toNormalized();
        // Create a new rotation matrix from the x, y, and z vectors.
        Matrix4 rotationMatrix = getRotationMatrixFromAxes(xRotation, yRotation, zRotation);
        return rotationMatrix.getRotation();
    }

    private static Matrix4 getRotationMatrixFromAxes(Vector3 xAxis, Vector3 yAxis, Vector3 zAxis) {
        return new Matrix4(
                new float[] {
                    xAxis.getX(),
                    xAxis.getY(),
                    xAxis.getZ(),
                    0f,
                    yAxis.getX(),
                    yAxis.getY(),
                    yAxis.getZ(),
                    0f,
                    zAxis.getX(),
                    zAxis.getY(),
                    zAxis.getZ(),
                    0f,
                    0f,
                    0f,
                    0f,
                    1f
                });
    }
}
