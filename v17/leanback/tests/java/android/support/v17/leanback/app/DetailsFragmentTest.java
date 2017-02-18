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

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.Suppress;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.FitWidthBitmapDrawable;
import android.support.v17.leanback.media.MediaPlayerGlue;
import android.support.v17.leanback.testutils.PollingCheck;
import android.support.v17.leanback.widget.DetailsParallax;
import android.support.v17.leanback.widget.DetailsParallaxDrawable;
import android.support.v17.leanback.widget.ParallaxTarget;
import android.support.v17.leanback.widget.RecyclerViewParallax;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.KeyEvent;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link DetailsFragment}.
 */
@RunWith(JUnit4.class)
@MediumTest
public class DetailsFragmentTest extends SingleFragmentTestBase {

    static final int PARALLAX_VERTICAL_OFFSET = -300;

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
                    android.support.v17.leanback.test.R.drawable.spiderman));
        }

        @Override
        public void onResume() {
            super.onResume();
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    android.support.v17.leanback.test.R.drawable.spiderman);
            ((FitWidthBitmapDrawable) mParallaxDrawable.getCoverDrawable()).setBitmap(bitmap);
        }

        DetailsParallaxDrawable getParallaxDrawable() {
            return mParallaxDrawable;
        }
    }

    @Test
    public void parallaxSetupTest() {
        launchAndWaitActivity(DetailsFragmentTest.DetailsFragmentParallax.class,
                new SingleFragmentTestBase.Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        double delta = 0.0002;
        DetailsParallax dpm = ((DetailsFragment) mActivity.getTestFragment()).getParallax();

        RecyclerViewParallax.ChildPositionProperty frameTop =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowTop();
        assertEquals(0f, frameTop.getFraction(), delta);
        assertEquals(0f, frameTop.getAdapterPosition(), delta);


        RecyclerViewParallax.ChildPositionProperty frameBottom =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowBottom();
        assertEquals(1f, frameBottom.getFraction(), delta);
        assertEquals(0f, frameBottom.getAdapterPosition(), delta);
    }

    @Suppress // Disabled due to flakiness.
    @Test
    public void parallaxTest() throws Throwable {
        launchAndWaitActivity(DetailsFragmentParallax.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentParallax detailsFragment =
                (DetailsFragmentParallax) mActivity.getTestFragment();
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
        assertEquals(windowHeight, mActivity.getWindow().getDecorView().getHeight());
        assertEquals(windowWidth, mActivity.getWindow().getDecorView().getWidth());

        final View detailsFrame = verticalGridView.findViewById(R.id.details_frame);

        assertEquals(windowWidth, bitmapDrawable.getBounds().width());

        final Rect detailsFrameRect = new Rect();
        detailsFrameRect.set(0, 0, detailsFrame.getWidth(), detailsFrame.getHeight());
        verticalGridView.offsetDescendantRectToMyCoords(detailsFrame, detailsFrameRect);

        assertEquals(Math.min(windowHeight, detailsFrameRect.top),
                bitmapDrawable.getBounds().height());
        assertEquals(0, bitmapDrawable.getVerticalOffset());

        assertTrue("TitleView is visible", detailsFragment.getView()
                .findViewById(R.id.browse_title_group).getVisibility() == View.VISIBLE);

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
                        .findViewById(R.id.browse_title_group).getVisibility() != View.VISIBLE;
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
                    Uri.parse("android.resource://android.support.v17.leanback.test/raw/video"));
        }

        @Override
        public void onStart() {
            super.onStart();
            setItem(new PhotoItem("Hello world", "Fake content goes here",
                    android.support.v17.leanback.test.R.drawable.spiderman));
            Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                    android.support.v17.leanback.test.R.drawable.spiderman);
            mDetailsBackground.setCoverBitmap(bitmap);
        }

        @Override
        public void onStop() {
            mDetailsBackground.setCoverBitmap(null);
            super.onStop();
        }
    }

    @Suppress // Disabled due to flakiness.
    @Test
    public void navigateBetweenRowsAndVideoUsingRequestFocus() throws Throwable {
        launchAndWaitActivity(DetailsFragmentWithVideo.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentWithVideo detailsFragment =
                (DetailsFragmentWithVideo) mActivity.getTestFragment();
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
        assertTrue(firstRow.getTop() < screenHeight);
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

    @Suppress // Disabled due to flakiness.
    @Test
    public void navigateBetweenRowsAndVideoUsingDPAD() throws Throwable {
        launchAndWaitActivity(DetailsFragmentWithVideo.class,
                new Options().uiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);

        final DetailsFragmentWithVideo detailsFragment =
                (DetailsFragmentWithVideo) mActivity.getTestFragment();
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
        assertTrue(firstRow.getTop() < screenHeight);
        assertTrue(detailsFragment.isShowingTitle());

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() >= screenHeight;
            }
        });
        assertFalse(detailsFragment.isShowingTitle());

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstRow.getTop() == originalFirstRowTop;
            }
        });
        assertTrue(detailsFragment.isShowingTitle());
    }

    @Test
    public void navigateBetweenRowsAndTitle() throws Throwable {
        launchAndWaitActivity(DetailsTestFragment.class, new Options().uiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN), 0);
        final DetailsTestFragment detailsFragment =
                (DetailsTestFragment) mActivity.getTestFragment();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.setOnSearchClickedListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                });
                detailsFragment.setItem(new PhotoItem("Hello world", "Fake content goes here",
                        android.support.v17.leanback.test.R.drawable.spiderman));
            }
        });

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return detailsFragment.getRowsFragment().getVerticalGridView().getChildCount() > 0;
            }
        });
        final View firstRow = detailsFragment.getRowsFragment().getVerticalGridView().getChildAt(0);
        final int originalFirstRowTop = firstRow.getTop();
        final int screenHeight = detailsFragment.getRowsFragment().getVerticalGridView()
                .getHeight();

        assertTrue(firstRow.hasFocus());
        assertTrue(detailsFragment.isShowingTitle());
        assertTrue(firstRow.getTop() < screenHeight);

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

}
