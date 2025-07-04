/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core

/**
 * Used to notify the SDK about changes in the client's
 * [android.app.ActivityManager.RunningAppProcessInfo.importance].
 *
 * When an SDK wants to get notified about changes in client's importance, it should register an
 * implementation of this interface by calling
 * [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat.registerSdkSandboxClientImportanceListener].
 */
public interface SdkSandboxClientImportanceListenerCompat {
    /**
     * Invoked every time the client transitions from a value <=
     * [android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND] to a higher value
     * or vice versa.
     *
     * @param isForeground true when the client transitions to
     *   [android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND] or lower and
     *   false when it is the other way round.
     */
    public fun onForegroundImportanceChanged(isForeground: Boolean)
}
