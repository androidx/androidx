/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.res.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.BaseInstrumentationTestCase;
import android.support.v7.appcompat.test.R;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.testutils.TestUtils;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;

@SmallTest
public class AppCompatResourcesTestCase extends BaseInstrumentationTestCase<AppCompatActivity> {

    public AppCompatResourcesTestCase() {
        super(AppCompatActivity.class);
    }

    @Test
    public void testGetColorStateListWithThemedAttributes() {
        final Activity context = getActivity();

        final int colorForegound = TestUtils.getThemeAttrColor(
                context, android.R.attr.colorForeground);

        final ColorStateList result = AppCompatResources.getColorStateList(
                context, R.color.color_state_list_themed_attrs);

        assertNotNull(result);

        // Now check the state colors

        // Disabled color should equals colorForeground with 50% of its alpha
        final int expectedDisabled = ColorUtils.setAlphaComponent(colorForegound,
                Math.round(Color.alpha(colorForegound) * 0.5f));
        assertEquals(expectedDisabled, result.getColorForState(
                new int[]{-android.R.attr.state_enabled}, 0));

        // Default color should equal colorForeground
        assertEquals(colorForegound, result.getDefaultColor());
    }

    @Test
    public void testGetDrawableVectorResource() {
        final Activity context = getActivity();
        assertNotNull(AppCompatResources.getDrawable(context, R.drawable.test_vector_off));
    }

}
