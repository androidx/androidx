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

package androidx.compose.remote.core.input;

import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.events.Event;

import org.jspecify.annotations.NonNull;

/**
 * Represents a pointer input event dispatched through the pointer router.
 *
 * <p>Event data layout: {@code [pointerId, x, y, velocityX, velocityY]}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PointerInputEvent implements Event {

    private int mMetadata;
    private final float[] mData;

    /**
     * Constructs a pointer input event with metadata and payload data.
     *
     * @param metadata packed metadata containing pass, subtype, and consumed flag
     * @param data payload data array
     */
    private PointerInputEvent(int metadata, float[] data) {
        mMetadata = metadata;
        mData = data;
    }

    /**
     * Obtains a new pointer input event instance.
     *
     * @return initialized pointer input event
     */
    public static @NonNull PointerInputEvent obtain() {
        // TODO(b/585250060): In the future, create a pool of events that can be reused.
        return new PointerInputEvent(0, new float[5]);
    }

    /**
     * Returns the event type identifying pointer events.
     *
     * @return integer event type
     */
    @Override
    public int getType() {
        return EVENT_TYPE_POINTER;
    }

    /**
     * Returns the event metadata.
     *
     * @return integer metadata
     */
    @Override
    public int getMetadata() {
        return mMetadata;
    }

    /**
     * Returns whether this event has been marked as consumed.
     *
     * @return true if consumed, false otherwise
     */
    public boolean isConsumed() {
        return MetadataUtil.unpackConsumed(mMetadata) == POINTER_STATUS_CONSUMED;
    }

    /**
     * Marks this event as consumed.
     */
    public void markConsumed() {
        mMetadata = MetadataUtil.updateConsumed(mMetadata, POINTER_STATUS_CONSUMED);
    }

    /**
     * Sets the dispatch pass in the event metadata.
     *
     * @param pass dispatch pass value
     */
    public void setPass(int pass) {
        mMetadata = MetadataUtil.updatePass(mMetadata, pass);
    }

    /**
     * Sets the pointer action subtype in the event metadata.
     *
     * @param subtype pointer action subtype
     */
    public void setSubtype(int subtype) {
        mMetadata = MetadataUtil.updateSubtype(mMetadata, subtype);
    }

    /**
     * Returns the pointer action subtype extracted from event metadata.
     *
     * @return pointer action subtype
     */
    public int getSubtype() {
        return MetadataUtil.unpackSubtype(mMetadata);
    }

    /**
     * Returns the id of the pointer.
     *
     * @return pointer id, from 0 to {@link HitPathTracker#MAX_POINTER_ID}
     * @throws IllegalArgumentException if the pointer id is outside of that range
     */
    public int getPointerId() {
        int pointerId = (int) mData[0];
        if (pointerId < 0 || pointerId > HitPathTracker.MAX_POINTER_ID) {
            throw new IllegalArgumentException(
                    "Pointer id out of bounds(0-"
                            + HitPathTracker.MAX_POINTER_ID
                            + "): "
                            + pointerId);
        }
        return pointerId;
    }

    /**
     * Returns the x position of the pointer.
     *
     * @return x position in pixels
     */
    public float getX() {
        return mData[1];
    }

    /**
     * Returns the y position of the pointer.
     *
     * @return y position in pixels
     */
    public float getY() {
        return mData[2];
    }

    /**
     * Sets the position of the pointer. The router uses this to move the event into the coordinates
     * of each component before it gives the event to the handlers of that component.
     *
     * @param x x position in pixels
     * @param y y position in pixels
     */
    void setPosition(float x, float y) {
        mData[1] = x;
        mData[2] = y;
    }

    /**
     * Returns the event data payload.
     *
     * @return float array payload
     */
    @Override
    public float @NonNull [] getData() {
        return mData;
    }
}
