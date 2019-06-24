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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.media2.widget.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaControlView} without any {@link androidx.media2.common.SessionPlayer} or
 * {@link androidx.media2.session.MediaController}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControlView_WithoutPlayerTest extends MediaWidgetTestBase {
    private MediaControlViewTestActivity mActivity;
    private MediaControlView mMediaControlView;

    @Rule
    public ActivityTestRule<MediaControlViewTestActivity> mActivityRule =
            new ActivityTestRule<>(MediaControlViewTestActivity.class);

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mMediaControlView = mActivity.findViewById(R.id.mediacontrolview);

        setKeepScreenOn(mActivityRule);
        checkAttachedToWindow(mMediaControlView);
    }

    @UiThreadTest
    @Test
    public void testConstructor() {
        new MediaControlView(mActivity);
        new MediaControlView(mActivity, null);
        new MediaControlView(mActivity, null, 0);
    }

    @Test
    public void testFullScreenListener() throws Throwable {
        onView(withId(R.id.fullscreen)).check(matches(not(isDisplayed())));

        final CountDownLatch latchOn = new CountDownLatch(1);
        final CountDownLatch latchOff = new CountDownLatch(1);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaControlView.setOnFullScreenListener(
                        new MediaControlView.OnFullScreenListener() {
                            @Override
                            public void onFullScreen(@NonNull View view, boolean fullScreen) {
                                if (fullScreen) {
                                    latchOn.countDown();
                                } else {
                                    latchOff.countDown();
                                }
                            }
                        });
            }
        });
        onView(withId(R.id.fullscreen)).check(matches(isCompletelyDisplayed()));

        onView(withId(R.id.fullscreen)).perform(click());
        assertTrue(latchOn.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.fullscreen)).perform(click());
        assertTrue(latchOff.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaControlView.setOnFullScreenListener(null);
            }
        });
        onView(withId(R.id.fullscreen)).check(matches(not(isDisplayed())));
    }
}
