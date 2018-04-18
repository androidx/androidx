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
 * Constants for testing the media controller.
 */
public class MediaControllerConstants {

    // MediaControllerCompat methods.
    public static final int SEND_COMMAND = 201;
    public static final int ADD_QUEUE_ITEM = 202;
    public static final int ADD_QUEUE_ITEM_WITH_INDEX = 203;
    public static final int REMOVE_QUEUE_ITEM = 204;
    public static final int SET_VOLUME_TO = 205;
    public static final int ADJUST_VOLUME = 206;

    // TransportControls methods.
    public static final int PLAY = 301;
    public static final int PAUSE = 302;
    public static final int STOP = 303;
    public static final int FAST_FORWARD = 304;
    public static final int REWIND = 305;
    public static final int SKIP_TO_PREVIOUS = 306;
    public static final int SKIP_TO_NEXT = 307;
    public static final int SEEK_TO = 308;
    public static final int SET_RATING = 309;
    public static final int PLAY_FROM_MEDIA_ID = 310;
    public static final int PLAY_FROM_SEARCH = 311;
    public static final int PLAY_FROM_URI = 312;
    public static final int SEND_CUSTOM_ACTION = 313;
    public static final int SEND_CUSTOM_ACTION_PARCELABLE = 314;
    public static final int SKIP_TO_QUEUE_ITEM = 315;
    public static final int PREPARE = 316;
    public static final int PREPARE_FROM_MEDIA_ID = 317;
    public static final int PREPARE_FROM_SEARCH = 318;
    public static final int PREPARE_FROM_URI = 319;
    public static final int SET_CAPTIONING_ENABLED = 320;
    public static final int SET_REPEAT_MODE = 321;
    public static final int SET_SHUFFLE_MODE = 322;

    private MediaControllerConstants() {
    }
}
