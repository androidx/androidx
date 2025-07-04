/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.sdkproviderutils;

/*
 * This is registered onto the SDK to receive callbacks for automated testing.
 */
interface IAutomatedTestCallback {
    void onResizeOccurred(int width, int height);
    void onConfigurationChanged(in Configuration configuration);
    /**
    * This callback reports a touch gesture applied on the remote content.
    * It returns the deltas in X and Y between Action DOWN and Action UP.
    */
    void onGestureFinished(in float totalChangeInX, in float totalChangeInY);
    /**
    * This callback notifies the client that the session is built remotely in SDK Sandbox Process.
    * This is needed as some tests have different expectations in case that SDK is loadded locally.
    */
    void onRemoteSession();
}
