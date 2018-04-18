/*
 * Copyright 2017 The Android Open Source Project
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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Registry for tool specific event handler. This provides map like functionality,
 * along with fallback to a default handler, while avoiding auto-boxing of tool
 * type values that would be necessitated where a Map used.
 *
 * @param <T> type of item being registered.
 */
final class ToolHandlerRegistry<T> {

    // Currently there are four known input types. ERASER is the last one, so has the
    // highest value. UNKNOWN is zero, so we add one. This allows delegates to be
    // registered by type, and avoid the auto-boxing that would be necessary were we
    // to store delegates in a Map<Integer, Delegate>.
    private static final int NUM_INPUT_TYPES = MotionEvent.TOOL_TYPE_ERASER + 1;

    private final List<T> mHandlers = Arrays.asList(null, null, null, null, null);
    private final T mDefault;

    ToolHandlerRegistry(@NonNull T defaultDelegate) {
        checkArgument(defaultDelegate != null);
        mDefault = defaultDelegate;

        // Initialize all values to null.
        for (int i = 0; i < NUM_INPUT_TYPES; i++) {
            mHandlers.set(i, null);
        }
    }

    /**
     * @param toolType
     * @param delegate the delegate, or null to unregister.
     * @throws IllegalStateException if an tooltype handler is already registered.
     */
    void set(int toolType, @Nullable T delegate) {
        checkArgument(toolType >= 0 && toolType <= MotionEvent.TOOL_TYPE_ERASER);
        checkState(mHandlers.get(toolType) == null);

        mHandlers.set(toolType, delegate);
    }

    T get(@NonNull MotionEvent e) {
        T d = mHandlers.get(e.getToolType(0));
        return d != null ? d : mDefault;
    }
}
