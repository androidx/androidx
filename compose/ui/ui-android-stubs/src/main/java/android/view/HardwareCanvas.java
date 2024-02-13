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

package android.view;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.NonNull;

/**
 * Stub for HardwareCanvas on Android L
 */
public abstract class HardwareCanvas extends Canvas {

    /**
     * Draws the specified display list onto this canvas.
     *
     * @param renderNode The RenderNode to replay.
     * @param dirty Ignored, can be null.
     * @param flags Optional flags about drawing, see {@link RenderNode} for
     *              the possible flags.
     *
     * @return One of {@link RenderNode#STATUS_DONE} or {@link RenderNode#STATUS_DREW}
     *         if anything was drawn.
     */
    public abstract int drawRenderNode(
            @NonNull RenderNode renderNode,
            @NonNull Rect dirty,
            int flags
    );

    @Override
    public void enableZ() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableZ() {
        throw new UnsupportedOperationException();
    }
}
