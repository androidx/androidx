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

import android.content.Intent;
import android.content.res.Configuration;
import androidx.car.app.ICarHost;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.serialization.Bundleable;

/** @hide */
oneway interface ICarApp {
  /**
   * Initializes app.
   *
   * @param host the binder for performing two-way communication.
   * @param intent the intent that was given to start the app.
   * @param configuration the current car screen's configuration.
   */
  void onAppCreate(ICarHost host, in Intent intent, in Configuration configuration,
          IOnDoneCallback callback) = 1;

  /** Notifies that the app is now visible. */
  void onAppStart(IOnDoneCallback callback) = 2;

  /** Notifies that the app is now actively running. */
  void onAppResume(IOnDoneCallback callback) = 3;

  /** Notifies that the app is not actively running but still visible. */
  void onAppPause(IOnDoneCallback callback) = 4;

  /** Notifies that the app is no longer visible. */
  void onAppStop(IOnDoneCallback callback) = 5;

  /** Provides a new intent for the app. */
  void onNewIntent(in Intent intent, IOnDoneCallback callback) = 6;

  /** Provides a new configuration for the app. */
  void onConfigurationChanged(in Configuration configuration, IOnDoneCallback callback) = 7;

  /**
   * Requests the manager binder corresponding to the {@code type} to be
   * returned via the {@code callback}.
   *
   * <p>type is a @CarContext#CarAppService.
   */
  void getManager(in String type, IOnDoneCallback callback) = 8;

  /**
   * Requests information of the application (min API level, target API level, etc.).
   */
  void getAppInfo(IOnDoneCallback callback) = 9;

  /**
   * Sends host information and negotiated API level to the app.
   */
  void onHandshakeCompleted(in Bundleable handshakeInfo, IOnDoneCallback callback) = 10;
}
