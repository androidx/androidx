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

package androidx.car.app.activity.renderer;

import android.graphics.Insets;

/**
 * Interface to events relevant to remote rendering.
 *
 * @hide
 */
interface IInsetsListener {
  /**
   * Notifies that the {@link Insets} have changed.
   *
   * @deprecated Use onWindowInsetsChanged(Insets, Insets) instead.
   */
  void onInsetsChanged(in Insets insets) = 1;

  /**
   * Notifies that the {@link Insets} of the window have changed.
   */
  void onWindowInsetsChanged(in Insets insets, in Insets safeInsets) = 2;

}

