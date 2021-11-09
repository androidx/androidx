/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.core.graphics.drawable;

import static org.mockito.Mockito.verify;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class WrappedDrawableApi14Test {

    /**
     * Ensure setLayoutDirection is propagated to wrapped drawables.
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testSetLayoutDirection() {
        // Note that Mockito is VERY SLOW on CF targets, so this test must be medium+.
        Drawable baseDrawable = Mockito.spy(new ColorDrawable());
        WrappedDrawableApi14 drawable = new WrappedDrawableApi14(baseDrawable);
        drawable.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        verify(baseDrawable).setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    }
}
