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

package androidx.wear.watchface.complications.rendering.test;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ComplicationDrawableAndroidTest {
    private int mDefaultTextSize;

    @Before
    public void setUp() {
        int textSize = R.dimen.complicationDrawable_textSize;
        mDefaultTextSize =
                ApplicationProvider.getApplicationContext()
                        .getResources()
                        .getDimensionPixelSize(textSize);
    }

    @Test
    public void defaultValuesAreLoadedAfterLoadingFromResource() {
        ComplicationDrawable drawable =
                (ComplicationDrawable)
                        ApplicationProvider.getApplicationContext().getResources().getDrawable(
                                R.drawable.default_complication_drawable, null);
        int textSizeFromResources = drawable.getActiveStyle().getTextSize();
        assertThat(textSizeFromResources).isEqualTo(mDefaultTextSize);
    }

    @Test
    public void inflateFromEmptyTag() {
        ComplicationDrawable drawable =
                (ComplicationDrawable)
                        ApplicationProvider.getApplicationContext().getResources().getDrawable(
                                R.drawable.default_complication_drawable, null);
        assertThat(drawable).isNotNull();
    }
}
