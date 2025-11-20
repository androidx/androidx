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

import androidx.fragment.app.Fragment;
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
 * Unit tests for {@link DetailsSupportFragment}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@FlakyTest(bugId = 207674174)
public class DetailsSupportFragmentTest extends SingleSupportFragmentTestBase {

    static final int PARALLAX_VERTICAL_OFFSET = -300;

    static int getCoverDrawableAlpha(DetailsSupportFragmentBackgroundController controller) {
        return ((FitWidthBitmapDrawable) controller.mParallaxDrawable.getCoverDrawable())
                .getAlpha();
    }

    public static class DetailsSupportFragmentParallax extends DetailsTestSupportFragment {

        private DetailsParallaxDrawable mParallaxDrawable;

        public DetailsSupportFragmentParallax() {
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
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentTest.DetailsSupportFragmentParallax.class,
                new SingleSupportFragmentTestBase.Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        double delta = 0.0002;
        DetailsParallax dpm = ((DetailsSupportFragment) activity.getTestFragment()).getParallax();

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
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(DetailsSupportFragmentParallax.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsSupportFragmentParallax detailsFragment =
                (DetailsSupportFragmentParallax) activity.getTestFragment();
        DetailsParallaxDrawable drawable =
                detailsFragment.getParallaxDrawable();
        final FitWidthBitmapDrawable bitmapDrawable = (FitWidthBitmapDrawable)
                drawable.getCoverDrawable();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsSupportFragment().getAdapter() != null
                        && detailsFragment.getRowsSupportFragment().getAdapter().size() > 1;
            }
        });

        final VerticalGridView verticalGridView = detailsFragment.getRowsSupportFragment()
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

    public static class DetailsSupportFragmentWithVideo extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);
        MediaPlayerGlue mGlue;

        public DetailsSupportFragmentWithVideo() {
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

    public static class DetailsSupportFragmentWithVideo1 extends DetailsSupportFragmentWithVideo {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }
    }

    public static class DetailsSupportFragmentWithVideo2 extends DetailsSupportFragmentWithVideo {

        @Override
        public void onStart() {
            super.onStart();
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    androidx.leanback.test.R.drawable.spiderman));
        }
    }

    private void navigateBetweenRowsAndVideoUsingRequestFocusInternal(Class<?> cls)
            throws Throwable {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsSupportFragmentWithVideo detailsFragment =
                (DetailsSupportFragmentWithVideo) activity.getTestFragment();
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
        final int originalFirstRowTop = firstRow.getTop();
        assertTrue(firstRow.hasFocus());
        assertTrue(firstRow.getTop() > 0 && firstRow.getTop() < screenHeight);
        assertTrue(detailsFragment.isShowingTitle());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.mVideoSupportFragment.getView().requestFocus();
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
                detailsFragment.getRowsSupportFragment().getVerticalGridView().requestFocus();
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
        navigateBetweenRowsAndVideoUsingRequestFocusInternal(DetailsSupportFragmentWithVideo1.class);
    }

    @Test
    public void navigateBetweenRowsAndVideoUsingRequestFocus2() throws Throwable {
        navigateBetweenRowsAndVideoUsingRequestFocusInternal(DetailsSupportFragmentWithVideo2.class);
    }

    private void navigateBetweenRowsAndVideoUsingDPADInternal(Class<?> cls) throws Throwable {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsSupportFragmentWithVideo detailsFragment =
                (DetailsSupportFragmentWithVideo) activity.getTestFragment();
        // wait video playing
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
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
                return ((PlaybackSupportFragment) detailsFragment.mVideoSupportFragment).mBgAlpha == 0;
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

        navigateBetweenRowsAndVideoUsingDPADInternal(DetailsSupportFragmentWithVideo1.class);
    }

    @FlakyTest(bugId = 228336699)
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void navigateBetweenRowsAndVideoUsingDPAD2() throws Throwable {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        navigateBetweenRowsAndVideoUsingDPADInternal(DetailsSupportFragmentWithVideo2.class);
    }

    public static class EmptyFragmentClass extends Fragment {
        @Override
        public void onStart() {
            super.onStart();
            getActivity().finish();
        }
    }

    private void fragmentOnStartWithVideoInternal(Class<?> cls) throws Throwable {
        final SingleSupportFragmentTestActivity activity = launchAndWaitActivity(cls,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsSupportFragmentWithVideo detailsFragment =
                (DetailsSupportFragmentWithVideo) activity.getTestFragment();
        // wait video playing
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null
                        && detailsFragment.mGlue.isMediaPlaying();
            }
        });

        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
                .getHeight();
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
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
                        Intent intent = new Intent(activity, SingleSupportFragmentTestActivity.class);
                        intent.putExtra(SingleSupportFragmentTestActivity.EXTRA_FRAGMENT_NAME,
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
        assertTrue(detailsFragment.mVideoSupportFragment.getView().hasFocus());
    }

    @Test
    public void fragmentOnStartWithVideo1() throws Throwable {
        fragmentOnStartWithVideoInternal(DetailsSupportFragmentWithVideo1.class);
    }

    @Test
    public void fragmentOnStartWithVideo2() throws Throwable {
        fragmentOnStartWithVideoInternal(DetailsSupportFragmentWithVideo2.class);
    }

    @Test
    public void navigateBetweenRowsAndTitle() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsTestSupportFragment.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsTestSupportFragment detailsFragment =
                (DetailsTestSupportFragment) activity.getTestFragment();

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
                return detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        final int originalFirstRowTop = firstRow.getTop();
        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
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

    public static class DetailsSupportFragmentWithNoVideo extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);

        public DetailsSupportFragmentWithNoVideo() {
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
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentWithNoVideo.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentWithNoVideo detailsFragment =
                (DetailsSupportFragmentWithNoVideo) activity.getTestFragment();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
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
                        return detailsFragment.mVideoSupportFragment != null
                                && detailsFragment.mVideoSupportFragment.getView() != null;
                }
        });
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        assertTrue(detailsFragment.mVideoSupportFragment.getView().hasFocus());
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
        assertTrue(detailsFragment.mRowsSupportFragment.getView().hasFocus());
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        detailsFragment.mDetailsBackgroundController.switchToVideo();
                    }
                }
        );
        assertTrue(detailsFragment.mVideoSupportFragment.getView().hasFocus());
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(firstRow));
    }

    @Test
    public void sharedGlueHost() {
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentWithNoVideo.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentWithNoVideo detailsFragment =
                (DetailsSupportFragmentWithNoVideo) activity.getTestFragment();

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
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null;
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
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentWithNoVideo.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentWithNoVideo detailsFragment =
                (DetailsSupportFragmentWithNoVideo) activity.getTestFragment();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsSupportFragment().getVerticalGridView().getChildAt(0);
        final int screenHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
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

    public static class DetailsSupportFragmentWithNoItem extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);

        public DetailsSupportFragmentWithNoItem() {
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
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentWithNoItem.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentWithNoItem detailsFragment =
                (DetailsSupportFragmentWithNoItem) activity.getTestFragment();

        final int recyclerViewHeight = detailsFragment.getRowsSupportFragment().getVerticalGridView()
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

    public static class DetailsSupportFragmentSwitchToVideoInOnCreate extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);

        public DetailsSupportFragmentSwitchToVideoInOnCreate() {
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
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentSwitchToVideoInOnCreate.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentSwitchToVideoInOnCreate detailsFragment =
                (DetailsSupportFragmentSwitchToVideoInOnCreate) activity.getTestFragment();

        // the pending enter transition flag should be automatically cleared
        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTER_TRANSITION_COMPLETE.getStatus());
        assertNull(TransitionHelper.getEnterTransition(activity.getWindow()));
        assertEquals(0, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        assertTrue(detailsFragment.getRowsSupportFragment().getView().hasFocus());
        //SystemClock.sleep(5000);
        assertFalse(detailsFragment.isShowingTitle());

        SystemClock.sleep(1000);
        assertNull(detailsFragment.mVideoSupportFragment);
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
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null
                        && detailsFragment.mVideoSupportFragment.getView().hasFocus();
            }
        });
        // wait auto hide play controls done:
        PollingCheck.waitFor(8000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((PlaybackSupportFragment) detailsFragment.mVideoSupportFragment).mBgAlpha == 0;
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
        assertTrue(detailsFragment.mVideoSupportFragment.getView().hasFocus());

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
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentSwitchToVideoInOnCreate.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentSwitchToVideoInOnCreate detailsFragment =
                (DetailsSupportFragmentSwitchToVideoInOnCreate) activity.getTestFragment();

        // the pending enter transition flag should be automatically cleared
        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTER_TRANSITION_COMPLETE.getStatus());
        assertNull(TransitionHelper.getEnterTransition(activity.getWindow()));
        assertEquals(0, getCoverDrawableAlpha(detailsFragment.mDetailsBackgroundController));
        assertTrue(detailsFragment.getRowsSupportFragment().getView().hasFocus());
        assertFalse(detailsFragment.isShowingTitle());

        SystemClock.sleep(1000);
        assertNull(detailsFragment.mVideoSupportFragment);
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
                return detailsFragment.mVideoSupportFragment != null
                        && detailsFragment.mVideoSupportFragment.getView() != null
                        && detailsFragment.mVideoSupportFragment.getView().hasFocus();
            }
        });
        // wait auto hide play controls done:
        PollingCheck.waitFor(8000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((PlaybackSupportFragment) detailsFragment.mVideoSupportFragment).mBgAlpha == 0;
            }
        });

        // before any details row is presented, pressing BACK will quit the activity
        sendKeys(KeyEvent.KEYCODE_BACK);
        PollingCheck.waitFor(4000, new PollingCheck.ActivityStop(activity));
    }

    public static class DetailsSupportFragmentSwitchToVideoAndPrepareEntranceTransition
            extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);

        public DetailsSupportFragmentSwitchToVideoAndPrepareEntranceTransition() {
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
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                DetailsSupportFragmentSwitchToVideoAndPrepareEntranceTransition.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentSwitchToVideoAndPrepareEntranceTransition detailsFragment =
                (DetailsSupportFragmentSwitchToVideoAndPrepareEntranceTransition)
                        activity.getTestFragment();

        assertEquals(StateMachine.STATUS_INVOKED,
                detailsFragment.STATE_ENTRANCE_COMPLETE.getStatus());
    }

    public static class DetailsSupportFragmentEntranceTransition
            extends DetailsTestSupportFragment {

        final DetailsSupportFragmentBackgroundController mDetailsBackground =
                new DetailsSupportFragmentBackgroundController(this);

        public DetailsSupportFragmentEntranceTransition() {
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
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(DetailsSupportFragmentEntranceTransition.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsSupportFragmentEntranceTransition detailsFragment =
                (DetailsSupportFragmentEntranceTransition)
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

    public static class DetailsSupportFragmentEntranceTransitionTimeout extends DetailsTestSupportFragment {

        public DetailsSupportFragmentEntranceTransitionTimeout() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prepareEntranceTransition();
        }

    }

    @Test
    public void startEntranceTransitionAfterDestroyed() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                DetailsSupportFragmentEntranceTransition.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN),
                1000);
        final DetailsSupportFragmentEntranceTransition detailsFragment =
                (DetailsSupportFragmentEntranceTransition)
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
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                DetailsSupportFragmentEntranceTransition.class, new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN),
                1000);

        DetailsSupportFragmentEntranceTransition detailsFragment =
                (DetailsSupportFragmentEntranceTransition) activity.getTestFragment();
        View fragmentView = detailsFragment.getView();
        RowsSupportFragment rowsSupportFragment = detailsFragment.getRowsSupportFragment();
        VerticalGridView gridView = rowsSupportFragment.getVerticalGridView();
        LeakDetector leakDetector = new LeakDetector();
        leakDetector.observeObject(fragmentView);
        // Note: RowsSupportFragment is referred by childFragmentManager of details fragment.
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
        activity.getSupportFragmentManager().beginTransaction()
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
