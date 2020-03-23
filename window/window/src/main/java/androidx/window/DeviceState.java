/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Information about the state of the device.
 * <p>Currently only includes the description of the state for foldable devices.
 */
public final class DeviceState {

    @Posture
    private int mPosture;

    /**
     * Unknown state of the device. May mean that either this device doesn't support different
     * postures or doesn't provide any information about its state at all.
     */
    public static final int POSTURE_UNKNOWN = 0;

    /**
     * The foldable device is closed, its primary screen area is not accessible.
     */
    public static final int POSTURE_CLOSED = 1;

    /**
     * The foldable device's hinge is in an intermediate position between opened and closed state,
     * there is a non-flat angle between parts of the flexible screen or between physical screen
     * panels.
     *
     *  |
     *  |
     *  |
     *  |
     *  |
     *  *________________
     */
    public static final int POSTURE_HALF_OPENED = 2;

    /**
     * The foldable device is completely open, the screen space that is presented to the user is
     * flat.
     *
     * ________________ ________________
     *                 *
     */
    public static final int POSTURE_OPENED = 3;

    /**
     * The foldable device is flipped with the flexible screen parts or physical screens facing
     * opposite directions.
     *
     *   ________________
     *  *________________
     */
    public static final int POSTURE_FLIPPED = 4;

    static final int POSTURE_MAX_KNOWN = POSTURE_FLIPPED;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            POSTURE_UNKNOWN,
            POSTURE_CLOSED,
            POSTURE_HALF_OPENED,
            POSTURE_OPENED,
            POSTURE_FLIPPED
    })
    @interface Posture{}

    DeviceState(@Posture int posture) {
        mPosture = posture;
    }

    /**
     * Gets the posture of a foldable device with a flexible screen or multiple physical screens.
     * Devices with a single rigid display will always report {@link #POSTURE_UNKNOWN}.
     */
    @Posture
    public int getPosture() {
        return mPosture;
    }

    private static String postureToString(@Posture int posture) {
        switch (posture) {
            case POSTURE_UNKNOWN:
                return "UNKNOWN";
            case POSTURE_CLOSED:
                return "CLOSED";
            case POSTURE_HALF_OPENED:
                return "HALF_OPENED";
            case POSTURE_OPENED:
                return "OPENED";
            case POSTURE_FLIPPED:
                return "FLIPPED";
            default:
                return "Unknown posture value (" + posture + ")";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "DeviceState{ posture=" + postureToString(mPosture) + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceState that = (DeviceState) o;

        return mPosture == that.mPosture;
    }

    @Override
    public int hashCode() {
        return mPosture;
    }

    /**
     * Builder for {@link DeviceState} objects.
     */
    public static class Builder {
        @Posture
        private int mPosture;

        /**
         * Creates an initially empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the posture for the {@link DeviceState} instance.
         */
        @NonNull
        public Builder setPosture(@Posture int posture) {
            mPosture = posture;
            return this;
        }

        /**
         * Creates a {@link DeviceState} instance with the specified fields.
         * @return A DeviceState instance.
         */
        @NonNull
        public DeviceState build() {
            return new DeviceState(mPosture);
        }
    }
}
