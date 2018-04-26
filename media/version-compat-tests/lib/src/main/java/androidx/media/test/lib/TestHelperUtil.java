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

/**
 * Methods and constants used for calling methods between client and service apps by using
 * TestHelper/TestHelperService.
 */
public class TestHelperUtil {
    public static final ComponentName SERVICE_TEST_HELPER_COMPONENT_NAME = new ComponentName(
            IntentUtil.SERVICE_PACKAGE_NAME, "androidx.media.test.service.TestHelperService");

    public static final String ACTION_TEST_HELPER = "androidx.media.action.test.TEST_HELPER";

    private TestHelperUtil() {
    }
}
