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

package androidx.fragment.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.fragment.app.test.EmptyFragmentTestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DialogFragmentTest {
    @Rule
    public final ActivityTestRule<EmptyFragmentTestActivity> mActivityTestRule =
            new ActivityTestRule<>(EmptyFragmentTestActivity.class);

    @Test
    public void testDialogFragmentShows() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowsNow() throws Throwable {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        final TestDialogFragment fragment = new TestDialogFragment();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.showNow(mActivityTestRule.getActivity().getSupportFragmentManager(),
                        null);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentDismiss() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());

        fragment.dismiss();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertNull("Dialog should be null after dismiss()", fragment.getDialog());
    }

    public static class TestDialogFragment extends DialogFragment {
        private boolean mManualDismiss;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setTitle("Test")
                    .setMessage("Message")
                    .setPositiveButton("Button", null)
                    .create();
        }

        @Override
        public void dismiss() {
            super.dismiss();
            mManualDismiss = true;
        }

        @Override
        public void onStop() {
            super.onStop();
            assertNotNull(getDialog());
            if (mManualDismiss) {
                assertFalse("Dialog should not be showing in onStop() "
                        + "when manually dismissed", getDialog().isShowing());
            } else {
                assertTrue("Dialog should still be showing in onStop() "
                        + "during the normal lifecycle", getDialog().isShowing());
            }
        }
    }
}

