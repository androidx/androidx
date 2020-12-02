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

package androidx.mediarouter.app;

/**
 * Constants for opening Output Switcher activity.
 */
class OutputSwitcherConstants {
    /**
     * Copied from MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT.
     */
    static final String ACTION_MEDIA_OUTPUT = "com.android.settings.panel.action.MEDIA_OUTPUT";

    /**
     * Copied from MediaOutputSliceConstants.EXTRA_PACKAGE_NAME.
     */
    static final String EXTRA_PACKAGE_NAME = "com.android.settings.panel.extra.PACKAGE_NAME";

    /**
     * Copied from MediaOutputSliceConstants.KEY_MEDIA_SESSION_TOKEN.
     */
    static final String KEY_MEDIA_SESSION_TOKEN = "key_media_session_token";

    private OutputSwitcherConstants() {}
}
