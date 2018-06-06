/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.appcompat.res.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.test.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatResourcesTestCase {
    @Rule
    public final ActivityTestRule<AppCompatActivity> mActivityTestRule;

    public AppCompatResourcesTestCase() {
        mActivityTestRule = new ActivityTestRule<>(AppCompatActivity.class);
    }

    @Test
    public void testColorStateListCaching() {
        final Activity context = mActivityTestRule.getActivity();
        final ColorStateList result1 = AppCompatResources.getColorStateList(
                context, R.color.color_state_list_themed_attrs);
        final ColorStateList result2 = AppCompatResources.getColorStateList(
                context, R.color.color_state_list_themed_attrs);
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Test
    public void testGetDrawableVectorResource() {
        final Activity context = mActivityTestRule.getActivity();
        assertNotNull(AppCompatResources.getDrawable(context, R.drawable.test_vector_off));
    }

    @Test
    public void testGetAnimatedStateListDrawable() {
        final Activity context = mActivityTestRule.getActivity();
        assertNotNull(AppCompatResources.getDrawable(context, R.drawable.asl_heart));
    }

}
