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
 * Constants for testing the media browser and service.
 */
public class MediaBrowserConstants {

    // MediaBrowserServiceCompat methods.
    public static final int NOTIFY_CHILDREN_CHANGED = 1;
    public static final int SEND_DELAYED_NOTIFY_CHILDREN_CHANGED = 2;
    public static final int SEND_DELAYED_ITEM_LOADED = 3;
    public static final int CUSTOM_ACTION_SEND_PROGRESS_UPDATE = 4;
    public static final int CUSTOM_ACTION_SEND_ERROR = 5;
    public static final int CUSTOM_ACTION_SEND_RESULT = 6;
    public static final int SET_SESSION_TOKEN = 7;

    public static final String MEDIA_ID_ROOT = "test_media_id_root";

    public static final String EXTRAS_KEY = "test_extras_key";
    public static final String EXTRAS_VALUE = "test_extras_value";

    public static final String MEDIA_ID_INVALID = "test_media_id_invalid";
    public static final String MEDIA_ID_CHILDREN_DELAYED = "test_media_id_children_delayed";
    public static final String MEDIA_ID_ON_LOAD_ITEM_NOT_IMPLEMENTED =
            "test_media_id_on_load_item_not_implemented";

    public static final String SEARCH_QUERY = "children_2";
    public static final String SEARCH_QUERY_FOR_NO_RESULT = "query no result";
    public static final String SEARCH_QUERY_FOR_ERROR = "query for error";

    public static final String CUSTOM_ACTION = "CUSTOM_ACTION";
    public static final String CUSTOM_ACTION_FOR_ERROR = "CUSTOM_ACTION_FOR_ERROR";

    public static final String TEST_KEY_1 = "key_1";
    public static final String TEST_VALUE_1 = "value_1";
    public static final String TEST_KEY_2 = "key_2";
    public static final String TEST_VALUE_2 = "value_2";
    public static final String TEST_KEY_3 = "key_3";
    public static final String TEST_VALUE_3 = "value_3";
    public static final String TEST_KEY_4 = "key_4";
    public static final String TEST_VALUE_4 = "value_4";

    public static final String[] MEDIA_ID_CHILDREN = new String[]{
            "test_media_id_children_0", "test_media_id_children_1",
            "test_media_id_children_2", "test_media_id_children_3",
            MEDIA_ID_CHILDREN_DELAYED
    };
}
