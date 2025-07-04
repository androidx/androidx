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

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * An Anchor keeps track of a position in the real world. This can be an arbitrary position or a
 * position relative to a trackable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Anchor {
    /**
     * anchorId is an ID used as a reference to this anchor. It is equal to the XrSpace handle for
     * anchor in the OpenXR session managed by the perception library.
     */
    private final long mAnchorId;

    /**
     * anchorToken is a Binder reference of the anchor, it can be used to import the anchor by an
     * OpenXR session.
     */
    private final IBinder mAnchorToken;

    /* UUID of the anchor.*/
    private UUID mUuid;

    public Anchor(long anchorId, @NonNull IBinder anchorToken) {
        mAnchorId = anchorId;
        mAnchorToken = anchorToken;
        mUuid = null;
    }

    Anchor(AnchorData anchorData) {
        mAnchorToken = anchorData.mAnchorToken;
        mAnchorId = anchorData.mAnchorId;
        mUuid = null;
    }

    /** Returns the anchorId(native pointer) of the anchor. */
    public long getAnchorId() {
        return mAnchorId;
    }

    /**
     * Returns an IBInder token to this anchor. This is used for sharing the anchor with other
     * OpenXR sessions in other processes (such as SpaceFlinger).
     */
    public @NonNull IBinder getAnchorToken() {
        return mAnchorToken;
    }

    /**
     * Detaches the anchor. Any process which has imported this anchor will see the anchor as
     * untracked.
     */
    public boolean detach() {
        return detachAnchor(mAnchorId);
    }

    /**
     * Persists the anchor. When the anchor is persisted successfully, that means the environment
     * and the anchor's relative position to the environment is saved to storage. The anchor could
     * be recreated in the same environment in the following sessions with the original pose by
     * calling Session.createAnchor(uuid).
     *
     * @return the UUID of the anchor being persisted.
     */
    public @Nullable UUID persist() {
        byte[] uuidBytes = persistAnchor(mAnchorId);
        if (uuidBytes == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(uuidBytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        mUuid = new UUID(high, low);
        return mUuid;
    }

    private native boolean detachAnchor(long anchorId);

    private native byte[] persistAnchor(long anchorId);

    /**
     * Gets the Persistent State of the anchor. If the anchor doesn't have any persist requests yet,
     * it returns PersistState.PERSIST_NOT_REQUESTED. If the anchor was found by the uuid, it
     * returns the current persist state of the anchor.
     *
     * @return the persist state of the anchor.
     */
    public @NonNull PersistState getPersistState() {
        if (mUuid == null) {
            return PersistState.PERSIST_NOT_REQUESTED;
        }
        PersistState state =
                getPersistState(mUuid.getMostSignificantBits(), mUuid.getLeastSignificantBits());
        if (state == null) {
            return PersistState.NOT_VALID;
        }
        return state;
    }

    private native PersistState getPersistState(long highBits, long lowBits);

    /** Persistent State Enum for the anchor. It is retrieved with the getPersistState function. */
    public enum PersistState {
        PERSIST_NOT_REQUESTED,
        PERSIST_PENDING,
        PERSISTED,
        NOT_VALID,
    }

    /** Data returned from native OpenXR layer when creating an anchor. */
    static class AnchorData {
        long mAnchorId;
        IBinder mAnchorToken;
    }
}
