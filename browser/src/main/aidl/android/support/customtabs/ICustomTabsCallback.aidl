/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.customtabs;

import android.os.Bundle;

/**
 * Interface to a CustomTabsCallback.
 * @hide
 */
interface ICustomTabsCallback {
    void onNavigationEvent(int navigationEvent, in Bundle extras) = 1;
    void extraCallback(String callbackName, in Bundle args) = 2;
    void onMessageChannelReady(in Bundle extras) = 3;
    void onPostMessage(String message, in Bundle extras) = 4;
    void onRelationshipValidationResult(int relation, in Uri origin, boolean result, in Bundle extras) = 5;
}
