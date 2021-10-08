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

package androidx.appcompat.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.appcompat.resources.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("deprecation")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TintResourcesTest {
    @Rule
    public final androidx.test.rule.ActivityTestRule<Activity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(Activity.class);

    /**
     * Ensures that TintResources delegates calls to the wrapped Resources object.
     */
    @Test
    public void testTintResourcesDelegateBackToOriginalResources() {
        final TestResources testResources =
                new TestResources(mActivityTestRule.getActivity().getResources());

        // First make sure that the flag is false
        testResources.resetGetDrawableCalled();
        assertFalse(testResources.wasGetDrawableCalled());

        // Now wrap in a TintResources instance and get a Drawable
        final TintResources tintResources =
                new TintResources(mActivityTestRule.getActivity(), testResources);
        tintResources.getDrawable(android.R.drawable.ic_delete);

        // We can't delegate to the wrapped Resource object's getDrawable() because it will break
        // support for nested vector-enabled tinted resources.
        assertFalse(testResources.wasGetDrawableCalled());

        tintResources.getString(android.R.string.ok);

        // However, we can still delegate other calls.
        assertTrue(testResources.wasGetStringCalled());
    }

    public void testNestedTintResources() {
        Resources tintResources = new TintResources(mActivityTestRule.getActivity(),
                        mActivityTestRule.getActivity().getResources());
        Drawable d = tintResources.getDrawable(R.drawable.tint_nested);
        assertTrue(d instanceof LayerDrawable);

        // Color filter is applied to nested drawable.
        LayerDrawable ld = (LayerDrawable) d;
        assertNotNull(ld.getDrawable(0).getColorFilter());
    }
}
