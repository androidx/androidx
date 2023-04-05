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

import android.content.ComponentName;
import android.content.Intent;

import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.serialization.Bundleable;

/**
 * An interface to be used for communicating with the renderer.
 *
 * @hide
 */
interface IRendererService {

  /**
   * Initializes rendering.
   *
   * @param carActivity the binder for performing two-way communication
   * @param serviceName the service that is associated with the activity
   * @param displayId the display ID on which the content should be rendered
   *
   * @return true if successful
   */
  boolean initialize(ICarAppActivity carActivity, in ComponentName serviceName, int
  displayId) = 1;

  /**
   * Notifies of a new intent that requires processing.
   *
   * @param intent The intent that needs to be processed
   * @param serviceName the service that is associated with the activity
   * @param displayId the display ID on which the content should be rendererd
   *
   * @return true if successful
   */
  boolean onNewIntent(in Intent intent, in ComponentName serviceName, int displayId) = 2;

  /**
   * Notifies that {@link CarAppActivity} no longer needs the renderer.
   *
   * @param serviceName the service that is associated with the activity
   */
  void terminate(in ComponentName serviceName) = 3;

  /**
   * Performs a handshake, negotiating the api level for communication between app and host.
   *
   * @param serviceName       the component for the car app service that the handshake is for
   * @param appLatestApiLevel the latest api level for the app side
   *
   * @return a {@link HandshakeInfo} including the negotiated api level
   */
   Bundleable performHandshake(in ComponentName serviceName, int appLatestApiLevel) = 4;
}
