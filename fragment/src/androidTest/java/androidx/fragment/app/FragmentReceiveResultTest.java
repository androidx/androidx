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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.test.FragmentResultActivity;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for Fragment startActivityForResult and startIntentSenderForResult.
 */
@RunWith(AndroidJUnit4.class)
public class FragmentReceiveResultTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private FragmentTestActivity mActivity;
    private TestFragment mFragment;


    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mFragment = attachTestFragment();
    }

    @Test
    @SmallTest
    public void testStartActivityForResultOk() throws Throwable {
        startActivityForResult(10, Activity.RESULT_OK, "content 10");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(10, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_OK, mFragment.mResultCode);
        assertEquals("content 10", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartActivityForResultCanceled() throws Throwable {
        startActivityForResult(20, Activity.RESULT_CANCELED, "content 20");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(20, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_CANCELED, mFragment.mResultCode);
        assertEquals("content 20", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartIntentSenderForResultOk() throws Throwable {
        startIntentSenderForResult(30, Activity.RESULT_OK, "content 30");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(30, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_OK, mFragment.mResultCode);
        assertEquals("content 30", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartIntentSenderForResultCanceled() throws Throwable {
        startIntentSenderForResult(40, Activity.RESULT_CANCELED, "content 40");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(40, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_CANCELED, mFragment.mResultCode);
        assertEquals("content 40", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testActivityResult_withDelegate() {
        ActivityCompat.PermissionCompatDelegate
                delegate = mock(ActivityCompat.PermissionCompatDelegate.class);

        Intent data = new Intent();
        ActivityCompat.setPermissionCompatDelegate(delegate);

        mActivityRule.getActivity().onActivityResult(42, 43, data);

        verify(delegate).onActivityResult(same(mActivityRule.getActivity()), eq(42), eq(43),
                same(data));

        ActivityCompat.setPermissionCompatDelegate(null);
        mActivityRule.getActivity().onActivityResult(42, 43, data);
        verifyNoMoreInteractions(delegate);
    }

    private TestFragment attachTestFragment() throws Throwable {
        final TestFragment fragment = new TestFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.content, fragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
                mActivity.getFragmentManager().executePendingTransactions();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return fragment;
    }

    private void startActivityForResult(final int requestCode, final int resultCode,
            final String content) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mActivity, FragmentResultActivity.class);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content);

                mFragment.startActivityForResult(intent, requestCode);
            }
        });
        assertTrue(mFragment.mResultReceiveLatch.await(1, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void startIntentSenderForResult(final int requestCode, final int resultCode,
            final String content) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mActivity, FragmentResultActivity.class);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content);

                PendingIntent pendingIntent = PendingIntent.getActivity(mActivity,
                        requestCode, intent, 0);

                try {
                    mFragment.startIntentSenderForResult(pendingIntent.getIntentSender(),
                            requestCode, null, 0, 0, 0, null);
                } catch (IntentSender.SendIntentException e) {
                    fail("IntentSender failed");
                }
            }
        });
        assertTrue(mFragment.mResultReceiveLatch.await(1, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    public static class TestFragment extends Fragment {
        boolean mHasResult = false;
        int mRequestCode = -1;
        int mResultCode = 100;
        String mResultContent;
        final CountDownLatch mResultReceiveLatch = new CountDownLatch(1);

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mHasResult = true;
            mRequestCode = requestCode;
            mResultCode = resultCode;
            mResultContent = data.getStringExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT);
            mResultReceiveLatch.countDown();
        }
    }
}
