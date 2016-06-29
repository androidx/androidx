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
package android.support.v4.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.app.test.FragmentResultActivity;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.fragment.test.R;
import android.test.suitebuilder.annotation.SmallTest;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for Fragment startActivityForResult and startIntentSenderForResult.
 */
@RunWith(AndroidJUnit4.class)
public class FragmentReceiveResultTest extends BaseInstrumentationTestCase<FragmentTestActivity> {
    private FragmentTestActivity mActivity;
    private TestFragment mFragment;

    public FragmentReceiveResultTest() {
        super(FragmentTestActivity.class);
    }

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mFragment = attachTestFragment();
    }

    @Test
    @SmallTest
    public void testStartActivityForResultOk() {
        startActivityForResult(10, Activity.RESULT_OK, "content 10");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(10, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_OK, mFragment.mResultCode);
        assertEquals("content 10", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartActivityForResultCanceled() {
        startActivityForResult(20, Activity.RESULT_CANCELED, "content 20");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(20, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_CANCELED, mFragment.mResultCode);
        assertEquals("content 20", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartIntentSenderForResultOk() {
        startIntentSenderForResult(30, Activity.RESULT_OK, "content 30");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(30, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_OK, mFragment.mResultCode);
        assertEquals("content 30", mFragment.mResultContent);
    }

    @Test
    @SmallTest
    public void testStartIntentSenderForResultCanceled() {
        startIntentSenderForResult(40, Activity.RESULT_CANCELED, "content 40");

        assertTrue("Fragment should receive result", mFragment.mHasResult);
        assertEquals(40, mFragment.mRequestCode);
        assertEquals(Activity.RESULT_CANCELED, mFragment.mResultCode);
        assertEquals("content 40", mFragment.mResultContent);
    }

    private TestFragment attachTestFragment() {
        final TestFragment fragment = new TestFragment();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
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
            final String content) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mActivity, FragmentResultActivity.class);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode);
                intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content);

                mFragment.startActivityForResult(intent, requestCode);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void startIntentSenderForResult(final int requestCode, final int resultCode,
            final String content) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
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
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    public static class TestFragment extends Fragment {
        boolean mHasResult = false;
        int mRequestCode = -1;
        int mResultCode = 100;
        String mResultContent;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mHasResult = true;
            mRequestCode = requestCode;
            mResultCode = resultCode;
            mResultContent = data.getStringExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT);
        }
    }
}
