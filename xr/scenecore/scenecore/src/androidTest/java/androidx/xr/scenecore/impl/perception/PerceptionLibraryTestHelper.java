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

import android.os.IBinder;

/**
 * A library with an interface to native functions that are used to set up state for perception
 * manager tests.
 */
final class PerceptionLibraryTestHelper {

    PerceptionLibraryTestHelper() {}

    public native void reset();

    public native void setCreateSessionResult(boolean success);

    public native int getOpenXrSessionReferenceSpaceType();

    public native void setGetAnchorResult(IBinder binder, long anchorId);

    public native void setAnchorUuidBytes(byte[] anchorUuidBytes);

    public native void setAnchorPersistState(int persistState);

    public native void setGetCurrentHeadPoseResult(
            float x, float y, float z, float qx, float qy, float qz, float qw);

    public native void setLeftView(
            float x,
            float y,
            float z,
            float qx,
            float qy,
            float qz,
            float qw,
            float angleLeft,
            float angleRight,
            float angleUp,
            float angleDown);

    public native void setRightView(
            float x,
            float y,
            float z,
            float qx,
            float qy,
            float qz,
            float qw,
            float angleLeft,
            float angleRight,
            float angleUp,
            float angleDown);

    public native void addPlane(
            long plane,
            Pose centerPose,
            float extentWidth,
            float extentHeight,
            int type,
            int label);
}
