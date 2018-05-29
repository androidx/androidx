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

package androidx.media.test.lib;

/**
 * Constants for calling MediaSession2 methods.
 */
public class MediaSession2Constants {

    // Test method names
    public static final String TEST_GET_SESSION_ACTIVITY = "testGetSessionActivity";
    public static final String TEST_CONTROLLER_CALLBACK_SESSION_REJECTS =
            "testControllerCallback_sessionRejects";
    public static final String TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST =
            "testOnPlaylistMetadataChanged_sessionSetPlaylist";

    /**
     * Constants for test-specific custom command which will be run in the service app.
     */
    public static class CustomCommands {
        public static final int UPDATE_PLAYER = 1000;
        public static final int UPDATE_PLAYER_FOR_SETTING_STREAM_TYPE = 1001;
        public static final int UPDATE_PLAYER_WITH_VOLUME_PROVIDER = 1002;
        public static final int CUSTOM_METHOD_SET_MULTIPLE_VALUES = 1900;

        private CustomCommands() {
        }
    }

    private MediaSession2Constants() {
    }
}
