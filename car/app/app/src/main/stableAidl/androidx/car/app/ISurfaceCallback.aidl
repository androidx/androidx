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

import androidx.car.app.serialization.Bundleable;
import androidx.car.app.IOnDoneCallback;

/** @hide */
oneway interface ISurfaceCallback {
  /**
   * Notifies the app that the surface has changed.
   */
  void onSurfaceAvailable(in Bundleable surfaceContainer, IOnDoneCallback callback) = 1;

  /**
   * Notifies the app that the visiable area has changed.
   */
  void onVisibleAreaChanged(in Rect visibleArea, IOnDoneCallback callback) = 2;

  /**
   * Notifies the app that the stable area has changed.
   */
  void onStableAreaChanged(in Rect stableArea, IOnDoneCallback callback) = 3;

  /**
   * Notifies the app that the surface has destroyed.
   */
  void onSurfaceDestroyed(in Bundleable surfaceContainer, IOnDoneCallback callback) = 4;

  /**
   * Notifies the app about a surface scroll touch event.
   */
  void onScroll(float distanceX, float distanceY) = 5;

  /**
   * Notifies the app about a surface fling touch event.
   */
  void onFling(float velocityX, float velocityY) = 6;

  /**
   * Notifies the app about a surface scale touch event.
   */
  void onScale(float focusX, float focusY, float scaleFactor) = 7;

  /**
   * Notifies the app about a surface click event.
   */
  void onClick(float x, float y) = 8;
}
