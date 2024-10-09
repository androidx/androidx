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

import androidx.appcompat.test.R;
import androidx.fragment.app.DialogFragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppCompatDialogFragmentTest {
    @SuppressWarnings("deprecation")
    @Rule
    public final androidx.test.rule.ActivityTestRule<WindowDecorAppCompatActivity> mTestRule =
            new androidx.test.rule.ActivityTestRule<>(WindowDecorAppCompatActivity.class);

    private DialogFragment mFragment;

    @Test
    public void testDialogFragmentShows() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> mFragment = new TestDialogFragment()
        );
        mFragment.show(mTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", mFragment.getDialog());
        assertTrue("Dialog was not being shown", mFragment.getDialog().isShowing());

        // And make sure we dismiss the dialog
        mFragment.dismissAllowingStateLoss();
    }

    @Test
    public void testDialogFragmentWithLayout() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> mFragment = new AppCompatDialogFragment(R.layout.dialog_layout)
        );
        mFragment.show(mTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog is not null", mFragment.getDialog());
        assertTrue("Dialog is showing", mFragment.getDialog().isShowing());
        assertNotNull("Dialog is using custom layout",
                mFragment.getDialog().findViewById(R.id.dialog_content));

        // And make sure we dismiss the dialog
        mFragment.dismissAllowingStateLoss();
    }

    public static class TestDialogFragment extends AppCompatDialogFragment {
        @Override
        public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(requireContext())
                    .setTitle("Test")
                    .setMessage("Message")
                    .setPositiveButton("Button", null)
                    .create();
        }
    }
}

