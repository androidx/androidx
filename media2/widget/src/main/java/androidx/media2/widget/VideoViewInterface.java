/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2.widget;

import android.view.View;

import androidx.annotation.NonNull;

interface VideoViewInterface {
    /**
     * Assigns the view's surface to the given PlayerWrapper instance.
     *
     * @param player PlayerWrapper
     * @return true if the surface is successfully assigned, false if not. It will fail to assign
     *         if any of PlayerWrapper or surface is unavailable.
     */
    boolean assignSurfaceToPlayerWrapper(PlayerWrapper player);
    void setSurfaceListener(SurfaceListener l);
    int getViewType();
    void setPlayerWrapper(PlayerWrapper player);

    /**
     * Takes over oldView. It means that the PlayerWrapper will start rendering on this view.
     * If the view doesn't have a PlayerWrapper instance or its surface is not available,
     * the actual execution is deferred until a PlayerWrapper instance is set
     * by {@link #setPlayerWrapper} or its surface becomes available.
     * {@link SurfaceListener#onSurfaceTakeOverDone} will be called when the actual execution is
     * done.
     */
    void takeOver();

    /**
     * Indicates if the view's surface is available.
     *
     * @return true if the surface is available.
     */
    boolean hasAvailableSurface();

    /**
     * An instance of VideoViewInterface calls these surface notification methods accordingly if
     * a listener has been registered via {@link #setSurfaceListener(SurfaceListener)}.
     */
    interface SurfaceListener {
        void onSurfaceCreated(@NonNull View view, int width, int height);
        void onSurfaceDestroyed(@NonNull View view);
        void onSurfaceChanged(@NonNull View view, int width, int height);
        void onSurfaceTakeOverDone(@NonNull VideoViewInterface view);
    }
}
