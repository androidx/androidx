/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.renderer;

import android.graphics.Canvas;
import android.graphics.Rect;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A renderer for use on templates with a surface. */
public interface Renderer {
    /**
     * Informs the renderer that it will receive {@link #renderFrame} calls.
     *
     * @param onChangeListener a runnable that will initiate a render pass in the controller
     */
    void enable(@NonNull Runnable onChangeListener);

    /** Informs the renderer that it will no longer receive {@link #renderFrame} calls. */
    void disable();

    /** Request that a frame should be drawn to the supplied canvas. */
    void renderFrame(@NonNull Canvas canvas, @Nullable Rect visibleArea, @Nullable Rect stableArea);
}
