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

import android.net.Uri;
import android.os.Bundle;

/**
 * Interface to a CustomTabsCallback.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface ICustomTabsCallback {
    oneway void onNavigationEvent(int navigationEvent, in Bundle extras) = 1;
    oneway void extraCallback(String callbackName, in Bundle args) = 2;

    // Not defined with 'oneway' to preserve the calling order among |onPostMessage()| and related calls.
    void onMessageChannelReady(in Bundle extras) = 3;
    void onPostMessage(String message, in Bundle extras) = 4;
    oneway void onRelationshipValidationResult(int relation, in Uri origin, boolean result, in Bundle extras) = 5;

    // API with return value cannot be 'oneway'.
    Bundle extraCallbackWithResult(String callbackName, in Bundle args) = 6;
    oneway void onActivityResized(int height, int width, in Bundle extras) = 7;
    oneway void onWarmupCompleted(in Bundle extras) = 8;
    oneway void onActivityLayout(int left, int top, int right, int bottom, int state, in android.os.Bundle extras) = 9;
    oneway void onMinimized(in Bundle extras) = 10;
    oneway void onUnminimized(in Bundle extras) = 11;
}
