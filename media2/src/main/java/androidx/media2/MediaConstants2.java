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

package androidx.media2;

class MediaConstants2 {

    static final int CONNECT_RESULT_CONNECTED = 0;
    static final int CONNECT_RESULT_DISCONNECTED = -1;

    // Event string used by IMediaControllerCallback.onEvent()
    static final String SESSION_EVENT_ON_PLAYER_STATE_CHANGED =
            "androidx.media2.session.event.ON_PLAYER_STATE_CHANGED";
    static final String SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED =
            "androidx.media2.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED";
    static final String SESSION_EVENT_ON_ERROR = "androidx.media2.session.event.ON_ERROR";
    static final String SESSION_EVENT_ON_ROUTES_INFO_CHANGED =
            "androidx.media2.session.event.ON_ROUTES_INFO_CHANGED";
    static final String SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED =
            "androidx.media2.session.event.ON_PLAYBACK_INFO_CHANGED";
    static final String SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED =
            "androidx.media2.session.event.ON_PLAYBACK_SPEED_CHANGED";
    static final String SESSION_EVENT_ON_BUFFERING_STATE_CHANGED =
            "androidx.media2.session.event.ON_BUFFERING_STATE_CHANGED";
    static final String SESSION_EVENT_ON_SEEK_COMPLETED =
            "androidx.media2.session.event.ON_SEEK_COMPLETED";
    static final String SESSION_EVENT_ON_REPEAT_MODE_CHANGED =
            "androidx.media2.session.event.ON_REPEAT_MODE_CHANGED";
    static final String SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED =
            "androidx.media2.session.event.ON_SHUFFLE_MODE_CHANGED";
    static final String SESSION_EVENT_ON_PLAYLIST_CHANGED =
            "androidx.media2.session.event.ON_PLAYLIST_CHANGED";
    static final String SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED =
            "androidx.media2.session.event.ON_PLAYLIST_METADATA_CHANGED";
    static final String SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED =
            "androidx.media2.session.event.ON_ALLOWED_COMMANDS_CHANGED";
    static final String SESSION_EVENT_ON_CHILDREN_CHANGED =
            "androidx.media2.session.event.ON_CHILDREN_CHANGED";
    static final String SESSION_EVENT_ON_SEARCH_RESULT_CHANGED =
            "androidx.media2.session.event.ON_SEARCH_RESULT_CHANGED";
    static final String SESSION_EVENT_SEND_CUSTOM_COMMAND =
            "androidx.media2.session.event.SEND_CUSTOM_COMMAND";
    static final String SESSION_EVENT_SET_CUSTOM_LAYOUT =
            "androidx.media2.session.event.SET_CUSTOM_LAYOUT";

    // Command string used by MediaControllerCompat.sendCommand()
    static final String CONTROLLER_COMMAND_CONNECT = "androidx.media2.controller.command.CONNECT";
    static final String CONTROLLER_COMMAND_DISCONNECT =
            "androidx.media2.controller.command.DISCONNECT";
    static final String CONTROLLER_COMMAND_BY_COMMAND_CODE =
            "androidx.media2.controller.command.BY_COMMAND_CODE";
    static final String CONTROLLER_COMMAND_BY_CUSTOM_COMMAND =
            "androidx.media2.controller.command.BY_CUSTOM_COMMAND";


    static final String ARGUMENT_COMMAND_CODE = "androidx.media2.argument.COMMAND_CODE";
    static final String ARGUMENT_CUSTOM_COMMAND = "androidx.media2.argument.CUSTOM_COMMAND";
    static final String ARGUMENT_ALLOWED_COMMANDS = "androidx.media2.argument.ALLOWED_COMMANDS";
    static final String ARGUMENT_SEEK_POSITION = "androidx.media2.argument.SEEK_POSITION";
    static final String ARGUMENT_PLAYER_STATE = "androidx.media2.argument.PLAYER_STATE";
    static final String ARGUMENT_PLAYBACK_SPEED = "androidx.media2.argument.PLAYBACK_SPEED";
    static final String ARGUMENT_BUFFERING_STATE = "androidx.media2.argument.BUFFERING_STATE";
    static final String ARGUMENT_ERROR_CODE = "androidx.media2.argument.ERROR_CODE";
    static final String ARGUMENT_REPEAT_MODE = "androidx.media2.argument.REPEAT_MODE";
    static final String ARGUMENT_SHUFFLE_MODE = "androidx.media2.argument.SHUFFLE_MODE";
    static final String ARGUMENT_PLAYLIST = "androidx.media2.argument.PLAYLIST";
    static final String ARGUMENT_PLAYLIST_INDEX = "androidx.media2.argument.PLAYLIST_INDEX";
    static final String ARGUMENT_PLAYLIST_METADATA = "androidx.media2.argument.PLAYLIST_METADATA";
    static final String ARGUMENT_RATING = "androidx.media2.argument.RATING";
    static final String ARGUMENT_MEDIA_ITEM = "androidx.media2.argument.MEDIA_ITEM";
    static final String ARGUMENT_MEDIA_ID = "androidx.media2.argument.MEDIA_ID";
    static final String ARGUMENT_QUERY = "androidx.media2.argument.QUERY";
    static final String ARGUMENT_URI = "androidx.media2.argument.URI";
    static final String ARGUMENT_PLAYBACK_STATE_COMPAT =
            "androidx.media2.argument.PLAYBACK_STATE_COMPAT";
    static final String ARGUMENT_VOLUME = "androidx.media2.argument.VOLUME";
    static final String ARGUMENT_VOLUME_DIRECTION = "androidx.media2.argument.VOLUME_DIRECTION";
    static final String ARGUMENT_VOLUME_FLAGS = "androidx.media2.argument.VOLUME_FLAGS";
    static final String ARGUMENT_EXTRAS = "androidx.media2.argument.EXTRAS";
    static final String ARGUMENT_ARGUMENTS = "androidx.media2.argument.ARGUMENTS";
    static final String ARGUMENT_RESULT_RECEIVER = "androidx.media2.argument.RESULT_RECEIVER";
    static final String ARGUMENT_COMMAND_BUTTONS = "androidx.media2.argument.COMMAND_BUTTONS";
    static final String ARGUMENT_ROUTE_BUNDLE = "androidx.media2.argument.ROUTE_BUNDLE";
    static final String ARGUMENT_PLAYBACK_INFO = "androidx.media2.argument.PLAYBACK_INFO";
    static final String ARGUMENT_ITEM_COUNT = "androidx.media2.argument.ITEM_COUNT";

    static final String ARGUMENT_ICONTROLLER_CALLBACK =
            "androidx.media2.argument.ICONTROLLER_CALLBACK";
    static final String ARGUMENT_UID = "androidx.media2.argument.UID";
    static final String ARGUMENT_PID = "androidx.media2.argument.PID";
    static final String ARGUMENT_PACKAGE_NAME = "androidx.media2.argument.PACKAGE_NAME";

    static final String ROOT_EXTRA_DEFAULT = "androidx.media2.root_default_root";

    private MediaConstants2() {
    }
}
