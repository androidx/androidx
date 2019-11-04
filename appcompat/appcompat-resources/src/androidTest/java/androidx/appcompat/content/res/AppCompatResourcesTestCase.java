/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.appcompat.content.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.appcompat.resources.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppCompatResourcesTestCase {
    private Context mContext;

    public AppCompatResourcesTestCase() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testColorStateListCaching() {
        final ColorStateList result1 = AppCompatResources.getColorStateList(
                mContext, R.color.color_state_list_themed_attrs);
        final ColorStateList result2 = AppCompatResources.getColorStateList(
                mContext, R.color.color_state_list_themed_attrs);
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Test
    public void testGetDrawableVectorResource() {
        assertNotNull(AppCompatResources.getDrawable(mContext, R.drawable.test_vector_off));
    }

    @Test
    public void testGetAnimatedStateListDrawable() {
        assertNotNull(AppCompatResources.getDrawable(mContext, R.drawable.asl_heart));
    }
}
