/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.google.android.leanbackjank;

public final class IntentKeys {

    public static final String CATEGORY_COUNT = "CATEGORY_COUNT";
    public static final String ENTRIES_PER_CATEGORY = "ENTRIES_PER_CATEGORY";
    public static final String CARD_WIDTH = "CARD_WIDTH";
    public static final String CARD_HEIGHT = "CARD_HEIGHT";
    public static final String DISABLE_SHADOWS = "ENABLE_SHADOWS";
    public static final String WHICH_VIDEO = "WHICH_VIDEO";
    public static final String USE_SINGLE_BITMAP = "USE_SINGLE_BITMAP";

    // Define values for WHICH_VIDEO.
    public static final int NO_VIDEO = 0;
    public static final int VIDEO_360P_60FPS = 1;
    public static final int VIDEO_480P_60FPS = 2;
    public static final int VIDEO_1080P_60FPS = 3;
    public static final int VIDEO_2160P_60FPS = 4;

    private IntentKeys() {
    }
}
