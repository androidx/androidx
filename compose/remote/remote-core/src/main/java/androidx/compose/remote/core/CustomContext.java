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

package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Interface implemented by the platform's PaintContext to support rendering and lifecycle
 * management of native custom components within the RemoteCompose hierarchy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CustomContext {
    /** Property type constants */
    int TOUCH_DOWN = 0;
    int TOUCH_DRAG = 1;
    int TOUCH_UP = 2;
//    int TOUCH_CANCEL = 3; for future use
//    int TOUCH_MOVE = 4;

    /**
     * Creates a native custom component instance.
     *
     * @param id     unique component identifier
     * @param config initialization configuration parameters
     */
    void createCustom(int id, @NonNull String config);

    /**
     * Configures a property of the custom component with a String value.
     *
     * @param id    unique component identifier
     * @param type  property identifier constant
     * @param value String value to set
     */
    void configureCustom(int id, int type, @NonNull String value);

    /**
     * Configures a property of the custom component with an integer value.
     *
     * @param id    unique component identifier
     * @param type  property identifier constant
     * @param value integer value to set (e.g., raw color, size, etc.)
     */
    void configureCustom(int id, int type, int value);

    /**
     * Configures a property of the custom component with an integer value.
     *
     * @param id    unique component identifier
     * @param type  property identifier constant
     * @param value float value to set (e.g., raw color, size, etc.)
     */
    void configureCustom(int id, int type, float value);

    /**
     * Invokes native measurement on the custom component.
     * Input bounds are passed inside bounds: [minWidth, maxWidth, minHeight, maxHeight].
     * Output dimensions should be populated back into bounds: [outWidth, outHeight].
     *
     * @param id     unique component identifier
     * @param bounds float array for measurement bounds input/output
     */
    void measureCustom(int id, float @NonNull [] bounds);

    /**
     * Informs the custom component of its final layout coordinates and dimensions.
     * Bounds array structure: [x, y, width, height].
     *
     * @param id     unique component identifier
     * @param bounds float array containing resolved x, y, width, height
     */
    void layoutCustom(int id, float @NonNull [] bounds);

    /**
     * Informs the custom component of its final layout coordinates and dimensions.
     * Bounds array structure: [x, y, width, height].
     *
     * @param id   unique component identifier
     * @param type type of touch event
     * @param x    x coordinate
     * @param y    y coordinate
     */
    boolean touchCustom(int id, int type, float x, float y);

    /**
     * Renders the native custom component into the current graphics canvas.
     *
     * @param id unique component identifier
     */
    void drawCustom(int id);
}
