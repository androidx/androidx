/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.widget.ImageView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class ImageViewStyleTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testStyleWithNothing() {
        ImageViewStyle.Builder builder = new ImageViewStyle.Builder();
        ImageViewStyle style = builder.build();
        ImageView imageView = new ImageView(mContext);
        style.applyStyleOnImageViewIfValid(imageView);
    }

    @Test
    public void testStyleWithEverything() {
        ImageViewStyle.Builder builder = new ImageViewStyle.Builder();
        ImageViewStyle style = builder
                .setScaleType(ImageView.ScaleType.FIT_START)
                .setPadding(1, 2, 3, 4)
                .setLayoutMargin(5, 6, 7, 8)
                .setBackgroundColor(Color.YELLOW)
                .build();
        ImageView imageView = new ImageView(mContext);
        imageView.setImageIcon(Icon.createWithResource(mContext,
                androidx.autofill.test.R.drawable.ic_settings));
        style.applyStyleOnImageViewIfValid(imageView);

        Assert.assertEquals(ImageView.ScaleType.FIT_START, imageView.getScaleType());

        TestUtils.verifyPadding(imageView, 1, 2, 3, 4);
        TestUtils.verifyLayoutMargin(imageView, 5, 6, 7, 8);
        TestUtils.verifyBackgroundColor(imageView, Color.YELLOW);
    }
}
