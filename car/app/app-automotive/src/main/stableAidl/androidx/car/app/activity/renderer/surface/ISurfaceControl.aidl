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

import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.car.app.serialization.Bundleable;

/**
 * Interface implemented by an off-process renderer to receive events affecting the
 * {@link SurfaceView} it renders content on.
 *
 * @hide
 */
oneway interface ISurfaceControl {
  /** Notifies that the underlying surface changed. */
  void setSurfaceWrapper(in Bundleable surfaceWrapper) = 1;

  /** Notifies that the surface received a new touch event. */
  void onTouchEvent(in MotionEvent event) = 2;

  /** Notifies that the window focus changed. */
  void onWindowFocusChanged(boolean hasFocus, boolean isInTouchMode) = 3;

  /** Notifies that the surface received a new key event. */
  void onKeyEvent(in KeyEvent event) = 4;
}
