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

    // TODO: Should the Test method names belong to another file?
    // Test method names
    public static final String TEST_GET_SESSION_ACTIVITY = "testGetSessionActivity";
    public static final String TEST_CONTROLLER_CALLBACK_SESSION_REJECTS =
            "testControllerCallback_sessionRejects";
    public static final String TEST_ON_PLAYLIST_METADATA_CHANGED_SESSION_SET_PLAYLIST =
            "testOnPlaylistMetadataChanged_sessionSetPlaylist";

    // MediaSession2 methods
    public static class Session2Methods {
        public static final int UPDATE_PLAYER = 1000;
        public static final int UPDATE_PLAYER_FOR_SETTING_STREAM_TYPE = 1001;
        public static final int SET_PLAYLIST = 1002;
        public static final int SEND_CUSTOM_COMMAND = 1500;
        public static final int CLOSE = 1501;
        public static final int CUSTOM_SET_MULTIPLE_VALUES = 1900;

        private Session2Methods() {
        }
    }

    // MediaPlaylistAgent and MockPlaylistAgent methods
    public static class PlaylistAgentMethods {
        public static final int SET_PLAYLIST_MANUALLY = 3000;
        public static final int NOTIFY_PLAYLIST_CHANGED = 3001;
        public static final int SET_PLAYLIST_METADATA_MANUALLY = 3002;
        public static final int NOTIFY_PLAYLIST_METADATA_CHANGED = 3003;
        public static final int SET_SHUFFLE_MODE_MANUALLY = 3004;
        public static final int NOTIFY_SHUFFLE_MODE_CHANGED = 3005;
        public static final int SET_REPEAT_MODE_MANUALLY = 3006;
        public static final int NOTIFY_REPEAT_MODE_CHANGED = 3007;

        private PlaylistAgentMethods() {
        }
    }

    // BaseMediaPlayer and MockPlayer methods
    public static class BaseMediaPlayerMethods {
        public static final int SET_CURRENT_POSITION_MANUALLY = 4000;
        public static final int NOTIFY_SEEK_COMPLETED = 4001;
        public static final int SET_BUFFERED_POSITION_MANUALLY = 4002;
        public static final int NOTIFY_BUFFERED_STATE_CHANGED = 4003;
        public static final int SET_PLAYER_STATE_MANUALLY = 4004;
        public static final int NOTIFY_PLAYER_STATE_CHANGED = 4005;

        private BaseMediaPlayerMethods() {
        }
    }

    private MediaSession2Constants() {
    }
}
