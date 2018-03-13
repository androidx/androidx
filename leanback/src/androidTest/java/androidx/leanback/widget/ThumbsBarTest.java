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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import androidx.leanback.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)

public class ThumbsBarTest {
    private Context mContext;
    private ThumbsBar mBar;

    /**
     * Check ThumbsBar's initialization based on the constructor
     */
    @Test
    public void checkThumbsBarInitialize() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        assertEquals(mBar.mThumbHeightInPixel, mContext.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_thumbs_height));
        assertEquals(mBar.mThumbWidthInPixel, mContext.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_thumbs_width));
        assertEquals(mBar.mHeroThumbHeightInPixel, mContext.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_hero_thumbs_height));
        assertEquals(mBar.mHeroThumbWidthInPixel, mContext.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_hero_thumbs_width));
    }

    /**
     * Check getHeroIndex method when input is an even number
     */
    @Test
    public void checkGetHeroIndexOnEvenNumber() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int childCountForTest = 4;
        // according to the algorithm, hero thumb's index should be childCounts / 2
        int expectedHeroIndex = 2;
        when(mBar.getChildCount()).thenReturn(childCountForTest);
        assertEquals(mBar.getHeroIndex(), expectedHeroIndex);
    }

    /**
     * Check getHeroIndex method when input is an odd number.
     */
    @Test
    public void checkGetHeroIndexOnOddNumber() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int childCountForTest = 5;
        // according to the algorithm, hero thumb's index should be childCounts / 2
        int expectedHeroIndex = 2;
        when(mBar.getChildCount()).thenReturn(childCountForTest);
        assertEquals(mBar.getHeroIndex(), expectedHeroIndex);
    }

    /**
     * Check setThumbSize method.
     */
    @Test
    public void checkSetThumbSize() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest = 2560;
        int screenHeightInPixelForTest = 1600;
        int thumbsWidthInPixelForTest = 128;
        int thumbsHeightInPixelForTest = 256;
        // set screen size explicitly so the thumbs bar will have child view inside of it
        mBar.measure(View.MeasureSpec.makeMeasureSpec(screenWidthInPixelForTest,
                View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(screenHeightInPixelForTest,
                        View.MeasureSpec.EXACTLY));

        mBar.setThumbSize(thumbsWidthInPixelForTest, thumbsHeightInPixelForTest);
        // Verify the behavior of setThumbSize method
        assertEquals(mBar.mThumbWidthInPixel, thumbsWidthInPixelForTest);
        assertEquals(mBar.mThumbHeightInPixel, thumbsHeightInPixelForTest);
        // iterate through all child view to test if its width/ height has been set successfully
        for (int i = 0; i < mBar.getChildCount(); i++) {
            if (i != mBar.getHeroIndex()) {
                assertEquals(mBar.getChildAt(i).getLayoutParams().width,
                        thumbsWidthInPixelForTest);
                assertEquals(mBar.getChildAt(i).getLayoutParams().height,
                        thumbsHeightInPixelForTest);
            } else {
                assertEquals(mBar.getChildAt(i).getLayoutParams().width,
                        mContext.getResources().getDimensionPixelSize(
                                R.dimen.lb_playback_transport_hero_thumbs_width));
                assertEquals(mBar.getChildAt(i).getLayoutParams().height,
                        mContext.getResources().getDimensionPixelSize(
                                R.dimen.lb_playback_transport_hero_thumbs_height));
            }
        }
    }

    /**
     * Check setHeroThumbSize method.
     */
    @Test
    public void checkSetHeroThumbSize() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest = 2560;
        int screenHeightInPixelForTest = 1600;
        int HeroThumbsWidthInPixelForTest = 256;
        int HeroThumbsHeightInPixelForTest = 512;
        // set screen size explicitly so the thumbs bar will have child view inside of it
        mBar.measure(View.MeasureSpec.makeMeasureSpec(
                screenWidthInPixelForTest, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        screenHeightInPixelForTest, View.MeasureSpec.EXACTLY));
        mBar.setHeroThumbSize(HeroThumbsWidthInPixelForTest, HeroThumbsHeightInPixelForTest);
        // Verify the behavior of setThumbSize method
        assertEquals(mBar.mHeroThumbWidthInPixel, HeroThumbsWidthInPixelForTest);
        assertEquals(mBar.mHeroThumbHeightInPixel, HeroThumbsHeightInPixelForTest);
        // iterate through all child view to test if its width/ height has been set successfully
        for (int i = 0; i < mBar.getChildCount(); i++) {
            if (i != mBar.getHeroIndex()) {
                assertEquals(mBar.getChildAt(i).getLayoutParams().width,
                        mContext.getResources().getDimensionPixelSize(
                                R.dimen.lb_playback_transport_thumbs_width));
                assertEquals(mBar.getChildAt(i).getLayoutParams().height,
                        mContext.getResources().getDimensionPixelSize(
                                R.dimen.lb_playback_transport_thumbs_height));
            } else {
                assertEquals(mBar.getChildAt(i).getLayoutParams().width,
                        HeroThumbsWidthInPixelForTest);
                assertEquals(mBar.getChildAt(i).getLayoutParams().height,
                        HeroThumbsHeightInPixelForTest);
            }
        }
    }

    /**
     * Check setThumbSpace method.
     */
    @Test
    public void checkSetThumbSpace() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int thumbSpaceInPixelForTest = 48;
        mBar.setThumbSpace(thumbSpaceInPixelForTest);
        assertEquals(mBar.mMeasuredMarginInPixel, thumbSpaceInPixelForTest);
        verify(mBar).requestLayout();
    }

    /**
     * check calculateNumberOfThumbs method when the result from roundUp function is less than 2
     *
     * Firstly, to make sure the test cases can run on different devices with different screen
     * density (i.e. The return value from roundUp function should be the same no matter what kind
     * of device/ emulator is connected),
     * the screen width for test is set using dp, the pixel value will be computed by
     * multiplying context.getResources().getDisplayMetrics().density
     *
     * In this test case, the screen width is set to 240 in dp, so the calculation result should
     * be 1. According to the algorithm of calculateNumOfThumbs, it should be reassigned to 2 and
     * the final result should be 3 after counting the hero thumb.
     */
    @Test
    public void checkCalculateNumberOfThumbs1() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest =
                (int) (240 * mContext.getResources().getDisplayMetrics().density);
        int screenHeightInPixelForTest =
                (int) (240 * mContext.getResources().getDisplayMetrics().density);
        int expectedChildCounts = 3;
        mBar.measure(View.MeasureSpec.makeMeasureSpec(
                screenWidthInPixelForTest, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        screenHeightInPixelForTest, View.MeasureSpec.EXACTLY));
        assertEquals(mBar.getChildCount(), expectedChildCounts);
    }

    /**
     * check calculateNumberOfThumbs method when the result from roundUp function is an odd number
     * and larger than 2.
     *
     * In this test case, the screen width is set to 680 in dp, so the calculation result should
     * be 3. According to the algorithm of calculateNumOfThumbs, it should be incremented by 1, so
     * the final result is 5 after counting the hero thumb.
     */
    @Test
    public void checkCalculateNumberOfThumbs2() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest =
                (int) (680 * mContext.getResources().getDisplayMetrics().density);
        int screenHeightInPixelForTest =
                (int) (680 * mContext.getResources().getDisplayMetrics().density);
        int expectedChildCounts = 5;
        mBar.measure(View.MeasureSpec.makeMeasureSpec(
                screenWidthInPixelForTest, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        screenHeightInPixelForTest, View.MeasureSpec.EXACTLY));
        assertEquals(mBar.getChildCount(), expectedChildCounts);
    }

    /**
     * check calculateNumberOfThumbs method when the result from roundUp function is an even number
     * and larger than 2
     *
     * In this test case, the screen width is set to 800 in dp, so the calculation result should
     * be 4. Finally the result is expected to be 5 after counting the hero thumb.
     */
    @Test
    public void checkCalculateNumberOfThumbs3() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest =
                (int) (800 * mContext.getResources().getDisplayMetrics().density);
        int screenHeightInPixelForTest =
                (int) (800 * mContext.getResources().getDisplayMetrics().density);
        int expectedChildCounts = 5;
        mBar.measure(View.MeasureSpec.makeMeasureSpec(
                screenWidthInPixelForTest, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        screenHeightInPixelForTest, View.MeasureSpec.EXACTLY));
        assertEquals(mBar.getChildCount(), expectedChildCounts);
    }

    /**
     * check setNumberOfThumbs method
     *
     * When user calling setNumberOfThumbs(int numOfThumbs) method. The flag mIsUserSets will be
     * toggled to true to honor user's choice, and the result of child view's number should
     * not be impacted by calculateNumberOfThumbs(int widthInPixel) method.
     *
     * In this test case, the screen width is set to 960 in dp, the calculation result from
     * calculateNumberOfThumbs method should be 5. But after calling setNumberOfThumbs function to
     * set thumbs' number to 3, this value should not impact the final result.
     */
    @Test
    public void checkSetNumberOfThumbs() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mBar = Mockito.spy(new ThumbsBar(mContext, null));
            }
        });
        int screenWidthInPixelForTest =
                (int) (960 * mContext.getResources().getDisplayMetrics().density);
        int screenHeightInPixelForTest =
                (int) (960 * mContext.getResources().getDisplayMetrics().density);
        int numberOfThumbs = 3;
        mBar.setNumberOfThumbs(numberOfThumbs);
        mBar.measure(View.MeasureSpec.makeMeasureSpec(
                screenWidthInPixelForTest, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        screenHeightInPixelForTest, View.MeasureSpec.EXACTLY));
        assertEquals(mBar.getChildCount(), numberOfThumbs);
    }
}
