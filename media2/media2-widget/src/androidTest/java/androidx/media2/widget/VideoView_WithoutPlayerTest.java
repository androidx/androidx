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

import static org.junit.Assume.assumeTrue;

import android.app.Activity;

import androidx.media2.widget.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link VideoView} without any {@link androidx.media2.common.SessionPlayer} or {@link
 * androidx.media2.session.MediaController}.
 */
@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoView_WithoutPlayerTest extends MediaWidgetTestBase {
    private Activity mActivity;
    private VideoView mVideoView;

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<VideoViewTestActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(VideoViewTestActivity.class);

    @Before
    public void setup() throws Throwable {
        // Ignore all tests, b/202710013
        assumeTrue(false);

        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        checkAttachedToWindow(mVideoView);
    }

    @UiThreadTest
    @Test
    public void constructor() {
        new VideoView(mActivity);
        new VideoView(mActivity, null);
        new VideoView(mActivity, null, 0);
    }
}
