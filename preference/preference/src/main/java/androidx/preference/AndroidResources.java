/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;

/**
 * Utility class for attributes unavailable on older APIs
 */
@RestrictTo(LIBRARY)
@SuppressLint("InlinedApi")
public class AndroidResources {

    public static final int ANDROID_R_ICON_FRAME = android.R.id.icon_frame;
    static final int ANDROID_R_LIST_CONTAINER = android.R.id.list_container;
    static final int ANDROID_R_SWITCH_WIDGET = android.R.id.switch_widget;
    static final int ANDROID_R_PREFERENCE_FRAGMENT_STYLE = android.R.attr.preferenceFragmentStyle;

    private AndroidResources() {}
}
