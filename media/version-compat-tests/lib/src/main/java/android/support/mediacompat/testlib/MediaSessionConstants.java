/*
 * Copyright 2017 The Android Open Source Project
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
 * Constants for testing the media session.
 */
public class MediaSessionConstants {

    // MediaSessionCompat methods.
    public static final int SET_EXTRAS = 101;
    public static final int SET_FLAGS = 102;
    public static final int SET_METADATA = 103;
    public static final int SET_PLAYBACK_STATE = 104;
    public static final int SET_QUEUE = 105;
    public static final int SET_QUEUE_TITLE = 106;
    public static final int SET_SESSION_ACTIVITY = 107;
    public static final int SET_CAPTIONING_ENABLED = 108;
    public static final int SET_REPEAT_MODE = 109;
    public static final int SET_SHUFFLE_MODE = 110;
    public static final int SEND_SESSION_EVENT = 112;
    public static final int SET_ACTIVE = 113;
    public static final int RELEASE = 114;
    public static final int SET_PLAYBACK_TO_LOCAL = 115;
    public static final int SET_PLAYBACK_TO_REMOTE = 116;
    public static final int SET_RATING_TYPE = 117;

    public static final String TEST_SESSION_TAG = "test-session-tag";
    public static final String TEST_KEY = "test-key";
    public static final String TEST_VALUE = "test-val";
    public static final String TEST_SESSION_EVENT = "test-session-event";
    public static final String TEST_COMMAND = "test-command";
    public static final int TEST_FLAGS = 5;
    public static final int TEST_CURRENT_VOLUME = 10;
    public static final int TEST_MAX_VOLUME = 11;
    public static final long TEST_QUEUE_ID_1 = 10L;
    public static final long TEST_QUEUE_ID_2 = 20L;
    public static final String TEST_MEDIA_ID_1 = "media_id_1";
    public static final String TEST_MEDIA_ID_2 = "media_id_2";
    public static final String TEST_MEDIA_TITLE_1 = "media_title_1";
    public static final String TEST_MEDIA_TITLE_2 = "media_title_2";
    public static final long TEST_ACTION = 55L;

    public static final int TEST_ERROR_CODE = 0x3;
    public static final String TEST_ERROR_MSG = "test-error-msg";

    private MediaSessionConstants() {
    }
}
