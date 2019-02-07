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

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.test.EmptyFragmentTestActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
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

        assertWithMessage("Dialog was null")
                .that(fragment.getDialog())
                .isNotNull();
        assertWithMessage("Dialog was not being shown")
                .that(fragment.getDialog().isShowing())
                .isTrue();
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

        assertWithMessage("Dialog was null")
                .that(fragment.getDialog())
                .isNotNull();
        assertWithMessage("Dialog was not being shown")
                .that(fragment.getDialog().isShowing())
                .isTrue();

        final boolean[] dialogIsNonNull = new boolean[1];
        final boolean[] isShowing = new boolean[1];
        final int[] onDismissCalledCount = new int[1];
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                    public void onStop() {
                        Dialog dialog = fragment.getDialog();
                        dialogIsNonNull[0] = dialog != null;
                        isShowing[0] = dialog != null && dialog.isShowing();
                        countDownLatch.countDown();
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void onDestroy() {
                        onDismissCalledCount[0] = fragment.onDismissCalledCount;
                        countDownLatch.countDown();
                    }
                });
            }
        });

        mActivityTestRule.finishActivity();

        countDownLatch.await(1, TimeUnit.SECONDS);
        assertWithMessage("onDismiss() should be called before onDestroy()")
                .that(onDismissCalledCount[0])
                .isEqualTo(1);
        assertWithMessage("Dialog should not be null in onStop()")
                .that(dialogIsNonNull[0])
                .isTrue();
        assertWithMessage("Dialog should still be showing in onStop() "
                + "during the normal lifecycle")
                .that(isShowing[0])
                .isTrue();
    }

    @Test
    public void testDialogFragmentDismiss() throws Throwable {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        final TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertWithMessage("Dialog was null")
                .that(fragment.getDialog())
                .isNotNull();
        assertWithMessage("Dialog was not being shown")
                .that(fragment.getDialog().isShowing())
                .isTrue();

        final boolean[] dialogIsNonNull = new boolean[1];
        final boolean[] isShowing = new boolean[1];
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                    public void onStop() {
                        Dialog dialog = fragment.getDialog();
                        dialogIsNonNull[0] = dialog != null;
                        isShowing[0] = dialog != null && dialog.isShowing();
                        countDownLatch.countDown();
                    }
                });
            }
        });

        final CountDownLatch dismissLatch = new CountDownLatch(1);
        fragment.setDestroyViewCallback(new TestDialogFragment.OnDestroyViewCallback() {
            @Override
            public void onDestroyView(TestDialogFragment fragment) {
                dismissLatch.countDown();
            }
        });

        fragment.dismiss();

        countDownLatch.await(1, TimeUnit.SECONDS);

        assertWithMessage("Dialog should not be null in onStop()")
                .that(dialogIsNonNull[0])
                .isTrue();
        assertWithMessage("Dialog should not be showing in onStop() "
                + "when manually dismissed")
                .that(isShowing[0])
                .isFalse();

        // Wait for the DialogFragment's onDestroyView to be called which is where the Dialog
        // gets null'ed out
        dismissLatch.await();

        assertWithMessage("Dialog should be null after dismiss()")
                .that(fragment.getDialog())
                .isNull();
    }

    @Test
    public void testDialogFragmentDismissBeforeOnDestroy() throws Throwable {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        final TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        final int[] onDismissCalledCount = new int[1];
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void onDestroy() {
                        onDismissCalledCount[0] = fragment.onDismissCalledCount;
                        countDownLatch.countDown();
                    }
                });
            }
        });

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.dismiss();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);

        assertWithMessage("onDismiss() should be called only once before onDestroy()")
                .that(onDismissCalledCount[0])
                .isEqualTo(1);
    }

    public static class TestDialogFragment extends DialogFragment {

        public interface OnDestroyViewCallback {
            void onDestroyView(TestDialogFragment fragment);
        }

        public int onDismissCalledCount = 0;

        private @Nullable OnDestroyViewCallback mDestroyViewCallback = null;

        public void setDestroyViewCallback(OnDestroyViewCallback callback) {
            mDestroyViewCallback = callback;
        }

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
        public void onDismiss(@NonNull DialogInterface dialog) {
            super.onDismiss(dialog);
            onDismissCalledCount++;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (mDestroyViewCallback != null) {
                mDestroyViewCallback.onDestroyView(this);
            }
        }
    }
}

