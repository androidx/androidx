/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertThrows;

import android.view.View.MeasureSpec;
import android.widget.ImageView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.renderer.test.R;

import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RatioViewWrapperTest {
    @Rule public Expect expect = Expect.create();

    private ImageView mInnerImageView;

    @Before
    public void setUp() {
        mInnerImageView = new ImageView(getApplicationContext());
        mInnerImageView.setImageResource(R.drawable.android_24dp);
    }

    @Test
    public void twoExactlyConstraintsAreRespected() {
        final int width = 100;
        final int height = 50;
        final float ratio = 1.0f; // We don't actually care for now.

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.addView(mInnerImageView);
        viewWrapper.setAspectRatio(ratio);

        // Regardless of the aspect, both of these are exact measurespecs, so should be respected.
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        viewWrapper.measure(widthMeasureSpec, heightMeasureSpec);

        assertDimensions(viewWrapper, mInnerImageView, width, height);
    }

    @Test
    public void fixedWidthScalesProportionally() {
        final int width = 100;
        final float ratio = 2.0f; // Width = 2 * height

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.addView(mInnerImageView);
        viewWrapper.setAspectRatio(ratio);

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        // Emulates a "WRAP_CONTENT" within a container.
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST);
        viewWrapper.measure(widthMeasureSpec, heightMeasureSpec);

        assertDimensions(viewWrapper, mInnerImageView, width, 50);
    }

    @Test
    public void fixedHeightScalesProportionally() {
        final int height = 100;
        final float ratio = 0.5f; // width = height / 2

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.addView(mInnerImageView);
        viewWrapper.setAspectRatio(ratio);

        // Emulates WRAP_CONTENT
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        viewWrapper.measure(widthMeasureSpec, heightMeasureSpec);

        assertDimensions(viewWrapper, mInnerImageView, 50, height);
    }

    @Test
    public void canExceedParentWidthToKeepRatio() {
        final int height = 100;
        final float ratio = 2.0f; // Width = height * 2

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.addView(mInnerImageView);
        viewWrapper.setAspectRatio(ratio);

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST);
        // Emulates a "WRAP_CONTENT" within a container.
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        viewWrapper.measure(widthMeasureSpec, heightMeasureSpec);

        assertDimensions(viewWrapper, mInnerImageView, 200, height);
    }

    @Test
    public void canExceedParentHeightToKeepRatio() {
        // We probably shouldn't do this, but the RatioViewWrapper will exceed the bounds of the
        // parent if needed to keep the ratio.

        final int width = 100;
        final float ratio = 0.5f; // Width = height / 2

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.addView(mInnerImageView);
        viewWrapper.setAspectRatio(ratio);

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        // Emulates a "WRAP_CONTENT" within a container.
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST);
        viewWrapper.measure(widthMeasureSpec, heightMeasureSpec);

        assertDimensions(viewWrapper, mInnerImageView, width, 200);
    }

    @Test
    public void cannotHaveZeroChildren() {
        final int width = 100;
        final int height = 100;

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.setAspectRatio(1.0f);

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        assertThrows(
                IllegalStateException.class,
                () -> viewWrapper.measure(widthMeasureSpec, heightMeasureSpec));
    }

    @Test
    public void cannotHaveMoreThanOneChild() {
        final int width = 100;
        final int height = 100;

        RatioViewWrapper viewWrapper = new RatioViewWrapper(getApplicationContext());
        viewWrapper.setAspectRatio(1.0f);

        viewWrapper.addView(new ImageView(getApplicationContext()));
        viewWrapper.addView(new ImageView(getApplicationContext()));

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        assertThrows(
                IllegalStateException.class,
                () -> viewWrapper.measure(widthMeasureSpec, heightMeasureSpec));
    }

    private void assertDimensions(
            RatioViewWrapper ratioViewWrapper, ImageView imageView, int width, int height) {
        expect.that(ratioViewWrapper.getMeasuredWidth()).isEqualTo(width);
        expect.that(ratioViewWrapper.getMeasuredHeight()).isEqualTo(height);
        expect.that(imageView.getMeasuredWidth()).isEqualTo(width);
        expect.that(imageView.getMeasuredHeight()).isEqualTo(height);
    }
}
