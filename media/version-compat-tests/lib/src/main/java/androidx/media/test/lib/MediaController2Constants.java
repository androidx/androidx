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
public class MediaController2Constants {

    // MediaController2 methods
    public static final int PLAY = 1000;
    public static final int PAUSE = 1001;
    public static final int RESET = 1002;
    public static final int PREPARE = 1003;
    public static final int SEEK_TO = 1005;
    public static final int SET_PLAYBACK_SPEED = 1006;
    public static final int SET_PLAYLIST = 1007;
    public static final int UPDATE_PLAYLIST_METADATA = 1008;
    public static final int ADD_PLAYLIST_ITEM = 1009;
    public static final int REMOVE_PLAYLIST_ITEM = 1010;
    public static final int REPLACE_PLAYLIST_ITEM = 1011;
    public static final int SKIP_TO_PREVIOUS_ITEM = 1012;
    public static final int SKIP_TO_NEXT_ITEM = 1013;
    public static final int SKIP_TO_PLAYLIST_ITEM = 1014;
    public static final int SET_SHUFFLE_MODE = 1015;
    public static final int SET_REPEAT_MODE = 1016;
    public static final int SET_VOLUME_TO = 1017;
    public static final int ADJUST_VOLUME = 1018;
    public static final int SEND_CUSTOM_COMMAND = 1019;
    public static final int FAST_FORWARD = 1020;
    public static final int REWIND = 1021;
    public static final int PLAY_FROM_SEARCH = 1022;
    public static final int PLAY_FROM_URI = 1023;
    public static final int PLAY_FROM_MEDIA_ID = 1024;
    public static final int PREPARE_FROM_SEARCH = 1025;
    public static final int PREPARE_FROM_URI = 1026;
    public static final int PREPARE_FROM_MEDIA_ID = 1027;
    public static final int SET_RATING = 1028;
    public static final int SUBSCRIBE_ROUTES_INFO = 1029;
    public static final int UNSUBSCRIBE_ROUTES_INFO = 1030;
    public static final int SELECT_ROUTE = 1031;

    private MediaController2Constants() {
    }
}
