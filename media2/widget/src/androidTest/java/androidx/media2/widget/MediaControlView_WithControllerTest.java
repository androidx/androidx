/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.session.MediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;

/**
 * Test {@link MediaControlView} with a {@link MediaController}.
 * Please place actual test cases in {@link MediaControlView_WithSthTestBase}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControlView_WithControllerTest extends MediaControlView_WithSthTestBase {
    @Override
    PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item) {
        return createPlayerWrapperOfController(callback, item);
    }
}
