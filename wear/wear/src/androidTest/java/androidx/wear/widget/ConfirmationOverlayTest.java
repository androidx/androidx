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

package androidx.wear.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.wear.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ConfirmationOverlayTest {

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<ConfirmationOverlayTestActivity> mActivityRule =
            new ActivityTestRule<>(ConfirmationOverlayTestActivity.class, true, true);

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private ConfirmationOverlay.OnAnimationFinishedListener mOnAnimationFinishedListener;

    private LinearLayout mLinearLayout;
    private TextView mActualTextView;

    private Activity setupActivity() {
        Activity activity = mActivityRule.getActivity();
        mLinearLayout = new LinearLayout(activity);
        activity.setContentView(mLinearLayout);
        return activity;
    }

    @Test
    public void testDefaults_onActivity() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConfirmationOverlay overlay = new ConfirmationOverlay();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                getOverlayViews(getContentView(activity));
                assertEquals(View.GONE, mActualTextView.getVisibility());
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
                overlay.hide();
            }
        });
        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertNull(getContentView(mActivityRule.getActivity()));
    }

    @Test
    @UiThreadTest
    public void testDefaults_onView() {
        setupActivity();
        ConfirmationOverlay overlay = new ConfirmationOverlay();

        overlay.showAbove(mLinearLayout);
        getOverlayViews(mLinearLayout.getRootView());
        assertEquals(View.GONE, mActualTextView.getVisibility());

        overlay.hide();
        assertEquals(0, mLinearLayout.getChildCount());
    }

    @Test
    @UiThreadTest
    public void testWithMessage_onView() {
        setupActivity();
        ConfirmationOverlay overlay = new ConfirmationOverlay();
        overlay.setMessage((CharSequence) "Confirmation");

        overlay.showAbove(mLinearLayout);
        getOverlayViews(mLinearLayout.getRootView());
        assertEquals(View.VISIBLE, mActualTextView.getVisibility());

        overlay.hide();
        assertEquals(0, mLinearLayout.getChildCount());
    }

    @Test
    public void testSuccess_onActivity() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConfirmationOverlay overlay = new ConfirmationOverlay()
                .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                .setOnAnimationFinishedListener(mOnAnimationFinishedListener)
                .setMessage((CharSequence) "Sent");

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                getOverlayViews(getContentView(activity));
                assertEquals("Sent", mActualTextView.getText());
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
                overlay.hide();
            }
        });
        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertNull(getContentView(mActivityRule.getActivity()));
    }

    @Test
    public void testFailure_onActivity() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConfirmationOverlay overlay = new ConfirmationOverlay()
                .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                .setOnAnimationFinishedListener(mOnAnimationFinishedListener)
                .setMessage((CharSequence) "Failed");

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                getOverlayViews(getContentView(activity));
                assertEquals("Failed", mActualTextView.getText());
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
                overlay.hide();
            }
        });
        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertNull(getContentView(mActivityRule.getActivity()));
    }

    @Test
    public void testOpenOnPhone_onActivity() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConfirmationOverlay overlay = new ConfirmationOverlay()
                .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                .setOnAnimationFinishedListener(mOnAnimationFinishedListener)
                .setMessage((CharSequence) "Opening...");

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                getOverlayViews(getContentView(activity));
                assertEquals("Opening...", mActualTextView.getText());
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
                overlay.hide();
            }
        });
        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertNull(getContentView(mActivityRule.getActivity()));
    }

    @Test
    @UiThreadTest
    public void testThrowsExceptionOnInvalidType() {
        Activity activity = setupActivity();
        try {
            new ConfirmationOverlay().setType(-1).showOn(activity);
            fail("Expected ConfirmationOverlay to throw an exception when given an invalid type.");
        } catch (IllegalStateException e) {
            // Success.
        }
    }

    @Test
    public void testOverlayVisibleDuringSpecifiedDuration() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final int overlayDurationMillis = 2000;
        final ConfirmationOverlay overlay =
                new ConfirmationOverlay()
                        .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                        .setDuration(overlayDurationMillis);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
            }
        });
        latch.await(overlayDurationMillis - 1000, TimeUnit.MILLISECONDS);
        assertEquals(1, latch.getCount());
        assertNotNull(getContentView(mActivityRule.getActivity()));
    }

    @Test
    public void testOverlayHiddenAfterSpecifiedDuration() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final int overlayDurationMillis = 2000;
        final ConfirmationOverlay overlay =
                new ConfirmationOverlay()
                        .setType(ConfirmationOverlay.SUCCESS_ANIMATION)
                        .setDuration(overlayDurationMillis);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = setupActivity();
                overlay.showOn(activity);
                ConfirmationOverlay.OnAnimationFinishedListener onAnimationFinishedListener =
                        new ConfirmationOverlay.OnAnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                latch.countDown();
                            }
                        };
                overlay.setOnAnimationFinishedListener(onAnimationFinishedListener);
            }
        });
        latch.await(overlayDurationMillis + 100, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
        assertNull(getContentView(mActivityRule.getActivity()));
    }

    private static View getContentView(Activity activity) {
        ViewGroup viewGroup = activity.getWindow().findViewById(android.R.id.content);

        // Return the second child, since the first is the LinearLayout added in setup().
        return viewGroup.getChildAt(1);
    }

    private void getOverlayViews(View parent) {
        mActualTextView = parent.findViewById(R.id.wearable_support_confirmation_overlay_message);
    }
}
