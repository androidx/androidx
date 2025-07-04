/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl.perception;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** A translation and rotation of an object (e.g. Trackable, Anchor). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Pose {
    private float mTx;
    private float mTy;
    private float mTz;
    private float mQx;
    private float mQy;
    private float mQz;
    private float mQw;

    /** Constructor which sets all components of the pose. */
    public Pose(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
        mTx = tx;
        mTy = ty;
        mTz = tz;
        mQx = qx;
        mQy = qy;
        mQz = qz;
        mQw = qw;
    }

    /** A pose at zero position and orientation. */
    public static @NonNull Pose identity() {
        return new Pose(0, 0, 0, 0, 0, 0, 1);
    }

    /** Replaces the translation components of this pose without changing the rotation. */
    public void updateTranslation(float tx, float ty, float tz) {
        mTx = tx;
        mTy = ty;
        mTz = tz;
    }

    /** Replaces the rotation components of this pose without changing the translation. */
    public void updateRotation(float qx, float qy, float qz, float qw) {
        mQx = qx;
        mQy = qy;
        mQz = qz;
        mQw = qw;
    }

    /** Returns the X components of this pose's translation. */
    public float tx() {
        return mTx;
    }

    /** Returns the Y components of this pose's translation. */
    public float ty() {
        return mTy;
    }

    /** Returns the Z components of this pose's translation. */
    public float tz() {
        return mTz;
    }

    /** Returns the X component of this pose's rotation quaternion. */
    public float qx() {
        return mQx;
    }

    /** Returns the Y component of this pose's rotation quaternion. */
    public float qy() {
        return mQy;
    }

    /** Returns the Z component of this pose's rotation quaternion. */
    public float qz() {
        return mQz;
    }

    /** Returns the W component of this pose's rotation quaternion. */
    public float qw() {
        return mQw;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Pose) {
            Pose that = (Pose) object;
            return mTx == that.mTx
                    && mTy == that.mTy
                    && mTz == that.mTz
                    && mQx == that.mQx
                    && mQy == that.mQy
                    && mQz == that.mQz
                    && mQw == that.mQw;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(mTx);
        result = 31 * result + Float.floatToIntBits(mTy);
        result = 31 * result + Float.floatToIntBits(mTz);
        result = 31 * result + Float.floatToIntBits(mQx);
        result = 31 * result + Float.floatToIntBits(mQy);
        result = 31 * result + Float.floatToIntBits(mQz);
        result = 31 * result + Float.floatToIntBits(mQw);
        return result;
    }
}
