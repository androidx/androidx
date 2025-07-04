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

package androidx.privacysandbox.databridge.core

/**
 * Callback for getting updates whenever there is change in value of the registered keys from the
 * app or the SDKs that the app uses.
 *
 * Used as a parameter for
 * [androidx.privacysandbox.databridge.client.DataBridgeClient.registerKeyUpdateCallback] Whenever
 * there is an update in the value for any of the keys in the key list, the
 * [KeyUpdateCallback.onKeyUpdated] method is called.
 */
public interface KeyUpdateCallback {
    /**
     * Triggered when there is an update in the keys that are registered by the caller to receive
     * updates
     *
     * @param key: The key which is updated
     * @param value: Value corresponding to the key which is updated
     */
    public fun onKeyUpdated(key: Key, value: Any?)
}
