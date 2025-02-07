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

import android.view.MotionEvent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for event handlers. This is keyed by a ToolSourceKey which allows for searching for a
 * handler which handles both a specific SOURCE + TOOL and if none exists, will fall back to
 * TOOL only handlers. If none exists, a default handler will be returned instead.
 *
 * <p>ToolHandlerRegistry guarantees that it will never return a null handler ensuring
 * client code isn't peppered with null checks. To that end a default handler
 * is required. This default handler will be returned when a handler matching
 * the event ToolSourceKey has not be registered using
 * {@link ToolSourceHandlerRegistry#set(ToolSourceKey, T)}.
 *
 * @param <T> type of item being registered.
 */
final class ToolSourceHandlerRegistry<T> {

    /**
     * A map that is keyed by a ToolSourceKey which contains either a TOOL + SOURCE or just a
     * TOOL. This allows for handlers to get routed to more specific handlers before falling back
     * to less specific and finally the default one.
     */

    private final Map<ToolSourceKey, T> mHandlers = new HashMap<ToolSourceKey, T>();

    private final T mDefault;

    ToolSourceHandlerRegistry(@NonNull T defaultDelegate) {
        checkArgument(defaultDelegate != null);
        mDefault = defaultDelegate;
    }

    /**
     * @param delegate the delegate, or null to unregister.
     * @throws IllegalStateException if a key already has a registered handler.
     */
    void set(@NonNull ToolSourceKey key, @Nullable T delegate) {
        if (delegate == null && mHandlers.containsKey(key)) {
            mHandlers.remove(key);
            return;
        }

        mHandlers.put(key, delegate);
    }

    T get(@NonNull MotionEvent e) {
        ToolSourceKey key = ToolSourceKey.fromMotionEvent(e);
        T d = mHandlers.get(key);
        if (d == null) {
            // If the map of handlers doesn't contain a specific MotionEventKey(tool, source)
            // then fallback to the less specific MotionEventKey(tool).
            d = mHandlers.get(new ToolSourceKey(key.getToolType()));
        }
        return d != null ? d : mDefault;
    }
}
