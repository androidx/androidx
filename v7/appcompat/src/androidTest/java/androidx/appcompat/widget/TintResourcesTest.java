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

package androidx.appcompat.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TintResourcesTest {
    @Rule
    public final ActivityTestRule<AppCompatActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatActivity.class);

    @Test
    public void testTintResourcesDelegateBackToOriginalResources() {
        final TestResources testResources =
                new TestResources(mActivityTestRule.getActivity().getResources());
        // First make sure that the flag is false
        assertFalse(testResources.wasGetDrawableCalled());

        // Now wrap in a TintResources instance and get a Drawable
        final Resources tintResources =
                new TintResources(mActivityTestRule.getActivity(), testResources);
        tintResources.getDrawable(android.R.drawable.ic_delete);

        // ...and assert that the flag was flipped
        assertTrue(testResources.wasGetDrawableCalled());
    }

    /**
     * Special Resources class which returns a known Drawable instance from a special ID
     */
    private static class TestResources extends Resources {
        private boolean mGetDrawableCalled;

        public TestResources(Resources res) {
            super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
        }

        @Override
        public Drawable getDrawable(int id) throws NotFoundException {
            mGetDrawableCalled = true;
            return super.getDrawable(id);
        }

        public boolean wasGetDrawableCalled() {
            return mGetDrawableCalled;
        }
    }

}
