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

package androidx.appcompat.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DialogTestCase {
    @Rule
    public final ActivityTestRule<WindowDecorAppCompatActivity> mActivityTestRule =
            new ActivityTestRule<>(WindowDecorAppCompatActivity.class);

    @Test
    public void testDialogFragmentShows() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    public static class TestDialogFragment extends AppCompatDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setTitle("Test")
                    .setMessage("Message")
                    .setPositiveButton("Button", null)
                    .create();
        }
    }
}

