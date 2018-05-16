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

import android.content.ComponentName;
import android.support.mediacompat.testlib.util.IntentUtil;

public class CommonConstants {

    // Constants for using TestHelper/TestHelperService
    public static final ComponentName SERVICE_APP_TEST_HELPER_SERVICE_COMPONENT_NAME =
            new ComponentName(IntentUtil.SERVICE_PACKAGE_NAME,
                    "androidx.media.test.service.TestHelperService");
    public static final ComponentName CLIENT_APP_TEST_HELPER_SERVICE_COMPONENT_NAME =
            new ComponentName(IntentUtil.CLIENT_PACKAGE_NAME,
                    "androidx.media.test.client.TestHelperService");
    public static final String ACTION_TEST_HELPER = "androidx.media.action.test.TEST_HELPER";

    // Keys for arguments.
    public static final String KEY_STREAM = "stream";
    public static final String KEY_AUDIO_ATTRIBUTES = "audioAttributes";
    public static final String KEY_PLAYER_STATE = "playerState";
    public static final String KEY_PLAYLIST = "playlist";
    public static final String KEY_PLAYLIST_METADATA = "playlistMetadata";
    public static final String KEY_SHUFFLE_MODE = "shuffleMode";
    public static final String KEY_REPEAT_MODE = "repeatMode";
    public static final String KEY_CURRENT_POSITION = "currentPosition";
    public static final String KEY_SEEK_POSITION = "seekPosition";
    public static final String KEY_CUSTOM_COMMAND = "customCommand";
    public static final String KEY_BUFFERED_POSITION = "bufferedPosition";
    public static final String KEY_BUFFERING_STATE = "bufferingState";
    public static final String KEY_SPEED = "speed";
    public static final String KEY_MEDIA_ITEM = "mediaItem";
    public static final String KEY_ITEM_INDEX = "itemIndex";
    public static final String KEY_VOLUME_VALUE = "volumeValue";
    public static final String KEY_VOLUME_DIRECTION = "volumeDirection";
    public static final String KEY_FLAGS = "flags";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_COMMAND_GROUP = "commandGroup";
    public static final String KEY_ARGUMENTS = "arguments";
    public static final String KEY_RESULT_RECEIVER = "resultReceiver";
    public static final String KEY_QUERY = "query";
    public static final String KEY_EXTRAS = "extras";
    public static final String KEY_URI = "uri";
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_RATING = "rating";
    public static final String KEY_ROUTE = "route";
    public static final String KEY_ROUTE_LIST = "routeList";
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_COMMAND_BUTTON_LIST = "commandButtonList";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_MAX_VOLUME = "maxVolume";
    public static final String KEY_CURRENT_VOLUME = "currentVolume";
    public static final String KEY_VOLUME_CONTROL_TYPE = "volumeControlType";

    public static final int INDEX_FOR_UNKONWN_DSD = -1;
    public static final int INDEX_FOR_NULL_DSD = -2;

    // Default test name
    public static final String DEFAULT_TEST_NAME = "defaultTestName";

    private CommonConstants() {
    }
}
