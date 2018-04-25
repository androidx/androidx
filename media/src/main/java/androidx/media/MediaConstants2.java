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

package androidx.media;

class MediaConstants2 {

    static final int CONNECT_RESULT_CONNECTED = 0;
    static final int CONNECT_RESULT_DISCONNECTED = -1;

    // Event string used by IMediaControllerCallback.onEvent()
    static final String SESSION_EVENT_ON_PLAYER_STATE_CHANGED =
            "androidx.media.session.event.ON_PLAYER_STATE_CHANGED";
    static final String SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED =
            "androidx.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED";
    static final String SESSION_EVENT_ON_ERROR = "androidx.media.session.event.ON_ERROR";
    static final String SESSION_EVENT_ON_ROUTES_INFO_CHANGED =
            "androidx.media.session.event.ON_ROUTES_INFO_CHANGED";
    static final String SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED =
            "androidx.media.session.event.ON_PLAYBACK_INFO_CHANGED";
    static final String SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED =
            "androidx.media.session.event.ON_PLAYBACK_SPEED_CHANGED";
    static final String SESSION_EVENT_ON_BUFFERING_STATE_CHANGED =
            "androidx.media.session.event.ON_BUFFERING_STATE_CHANGED";
    static final String SESSION_EVENT_ON_SEEK_COMPLETED =
            "androidx.media.session.event.ON_SEEK_COMPLETED";
    static final String SESSION_EVENT_ON_REPEAT_MODE_CHANGED =
            "androidx.media.session.event.ON_REPEAT_MODE_CHANGED";
    static final String SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED =
            "androidx.media.session.event.ON_SHUFFLE_MODE_CHANGED";
    static final String SESSION_EVENT_ON_PLAYLIST_CHANGED =
            "androidx.media.session.event.ON_PLAYLIST_CHANGED";
    static final String SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED =
            "androidx.media.session.event.ON_PLAYLIST_METADATA_CHANGED";
    static final String SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED =
            "androidx.media.session.event.ON_ALLOWED_COMMANDS_CHANGED";
    static final String SESSION_EVENT_ON_CHILDREN_CHANGED =
            "androidx.media.session.event.ON_CHILDREN_CHANGED";
    static final String SESSION_EVENT_ON_SEARCH_RESULT_CHANGED =
            "androidx.media.session.event.ON_SEARCH_RESULT_CHANGED";
    static final String SESSION_EVENT_SEND_CUSTOM_COMMAND =
            "androidx.media.session.event.SEND_CUSTOM_COMMAND";
    static final String SESSION_EVENT_SET_CUSTOM_LAYOUT =
            "androidx.media.session.event.SET_CUSTOM_LAYOUT";

    // Command string used by MediaControllerCompat.sendCommand()
    static final String CONTROLLER_COMMAND_CONNECT = "androidx.media.controller.command.CONNECT";
    static final String CONTROLLER_COMMAND_DISCONNECT =
            "androidx.media.controller.command.DISCONNECT";
    static final String CONTROLLER_COMMAND_BY_COMMAND_CODE =
            "androidx.media.controller.command.BY_COMMAND_CODE";
    static final String CONTROLLER_COMMAND_BY_CUSTOM_COMMAND =
            "androidx.media.controller.command.BY_CUSTOM_COMMAND";


    static final String ARGUMENT_COMMAND_CODE = "androidx.media.argument.COMMAND_CODE";
    static final String ARGUMENT_CUSTOM_COMMAND = "androidx.media.argument.CUSTOM_COMMAND";
    static final String ARGUMENT_ALLOWED_COMMANDS = "androidx.media.argument.ALLOWED_COMMANDS";
    static final String ARGUMENT_SEEK_POSITION = "androidx.media.argument.SEEK_POSITION";
    static final String ARGUMENT_PLAYER_STATE = "androidx.media.argument.PLAYER_STATE";
    static final String ARGUMENT_PLAYBACK_SPEED = "androidx.media.argument.PLAYBACK_SPEED";
    static final String ARGUMENT_BUFFERING_STATE = "androidx.media.argument.BUFFERING_STATE";
    static final String ARGUMENT_ERROR_CODE = "androidx.media.argument.ERROR_CODE";
    static final String ARGUMENT_REPEAT_MODE = "androidx.media.argument.REPEAT_MODE";
    static final String ARGUMENT_SHUFFLE_MODE = "androidx.media.argument.SHUFFLE_MODE";
    static final String ARGUMENT_PLAYLIST = "androidx.media.argument.PLAYLIST";
    static final String ARGUMENT_PLAYLIST_INDEX = "androidx.media.argument.PLAYLIST_INDEX";
    static final String ARGUMENT_PLAYLIST_METADATA = "androidx.media.argument.PLAYLIST_METADATA";
    static final String ARGUMENT_RATING = "androidx.media.argument.RATING";
    static final String ARGUMENT_MEDIA_ITEM = "androidx.media.argument.MEDIA_ITEM";
    static final String ARGUMENT_MEDIA_ID = "androidx.media.argument.MEDIA_ID";
    static final String ARGUMENT_QUERY = "androidx.media.argument.QUERY";
    static final String ARGUMENT_URI = "androidx.media.argument.URI";
    static final String ARGUMENT_PLAYBACK_STATE_COMPAT =
            "androidx.media.argument.PLAYBACK_STATE_COMPAT";
    static final String ARGUMENT_VOLUME = "androidx.media.argument.VOLUME";
    static final String ARGUMENT_VOLUME_DIRECTION = "androidx.media.argument.VOLUME_DIRECTION";
    static final String ARGUMENT_VOLUME_FLAGS = "androidx.media.argument.VOLUME_FLAGS";
    static final String ARGUMENT_EXTRAS = "androidx.media.argument.EXTRAS";
    static final String ARGUMENT_ARGUMENTS = "androidx.media.argument.ARGUMENTS";
    static final String ARGUMENT_RESULT_RECEIVER = "androidx.media.argument.RESULT_RECEIVER";
    static final String ARGUMENT_COMMAND_BUTTONS = "androidx.media.argument.COMMAND_BUTTONS";
    static final String ARGUMENT_ROUTE_BUNDLE = "androidx.media.argument.ROUTE_BUNDLE";
    static final String ARGUMENT_PLAYBACK_INFO = "androidx.media.argument.PLAYBACK_INFO";
    static final String ARGUMENT_ITEM_COUNT = "androidx.media.argument.ITEM_COUNT";
    static final String ARGUMENT_PAGE = "androidx.media.argument.PAGE";
    static final String ARGUMENT_PAGE_SIZE = "androidx.media.argument.PAGE_SIZE";

    static final String ARGUMENT_ICONTROLLER_CALLBACK =
            "androidx.media.argument.ICONTROLLER_CALLBACK";
    static final String ARGUMENT_UID = "androidx.media.argument.UID";
    static final String ARGUMENT_PID = "androidx.media.argument.PID";
    static final String ARGUMENT_PACKAGE_NAME = "androidx.media.argument.PACKAGE_NAME";

    static final String ROOT_EXTRA_DEFAULT = "androidx.media.root_default_root";

    private MediaConstants2() {
    }
}
