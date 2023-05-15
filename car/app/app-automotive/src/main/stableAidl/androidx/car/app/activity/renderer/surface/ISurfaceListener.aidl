/**
 * Copyright 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.activity.renderer.surface;

import androidx.car.app.serialization.Bundleable;

/**
 * A surface event listener interface.
 *
 * @hide
 */
oneway interface ISurfaceListener {
  /**
   * Notifies that the surface has become available.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains information on the surface that
   * has become available.
   */
  void onSurfaceAvailable(in Bundleable surfaceWrapper) = 1;

  /**
   * Notifies that the surface size has changed.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains the updated information on the
   * surface.
   */
  void onSurfaceChanged(in Bundleable surfaceWrapper) = 2;

  /**
   * Notifies that the surface is destroyed.
   *
   * @param surfaceWrapper a {@link SurfaceWrapper} that contains the updated information on the
   * surface.
   */
  void onSurfaceDestroyed(in Bundleable surfaceWrapper) = 3;
}
