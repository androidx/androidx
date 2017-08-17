/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.mediacompat.testlib;

/**
 * Constants used for sending intent between client and service apks.
 */
public class IntentConstants {
    public static final String ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD =
            "android.support.mediacompat.service.action.CALL_MEDIA_BROWSER_SERVICE_METHOD";
    public static final String KEY_METHOD_ID = "method_id";
    public static final String KEY_ARGUMENT = "argument";
}
