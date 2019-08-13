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
 * type values that would be necessitated were a Map used.k
 *
 * <p>ToolHandlerRegistry guarantees that it will never return a null handler ensuring
 * client code isn't peppered with null checks. To that end a default handler
 * is required. This default handler will be returned when a handler matching
 * the event tooltype has not be registered using {@link #set(int, T)}.
 *
 * @param <T> type of item being registered.
 */
final class ToolHandlerRegistry<T> {

    // list with one null entry for each known tooltype (0-4).
    // See MotionEvent.TOOL_TYPE_ERASER for details. We're using a list here because
    // it is parameterized type friendly, and a natural container given that
    // the index values are 0-based ints.
    private final List<T> mHandlers = Arrays.asList(null, null, null, null, null);
    private final T mDefault;

    ToolHandlerRegistry(@NonNull T defaultDelegate) {
        checkArgument(defaultDelegate != null);
        mDefault = defaultDelegate;
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
