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

import static java.util.stream.Collectors.toCollection;

import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/** A perception session is used to manage and call into the OpenXR session */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Session {
    /** Java constant representation of OpenXR's XR_NULL_HANDLE. */
    public static final long XR_NULL_HANDLE = 0;

    private static final String TAG = "PerceptionSession";
    Activity mActivity;
    @PerceptionLibraryConstants.OpenXrSpaceType int mOpenXrReferenceSpaceType;
    Executor mExecutor;
    HashMap<Long, Plane> mFoundPlanes = new HashMap<>();

    Session(
            Activity activity,
            @PerceptionLibraryConstants.OpenXrSpaceType int openXrReferenceSpaceType,
            Executor executor) {
        mActivity = activity;
        mOpenXrReferenceSpaceType = openXrReferenceSpaceType;
        mExecutor = executor;
    }

    boolean initSession() {
        boolean xrLoaded = createOpenXrSession(mActivity, mOpenXrReferenceSpaceType);
        if (!xrLoaded) {
            Log.e(TAG, "Failed to load OpenXR session.");
            return false;
        }
        return true;
    }

    /** Creates an anchor for the specified plane type. */
    public @Nullable Anchor createAnchor(
            float minWidth, float minHeight, Plane.@NonNull Type type, Plane.@NonNull Label label) {
        Anchor.AnchorData anchorData =
                getAnchor(minWidth, minHeight, type.intValue, label.intValue);
        if (anchorData == null) {
            Log.i(TAG, "Failed to create an anchor.");
            return null;
        }
        Log.i(TAG, "Creating an anchor result:" + anchorData.mAnchorToken);
        return new Anchor(anchorData);
    }

    /**
     * Recreates a persisted anchor for the specified UUID from the storage.
     *
     * @return the anchor if the operation succeeds.
     */
    public @Nullable Anchor createAnchorFromUuid(@Nullable UUID uuid) {
        if (uuid == null) {
            Log.i(TAG, "UUID is null and cannot create a persisted anchor.");
            return null;
        }
        Anchor.AnchorData anchorData =
                createPersistedAnchor(
                        uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        if (anchorData == null) {
            Log.i(TAG, "Failed to create a persisted anchor.");
            return null;
        }
        Log.i(TAG, "Creating a persisted anchor result:" + anchorData.mAnchorToken);
        return new Anchor(anchorData);
    }

    /**
     * Returns all planes that can be found in the scene. The order is not guaranteed to be
     * consistent. An anchor can be created from a plane object and it will be tied to that plane.
     */
    public @NonNull List<Plane> getAllPlanes() {
        return getPlanes().stream()
                .map(
                        nativeId ->
                                mFoundPlanes.computeIfAbsent(
                                        nativeId,
                                        id -> new Plane(nativeId, mOpenXrReferenceSpaceType)))
                .collect(toCollection(ArrayList::new));
    }

    /** Returns the current head pose using the current timestamp in OpenXR. */
    public @Nullable Pose getHeadPose() {
        Pose pose = getCurrentHeadPose();
        if (pose == null) {
            Log.w(TAG, "Failed to get the head pose.");
            return null;
        }
        return pose;
    }

    /**
     * Returns the left and right views mapping to the left and right eyes using the current
     * timestamp from OpenXR.
     */
    public @Nullable ViewProjections getStereoViews() {
        Pair<ViewProjection, ViewProjection> stereoViews = getCurrentStereoViews();
        if (stereoViews == null) {
            Log.w(TAG, "Failed to get stereo views.");
            return null;
        }
        return new ViewProjections(stereoViews.first, stereoViews.second);
    }

    /** Get the underlying OpenXR XrSession handle. */
    public native long getNativeSession();

    /** Get the underlying OpenXR XrInstance handle. */
    public native long getNativeInstance();

    private native List<Long> getPlanes();

    private native Pose getCurrentHeadPose();

    private native Pair<ViewProjection, ViewProjection> getCurrentStereoViews();

    private native boolean createOpenXrSession(Activity activity, int referenceSpaceType);

    private native Anchor.AnchorData getAnchor(
            float minWidth, float minHeight, int type, int label);

    private native Anchor.AnchorData createPersistedAnchor(long highBits, long lowBits);

    /**
     * Unpersists an anchor for the specified UUID.
     *
     * @return whether the unpersist operation succeeds or not.
     */
    public boolean unpersistAnchor(@Nullable UUID uuid) {
        if (uuid == null) {
            Log.i(TAG, "UUID is null and cannot unpersist the anchor.");
            return false;
        }
        return unpersistAnchor(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    private native boolean unpersistAnchor(long highBits, long lowBits);
}
