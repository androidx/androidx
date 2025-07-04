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

package androidx.recyclerview.selection;

import static android.view.InputDevice.SOURCE_MOUSE;
import static android.view.InputDevice.SOURCE_UNKNOWN;
import static android.view.MotionEvent.TOOL_TYPE_ERASER;
import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_MOUSE;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.MotionEvent.TOOL_TYPE_UNKNOWN;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Enables storing multiple {@link MotionEvent} parameters (e.g.
 * {@link MotionEvent#getToolType(int)}) as a key in a map. This opens up the ability to map
 * these multiple parameters against their respective handlers. For example some events behave
 * differently based on their toolType and source where others just require toolType.
 */
@RestrictTo(LIBRARY)
public class ToolSourceKey {
    private final @ToolType int mToolType;
    private final @Source int mSource;

    ToolSourceKey(@ToolType int toolType) {
        mToolType = toolType;
        mSource = InputDevice.SOURCE_UNKNOWN;
    }

    ToolSourceKey(@ToolType int toolType, @Source int source) {
        mToolType = toolType;
        mSource = source;
    }

    /**
     * Create a `ToolSourceKey` from a supplied `MotionEvent`.
     *
     * @return {@link ToolSourceKey}
     */
    public static @NonNull ToolSourceKey fromMotionEvent(@NonNull MotionEvent e) {
        return new ToolSourceKey(e.getToolType(0), e.getSource());
    }

    public int getToolType() {
        return mToolType;
    }

    public int getSource() {
        return mSource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mToolType, mSource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ToolSourceKey)) {
            return false;
        }
        ToolSourceKey matcher = (ToolSourceKey) obj;
        return mToolType == matcher.getToolType() && mSource == matcher.getSource();
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(mToolType) + "," + String.valueOf(mSource);
    }

    @IntDef(value = {TOOL_TYPE_FINGER, TOOL_TYPE_MOUSE, TOOL_TYPE_ERASER, TOOL_TYPE_STYLUS,
            TOOL_TYPE_UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    @interface ToolType {
    }

    /**
     * Please add additional sources here from InputDevice.SOURCE_*.
     */
    @IntDef(value = {SOURCE_MOUSE, SOURCE_UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    @interface Source {
    }
}
