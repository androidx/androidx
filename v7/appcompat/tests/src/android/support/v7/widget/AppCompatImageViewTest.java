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
package android.support.v7.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;

import android.graphics.drawable.Drawable;
import android.support.test.espresso.ViewInteraction;
import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.TestUtilsMatchers;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.ImageView;

import org.junit.Test;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatImageView} class.
 */
@SmallTest
public class AppCompatImageViewTest
        extends AppCompatBaseViewTest<AppCompatImageViewActivity, AppCompatImageView> {
    public AppCompatImageViewTest() {
        super(AppCompatImageViewActivity.class);
    }

    @Test
    public void testImageViewBothSrcCompatAndroidSrcSet() {
        final int expectedColor = mContainer.getResources().getColor(R.color.test_blue);
        ViewInteraction imageViewInteration = onView(withId(R.id.view_android_src_srccompat));
        imageViewInteration.check(matches(TestUtilsMatchers.drawable(expectedColor)));
    }
}
