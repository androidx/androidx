// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from DetailsSupportFragmentTest.java.  DO NOT MODIFY. */

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
package androidx.leanback.app;

import static android.os.Build.VERSION.SDK_INT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.animation.PropertyValuesHolder;
import android.app.Fragment;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.leanback.graphics.FitWidthBitmapDrawable;
import androidx.leanback.media.MediaPlayerGlue;
import androidx.leanback.media.PlaybackGlueHost;
import androidx.leanback.test.R;
import androidx.leanback.testutils.LeakDetector;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.transition.TransitionHelper;
import androidx.leanback.util.StateMachine;
import androidx.leanback.widget.DetailsParallax;
import androidx.leanback.widget.DetailsParallaxDrawable;
import androidx.leanback.widget.ParallaxTarget;
import androidx.leanback.widget.RecyclerViewParallax;
import androidx.leanback.widget.VerticalGridView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link DetailsFragment}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@FlakyTest(bugId = 207674174)
public class DetailsFragmentTest extends SingleFragmentTestBase {

    static final int PARALLAX_VERTICAL_OFFSET = -300;

    static int getCoverDrawableAlpha(DetailsFragmentBackgroundController controller) {
        return ((FitWidthBitmapDrawable) controller.mParallaxDrawable.getCoverDrawable())
                .getAlpha();
    }

    public static class DetailsFragmentParallax extends DetailsTestFragment {

        private DetailsParallaxDrawable mParallaxDrawable;

        public DetailsFragmentParallax() {
            super();
            mMinVerticalOffset = PARALLAX_VERTICAL_OFFSET;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Drawable coverDrawable = new FitWidthBitmapDrawable();
            mParallaxDrawable = new DetailsParallaxDrawable(
                    getActivity(),
                    getParallax(),
                    coverDrawable,
                    new ParallaxTarget.PropertyValuesHolderTarget(
                            coverDrawable,
                            PropertyValuesHolder.ofInt("verticalOffset", 0, mMinVerticalOffset)
                    )
            );

            BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
            backgroundManager.attach(getActivity().getWindow());
            backgroundManager.setDrawable(mParallaxDrawable);
        }

        @Override
        public void onStart() {
            super.onStart();
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }

        @Override
        public void onResume() {
            super.onResume();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            ((FitWidthBitmapDrawable) mParallaxDrawable.getCoverDrawable()).setBitmap(bitmap);
        }

        DetailsParallaxDrawable getParallaxDrawable() {
            return mParallaxDrawable;
        }
    }

    @BeforeClass
    public static void setUp() {
        if (SDK_INT >= 31) {
            UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            uiAutomation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS");
            uiAutomation.executeShellCommand("settings put secure immersive_mode_confirmations "
                    + "confirmed");
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @AfterClass
    public static void tearDown() {
        if (SDK_INT >= 31) {
            UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            uiAutomation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS");
            uiAutomation.executeShellCommand("settings delete secure immersive_mode_confirmations");
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void parallaxSetupTest() {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentTest.DetailsFragmentParallax.class,
                new SingleFragmentTestBase.Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        double delta = 0.0002;
        DetailsParallax dpm = ((DetailsFragment) activity.getTestFragment()).getParallax();

        RecyclerViewParallax.ChildPositionProperty frameTop =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowTop();
        assertEquals(0f, frameTop.getFraction(), delta);
        assertEquals(0f, frameTop.getAdapterPosition(), delta);


        RecyclerViewParallax.ChildPositionProperty frameBottom =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowBottom();
        assertEquals(1f, frameBottom.getFraction(), delta);
        assertEquals(0f, frameBottom.getAdapterPosition(), delta);
    }

    @Test
    public void parallaxTest() throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(DetailsFragmentParallax.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentParallax detailsFragment =
                (DetailsFragmentParallax) activity.getTestFragment();
        DetailsParallaxDrawable drawable =
                detailsFragment.getParallaxDrawable();
        final FitWidthBitmapDrawable bitmapDrawable = (FitWidthBitmapDrawable)
                drawable.getCoverDrawable();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsFragment().getAdapter() != null
                        && detailsFragment.getRowsFragment().getAdapter().size() > 1;
            }
        });

        final VerticalGridView verticalGridView = detailsFragment.getRowsFragment()
                .getVerticalGridView();
        final int windowHeight = verticalGridView.getHeight();
        final int windowWidth = verticalGridView.getWidth();
        // make sure background manager attached to window is same size as VerticalGridView
        // i.e. no status bar.
        assertEquals(windowHeight, activity.getWindow().getDecorView().getHeight());
        assertEquals(windowWidth, activity.getWindow().getDecorView().getWidth());

        final View detailsFrame = verticalGridView.findViewById(
                androidx.leanback.R.id.details_frame);

        assertEquals(windowWidth, bitmapDrawable.getBounds().width());

        final Rect detailsFrameRect = new Rect();
        detailsFrameRect.set(0, 0, detailsFrame.getWidth(), detailsFrame.getHeight());
        verticalGridView.offsetDescendantRectToMyCoords(detailsFrame, detailsFrameRect);

        assertEquals(Math.min(windowHeight, detailsFrameRect.top),
                bitmapDrawable.getBounds().height());
        assertEquals(0, bitmapDrawable.getVerticalOffset());

        assertTrue("TitleView is visible", detailsFragment.getView()
                .findViewById(androidx.leanback.R.id.browse_title_group)
                .getVisibility() == View.VISIBLE);

        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                verticalGridView.scrollToPosition(1);
            }
        });

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return bitmapDrawable.getVerticalOffset() == PARALLAX_VERTICAL_OFFSET
                        && detailsFragment.getView()
                        .findViewById(androidx.leanback.R.id.browse_title_group)
                        .getVisibility() != View.VISIBLE;
            }
        });

        detailsFrameRect.set(0, 0, detailsFrame.getWidth(), detailsFrame.getHeight());
        verticalGridView.offsetDescendantRectToMyCoords(detailsFrame, detailsFrameRect);

        assertEquals(0, bitmapDrawable.getBounds().top);
        assertEquals(Math.max(detailsFrameRect.top, 0), bitmapDrawable.getBounds().bottom);
        assertEquals(windowWidth, bitmapDrawable.getBounds().width());

        ColorDrawable colorDrawable = (ColorDrawable) (drawable.getChildAt(1).getDrawable());
        assertEquals(windowWidth, colorDrawable.getBounds().width());
        assertEquals(detailsFrameRect.bottom, colorDrawable.getBounds().top);
        assertEquals(windowHeight, colorDrawable.getBounds().bottom);
    }

    public static class DetailsFragmentWithVideo extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);
        MediaPlayerGlue mGlue;

        public DetailsFragmentWithVideo() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();
            mGlue = new MediaPlayerGlue(getActivity());
            mDetailsBackground.setupVideoPlayback(mGlue);

            mGlue.setMode(MediaPlayerGlue.REPEAT_ALL);
            mGlue.setArtist("A Googleer");
            mGlue.setTitle("Diving with Sharks");
            mGlue.setMediaSource(
                    Uri.parse("android.resource://androidx.leanback.test/raw/video"));
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    public static class DetailsFragmentWithVideo1 extends DetailsFragmentWithVideo {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }
    }

    public static class DetailsFragmentWithVideo2 extends DetailsFragmentWithVideo {

        @Override
        public void onStart() {
            super.onStart();
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }
    }

    private void navigateBetweenRowsAndVideoUsingRequestFocusInternal(Class<?> cls)
            throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentWithVideo detailsFragment =
                (DetailsFragmentWithVideo) activity.getTestFragment();
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int originalFirstRowTop = firstRow.getTop();
        assertTrue(firstRow.hasFocus());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);
        assertTrue(detailsFragment.isShowingTitle());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.mVideoFragment.getView().requestFocus();
            }
        });
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() >= screenHeight;
            }
        });
        assertFalse(detailsFragment.isShowingTitle());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.getRowsFragment().getVerticalGridView().requestFocus();
            }
        });
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() == originalFirstRowTop;
            }
        });
        assertTrue(detailsFragment.isShowingTitle());
    }

    @Test
    public void navigateBetweenRowsAndVideoUsingRequestFocus1() throws Throwable {
        navigateBetweenRowsAndVideoUsingRequestFocusInternal(DetailsFragmentWithVideo1.class);
    }

    @Test
    public void navigateBetweenRowsAndVideoUsingRequestFocus2() throws Throwable {
        navigateBetweenRowsAndVideoUsingRequestFocusInternal(DetailsFragmentWithVideo2.class);
    }

    private void navigateBetweenRowsAndVideoUsingDPADInternal(Class<?> cls) throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentWithVideo detailsFragment =
                (DetailsFragmentWithVideo) activity.getTestFragment();
        // wait video playing
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int originalFirstRowTop = firstRow.getTop();
        assertTrue(firstRow.hasFocus());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);
        assertTrue(detailsFragment.isShowingTitle());

        // navigate to video
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() >= screenHeight;
            }
        });

        // wait auto hide play controls done:
        PollingCheck.waitFor(8000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((PlaybackFragment) detailsFragment.mVideoFragment).mBgAlpha == 0;
            }
        });

        // navigate to details
        sendKeys(KeyEvent.KEYCODE_BACK);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() == originalFirstRowTop;
            }
        });
        assertTrue(detailsFragment.isShowingTitle());
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void navigateBetweenRowsAndVideoUsingDPAD1() throws Throwable {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        navigateBetweenRowsAndVideoUsingDPADInternal(DetailsFragmentWithVideo1.class);
    }

    @FlakyTest(bugId = 228336699)
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void navigateBetweenRowsAndVideoUsingDPAD2() throws Throwable {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        navigateBetweenRowsAndVideoUsingDPADInternal(DetailsFragmentWithVideo2.class);
    }

    public static class EmptyFragmentClass extends Fragment {
        @Override
        public void onStart() {
            super.onStart();
            getActivity().finish();
        }
    }

    private void fragmentOnStartWithVideoInternal(Class<?> cls) throws Throwable {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentWithVideo detailsFragment =
                (DetailsFragmentWithVideo) activity.getTestFragment();
        // wait video playing
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int originalFirstRowTop = firstRow.getTop();
        assertTrue(firstRow.hasFocus());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);
        assertTrue(detailsFragment.isShowingTitle());

        // navigate to video
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() >= screenHeight;
            }
        });

        // start an empty activity
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(activity, SingleFragmentTestActivity.class);
                        intent.putExtra(SingleFragmentTestActivity.EXTRA_FRAGMENT_NAME,
                                EmptyFragmentClass.class.getName());
                        activity.startActivity(intent);
                    }
                }
        );
        PollingCheck.waitFor(2000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.isResumed();
            }
        });
        assertTrue(detailsFragment.mVideoFragment.getView().hasFocus());
    }

    @Test
    public void fragmentOnStartWithVideo1() throws Throwable {
        fragmentOnStartWithVideoInternal(DetailsFragmentWithVideo1.class);
    }

    @Test
    public void fragmentOnStartWithVideo2() throws Throwable {
        fragmentOnStartWithVideoInternal(DetailsFragmentWithVideo2.class);
    }

    @Test
    public void navigateBetweenRowsAndTitle() throws Throwable {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsTestFragment.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsTestFragment detailsFragment =
                (DetailsTestFragment) activity.getTestFragment();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.setOnSearchClickedListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                });
                detailsFragment.setItem(new PhotoItem("Hello world", "Fake content goes here",
                        androidx.leanback.test.R.drawable.spiderman));
            }
        });

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        final int originalFirstRowTop = firstRow.getTop();
        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();

        assertTrue(firstRow.hasFocus());
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(detailsFragment.getTitleView().hasFocus());
        assertEquals(originalFirstRowTop, firstRow.getTop());

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(firstRow.hasFocus());
        assertEquals(originalFirstRowTop, firstRow.getTop());
    }

    public static class DetailsFragmentWithNoVideo extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);

        public DetailsFragmentWithNoVideo() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();

            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Test
    public void lateSetupVideo() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentWithNoVideo.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentWithNoVideo detailsFragment =
                (DetailsFragmentWithNoVideo) activity.getTestFragment();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();

        assertTrue(firstRow.hasFocus());
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        assertTrue(firstRow.hasFocus());

        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue);
                        glue.setMode(MediaPlayerGlue.REPEAT_ALL);
                        glue.setArtist("A Googleer");
                        glue.setTitle("Diving with Sharks");
                        glue.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );

        // after setup Video Playback the DPAD up will navigate to Video Fragment.
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
                @Override
                    public boolean canProceed() {
                        return detailsFragment.mVideoFragment != null
                                && detailsFragment.mVideoFragment.getView() != null;
                }
        });
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        assertTrue(detailsFragment.mVideoFragment.getView().hasFocus());
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((MediaPlayerGlue) detailsFragment.mDetailsBackgroundController
                        .getPlaybackGlue()).isMediaPlaying();
            }
        });
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return 0 == getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController);
            }
        });

        // wait a little bit to replace with new Glue
        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue2 = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue2);
                        glue2.setMode(MediaPlayerGlue.REPEAT_ALL);
                        glue2.setArtist("A Googleer");
                        glue2.setTitle("Diving with Sharks");
                        glue2.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );

        // test switchToRows() and switchToVideo()
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.mDetailsBackgroundController.switchToRows();
                    }
                }
        );
        assertTrue(detailsFragment.mRowsFragment.getView().hasFocus());
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.mDetailsBackgroundController.switchToVideo();
                    }
                }
        );
        assertTrue(detailsFragment.mVideoFragment.getView().hasFocus());
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
    }

    @Test
    public void sharedGlueHost() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentWithNoVideo.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentWithNoVideo detailsFragment =
                (DetailsFragmentWithNoVideo) activity.getTestFragment();

        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue1 = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue1);
                        glue1.setArtist("A Googleer");
                        glue1.setTitle("Diving with Sharks");
                        glue1.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );

        // after setup Video Playback the DPAD up will navigate to Video Fragment.
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null;
            }
        });

        final MediaPlayerGlue glue1 = (MediaPlayerGlue) detailsFragment
                .mDetailsBackgroundController
                .getPlaybackGlue();
        PlaybackGlueHost playbackGlueHost = glue1.getHost();

        // wait a little bit to replace with new Glue
        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue2 = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue2);
                        glue2.setArtist("A Googleer");
                        glue2.setTitle("Diving with Sharks");
                        glue2.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );

        // wait for new glue to get its glue host
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                MediaPlayerGlue mediaPlayerGlue = (MediaPlayerGlue) detailsFragment
                        .mDetailsBackgroundController
                        .getPlaybackGlue();
                return mediaPlayerGlue != null && mediaPlayerGlue != glue1
                        && mediaPlayerGlue.getHost() != null;
            }
        });

        final MediaPlayerGlue glue2 = (MediaPlayerGlue) detailsFragment
                .mDetailsBackgroundController
                .getPlaybackGlue();

        assertTrue(glue1.getHost() == null);
        assertTrue(glue2.getHost() == playbackGlueHost);
    }

    @Test
    public void clearVideo() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentWithNoVideo.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentWithNoVideo detailsFragment =
                (DetailsFragmentWithNoVideo) activity.getTestFragment();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();

        assertTrue(firstRow.hasFocus());
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);

        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue);
                        glue.setMode(MediaPlayerGlue.REPEAT_ALL);
                        glue.setArtist("A Googleer");
                        glue.setTitle("Diving with Sharks");
                        glue.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((MediaPlayerGlue) detailsFragment.mDetailsBackgroundController
                        .getPlaybackGlue()).isMediaPlaying();
            }
        });
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return 0 == getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController);
            }
        });

        // wait a little bit then reset glue
        SystemClock.sleep(1000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(null);
                    }
                }
        );
        // background should fade in upon reset playback
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return 255 == getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController);
            }
        });
    }

    public static class DetailsFragmentWithNoItem extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);

        public DetailsFragmentWithNoItem() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Test
    public void noInitialItem() {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentWithNoItem.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentWithNoItem detailsFragment =
                (DetailsFragmentWithNoItem) activity.getTestFragment();

        final int recyclerViewHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();
        assertTrue(recyclerViewHeight > 0);

        assertEquals(255, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        Drawable coverDrawable = detailsFragment.mDetailsBackgroundController.getCoverDrawable();
        assertEquals(0, coverDrawable.getBounds().top);
        assertEquals(recyclerViewHeight, coverDrawable.getBounds().bottom);
        Drawable bottomDrawable = detailsFragment.mDetailsBackgroundController.getBottomDrawable();
        assertEquals(recyclerViewHeight, bottomDrawable.getBounds().top);
        assertEquals(recyclerViewHeight, bottomDrawable.getBounds().bottom);
    }

    public static class DetailsFragmentSwitchToVideoInOnCreate extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);

        public DetailsFragmentSwitchToVideoInOnCreate() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();
            mDetailsBackground.switchToVideo();
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Test
    public void switchToVideoInOnCreate() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentSwitchToVideoInOnCreate.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentSwitchToVideoInOnCreate detailsFragment =
                (DetailsFragmentSwitchToVideoInOnCreate) activity.getTestFragment();

        // the pending enter transition flag should be automatically cleared
        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTER_TRANSITION_COMPLETE.getStatus());
        assertNull(TransitionHelper.getEnterTransition(activity.getWindow()));
        assertEquals(0, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        assertTrue(detailsFragment.getRowsFragment().getView().hasFocus());
        //SystemClock.sleep(5000);
        assertFalse(detailsFragment.isShowingTitle());

        SystemClock.sleep(1000);
        assertNull(detailsFragment.mVideoFragment);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue);
                        glue.setMode(MediaPlayerGlue.REPEAT_ALL);
                        glue.setArtist("A Googleer");
                        glue.setTitle("Diving with Sharks");
                        glue.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );
        // once the video fragment is created it would be immediately assigned focus
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null
                        && detailsFragment.mVideoFragment.getView().hasFocus();
            }
        });
        // wait auto hide play controls done:
        PollingCheck.waitFor(8000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((PlaybackFragment) detailsFragment.mVideoFragment).mBgAlpha == 0;
            }
        });

        // switchToRows does nothing if there is no row
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.mDetailsBackgroundController.switchToRows();
                    }
                }
        );
        assertTrue(detailsFragment.mVideoFragment.getView().hasFocus());

        // create item, it should be layout outside screen
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.setItem(new PhotoItem("Hello world",
                                "Fake content goes here",
                                androidx.leanback.test.R.drawable.spiderman));
                    }
                }
        );
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getVerticalGridView().getChildCount() > 0
                        && detailsFragment.getVerticalGridView().getChildAt(0).getTop()
                        >= detailsFragment.getVerticalGridView().getHeight();
            }
        });

        // pressing BACK will return to details row
        sendKeys(KeyEvent.KEYCODE_BACK);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getVerticalGridView().getChildAt(0).getTop()
                        < (detailsFragment.getVerticalGridView().getHeight() * 0.7f);
            }
        });
        assertTrue(detailsFragment.getVerticalGridView().getChildAt(0).hasFocus());
    }

    @Test
    public void switchToVideoBackToQuit() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentSwitchToVideoInOnCreate.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentSwitchToVideoInOnCreate detailsFragment =
                (DetailsFragmentSwitchToVideoInOnCreate) activity.getTestFragment();

        // the pending enter transition flag should be automatically cleared
        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTER_TRANSITION_COMPLETE.getStatus());
        assertNull(TransitionHelper.getEnterTransition(activity.getWindow()));
        assertEquals(0, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        assertTrue(detailsFragment.getRowsFragment().getView().hasFocus());
        assertFalse(detailsFragment.isShowingTitle());

        SystemClock.sleep(1000);
        assertNull(detailsFragment.mVideoFragment);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        final MediaPlayerGlue glue = new MediaPlayerGlue(activity);
                        detailsFragment.mDetailsBackgroundController.setupVideoPlayback(glue);
                        glue.setMode(MediaPlayerGlue.REPEAT_ALL);
                        glue.setArtist("A Googleer");
                        glue.setTitle("Diving with Sharks");
                        glue.setMediaSource(Uri.parse(
                                "android.resource://androidx.leanback.test/raw/video"));
                    }
                }
        );
        // once the video fragment is created it would be immediately assigned focus
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoFragment != null
                        && detailsFragment.mVideoFragment.getView() != null
                        && detailsFragment.mVideoFragment.getView().hasFocus();
            }
        });
        // wait auto hide play controls done:
        PollingCheck.waitFor(8000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((PlaybackFragment) detailsFragment.mVideoFragment).mBgAlpha == 0;
            }
        });

        // before any details row is presented, pressing BACK will quit the activity
        sendKeys(KeyEvent.KEYCODE_BACK);
        PollingCheck.waitFor(4000, new PollingCheck.ActivityStop(activity));
    }

    public static class DetailsFragmentSwitchToVideoAndPrepareEntranceTransition
            extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);

        public DetailsFragmentSwitchToVideoAndPrepareEntranceTransition() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();
            mDetailsBackground.switchToVideo();
            prepareEntranceTransition();
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Test
    public void switchToVideoInOnCreateAndPrepareEntranceTransition() {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                DetailsFragmentSwitchToVideoAndPrepareEntranceTransition.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentSwitchToVideoAndPrepareEntranceTransition detailsFragment =
                (DetailsFragmentSwitchToVideoAndPrepareEntranceTransition)
                        activity.getTestFragment();

        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTRANCE_COMPLETE.getStatus());
    }

    public static class DetailsFragmentEntranceTransition
            extends DetailsTestFragment {

        final DetailsFragmentBackgroundController mDetailsBackground =
                new DetailsFragmentBackgroundController(this);

        public DetailsFragmentEntranceTransition() {
            mTimeToLoadOverviewRow = mTimeToLoadRelatedRow = 100;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDetailsBackground.enableParallax();
            prepareEntranceTransition();
        }

        @Override
        public void onStart() {
            super.onStart();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    androidx.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Test
    public void entranceTransitionBlocksSwitchToVideo() {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(DetailsFragmentEntranceTransition.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsFragmentEntranceTransition detailsFragment =
                (DetailsFragmentEntranceTransition)
                        activity.getTestFragment();

        if (Build.VERSION.SDK_INT < 21) {
            // when enter transition is not supported, mCanUseHost is immmediately true
            assertTrue(detailsFragment.mDetailsBackgroundController.mCanUseHost);
        } else {
            // calling switchToVideo() between prepareEntranceTransition and entrance transition
            // finishes will be ignored.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    detailsFragment.mDetailsBackgroundController.switchToVideo();
                }
            });
            assertFalse(detailsFragment.mDetailsBackgroundController.mCanUseHost);
        }
        assertEquals(255, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.setItem(new PhotoItem("Hello world", "Fake content goes here",
                        androidx.leanback.test.R.drawable.spiderman));
                detailsFragment.startEntranceTransition();
            }
        });
        // once Entrance transition is finished, mCanUseHost will be true
        // and we can switchToVideo and fade out the background.
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mDetailsBackgroundController.mCanUseHost;
            }
        });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.mDetailsBackgroundController.switchToVideo();
            }
        });
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return 0 == getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController);
            }
        });
    }

    public static class DetailsFragmentEntranceTransitionTimeout extends DetailsTestFragment {

        public DetailsFragmentEntranceTransitionTimeout() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prepareEntranceTransition();
        }

    }

    @Test
    public void startEntranceTransitionAfterDestroyed() {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                DetailsFragmentEntranceTransition.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN),
                1000);
        final DetailsFragmentEntranceTransition detailsFragment =
                (DetailsFragmentEntranceTransition)
                        activity.getTestFragment();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.setItem(new PhotoItem("Hello world", "Fake content goes here",
                        androidx.leanback.test.R.drawable.spiderman));
            }
        });
        SystemClock.sleep(100);
        activity.finish();
        PollingCheck.waitFor(new PollingCheck.ActivityStop(activity));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.startEntranceTransition();
            }
        });
    }

    public static final class EmptyFragment extends Fragment {
        EditText mEditText;

        @Override
        public View onCreateView(
                final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return mEditText = new EditText(container.getContext());
        }

        @Override
        public void onStart() {
            super.onStart();
            // focus IME on the new fragment because there is a memory leak that IME remembers
            // last editable view, which will cause a false reporting of leaking View.
            InputMethodManager imm =
                    (InputMethodManager) getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            mEditText.requestFocus();
            imm.showSoftInput(mEditText, 0);
        }

        @Override
        public void onDestroyView() {
            mEditText = null;
            super.onDestroyView();
        }
    }

    @Test
    public void viewLeakTest() throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                DetailsFragmentEntranceTransition.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN),
                1000);

        DetailsFragmentEntranceTransition detailsFragment =
                (DetailsFragmentEntranceTransition) activity.getTestFragment();
        View fragmentView = detailsFragment.getView();
        RowsFragment rowsSupportFragment = detailsFragment.getRowsFragment();
        VerticalGridView gridView = rowsSupportFragment.getVerticalGridView();
        LeakDetector leakDetector = new LeakDetector();
        leakDetector.observeObject(fragmentView);
        // Note: RowsFragment is referred by childFragmentManager of details fragment.
        leakDetector.observeObject(gridView);
        leakDetector.observeObject(gridView.getRecycledViewPool());
        leakDetector.observeObject(detailsFragment.mDetailsBackgroundController.mCoverBitmap);
        for (int i = 0; i < gridView.getChildCount(); i++) {
            leakDetector.observeObject(gridView.getChildAt(i));
        }
        fragmentView = null;
        rowsSupportFragment = null;
        gridView = null;
        EmptyFragment emptyFragment = new EmptyFragment();
        activity.getFragmentManager().beginTransaction()
                .replace(R.id.main_frame, emptyFragment)
                .addToBackStack("BK")
                .commit();

        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return emptyFragment.isResumed();
            }
        });
        assertTrue(detailsFragment.mBackgroundDrawable != null);
        leakDetector.assertNoLeak();
    }
}
