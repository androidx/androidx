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

package androidx.car.app.aaos.renderer;

/**
 * A lifecycle event listener interface.
 *
 * @hide
 */
interface ILifecycleListener {

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onCreate()}.
   */
  void onCreate() = 1;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onStart()}.
   */
  void onStart() = 2;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onResume()}.
   */
  void onResume() = 3;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onPause()}.
   */
  void onPause() = 4;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onStop()}.
   */
  void onStop() = 5;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onDestroyed()}.
   */
  void onDestroyed() = 6;
}
