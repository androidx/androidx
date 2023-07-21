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

import android.view.inputmethod.EditorInfo;

import androidx.car.app.activity.renderer.IProxyInputConnection;

/**
 * Interface to events relevant to remote rendering.
 *
 * @hide
 */
interface IRendererCallback {
  /** Notifies that the button was pressed. */
  void onBackPressed() = 1;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onCreate()}.
   */
  void onCreate() = 2;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onStart()}.
   */
  void onStart() = 3;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onResume()}.
   */
  void onResume() = 4;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onPause()}.
   */
  void onPause() = 5;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onStop()}.
   */
  void onStop() = 6;

  /**
   * Notifies that {@link CarAppActivity} called {Activity#onDestroyed()}.
   */
  void onDestroyed() = 7;

  /**
   * Creates a proxy to a remote {@link InputConnection}.
   *
   * @params editorInfo the {@link EditorInfo} for which the input connection should be created
   *
   * @return an {@link IProxyInputConnection} through which communication to the
   *   remote {@code InputConnection} should occur
   */
  IProxyInputConnection onCreateInputConnection(in EditorInfo editorInfo) = 8;
}

