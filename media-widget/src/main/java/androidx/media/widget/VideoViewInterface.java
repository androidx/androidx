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

package androidx.media.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.media2.MediaPlayer;

interface VideoViewInterface {
    /**
     * Assigns the view's surface to the given MediaPlayer instance.
     *
     * @param mp MediaPlayer
     * @return true if the surface is successfully assigned, false if not. It will fail to assign
     *         if any of MediaPlayer or surface is unavailable.
     */
    boolean assignSurfaceToMediaPlayer(MediaPlayer mp);
    void setSurfaceListener(SurfaceListener l);
    int getViewType();
    void setMediaPlayer(MediaPlayer mp);

    /**
     * Takes over oldView. It means that the MediaPlayer will start rendering on this view.
     * The visibility of oldView will be set as {@link View#GONE}. If the view doesn't have a
     * MediaPlayer instance or its surface is not available, the actual execution is deferred until
     * a MediaPlayer instance is set by {@link #setMediaPlayer} or its surface becomes available.
     * {@link SurfaceListener#onSurfaceTakeOverDone} will be called when the actual execution is
     * done.
     *
     * @param oldView The view that MediaPlayer is currently rendering on.
     */
    void takeOver(@NonNull VideoViewInterface oldView);

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
        void onSurfaceCreated(View view, int width, int height);
        void onSurfaceDestroyed(View view);
        void onSurfaceChanged(View view, int width, int height);
        void onSurfaceTakeOverDone(VideoViewInterface view);
    }
}
