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

package androidx.car.app;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.RequiresCarApi;

/** A callback for changes on the {@link SurfaceContainer} and its attributes. */
public interface SurfaceCallback {
    /**
     * Provides a {@link SurfaceContainer} from the host which is ready for drawing.
     *
     * <p>This method may be called multiple times if the surface changes characteristics. For
     * instance, the size or DPI may change without the underlying surface being destroyed.
     *
     * <p>This method is guaranteed to be called before any other methods on this listener.
     *
     * @param surfaceContainer the {@link SurfaceContainer} that is ready for drawing
     */
    void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer);

    /**
     * Indicates that the visible area provided by the host has changed.
     *
     * <p>The visible area may be occluded for several reasons including status bar changes,
     * overlays from other apps or dynamic UI within a template. The inset is the area currently
     * guaranteed to not be occluded by any other UI. If the app needs to show critical data, it
     * should be within the inset area.
     *
     * @param visibleArea the rectangle set to the surface area guaranteed to be visible. If {@link
     *                    Rect#isEmpty()} returns {@code true} for the visible area, then it is
     *                    currently unknown
     */
    void onVisibleAreaChanged(@NonNull Rect visibleArea);

    /**
     * Indicates that the stable area provided by the host has changed.
     *
     * <p>The visible area (see {@link #onVisibleAreaChanged} can be occluded for several reasons
     * including status bar changes, overlays from other apps or dynamic UI within the template. The
     * stable area is the visual area which will not be occluded by known dynamic content. The area
     * may change at any time, but every effort is made to keep it constant.
     *
     * @param stableArea inset rectangle of the surface space designated as stable. If {@link
     *                   Rect#isEmpty()} returns {@code true} for the stable area, then it is
     *                   currently unknown
     */
    void onStableAreaChanged(@NonNull Rect stableArea);

    /**
     * Indicates that the {@link SurfaceContainer} provided by the host will be destroyed after this
     * callback.
     *
     * @param surfaceContainer the {@link SurfaceContainer} being destroyed
     */
    void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer);

    /**
     * Provides information about a scroll touch event on the car screen.
     *
     * <p>See {@link androidx.car.app.navigation.model.NavigationTemplate} class description for
     * more details on how to receive this callback.
     *
     * @param distanceX the distance in pixels along the X axis that has been scrolled since the
     *                  last touch position during the scroll event
     * @param distanceY the distance in pixels along the Y axis that has been scrolled since the
     *                  last touch position during the scroll event
     */
    @RequiresCarApi(2)
    default void onScroll(float distanceX, float distanceY) {
    }

    /**
     * Provides information about a fling touch event on the car screen.
     *
     * <p>See {@link androidx.car.app.navigation.model.NavigationTemplate} class description for
     * more details on how to receive this callback.
     *
     * <p>This method may not be called in some car systems.
     *
     * @param velocityX the velocity of this fling measured in pixels per second along the x axis
     * @param velocityY the velocity of this fling measured in pixels per second along the y axis
     */
    @RequiresCarApi(2)
    default void onFling(float velocityX, float velocityY) {
    }

    /**
     * Provides information about a scale touch event on the car screen.
     *
     * <p>See {@link androidx.car.app.navigation.model.NavigationTemplate} class description for
     * more details on how to receive this callback.
     *
     * <p>This method may not be called in some car systems.
     *
     * @param focusX x coordinate of the focal point in pixels. A negative value indicates that
     *               the focal point is unavailable.
     * @param focusY y coordinate of the focal point in pixels. A negative value indicates that
     *               the focal point is unavailable.
     * @param scaleFactor the scaling factor from the previous state to the current
     *                    state during the scale event. This value is defined as (current state)
     *                    / (previous state)
     */
    @RequiresCarApi(2)
    default void onScale(float focusX, float focusY, float scaleFactor) {
    }
}
