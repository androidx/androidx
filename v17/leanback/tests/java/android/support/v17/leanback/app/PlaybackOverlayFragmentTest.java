/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v17.leanback.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PlaybackOverlayFragmentTest {

    @Rule
    public ActivityTestRule<PlaybackOverlayTestActivity> activityTestRule =
            new ActivityTestRule<>(PlaybackOverlayTestActivity.class, false, false);
    private PlaybackOverlayTestActivity mActivity;

    @Test
    public void workaroundVideoViewStealFocus() {
        Intent intent = new Intent();
        mActivity = activityTestRule.launchActivity(intent);

        assertFalse(mActivity.findViewById(R.id.videoView).hasFocus());
        assertTrue(mActivity.getPlaybackFragment().getView().hasFocus());
    }

}
