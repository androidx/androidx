/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.telephony;

import static android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
import static android.telephony.SubscriptionManager.getDefaultSubscriptionId;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build.VERSION;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Test for {@link androidx.core.telephony.TelephonyManagerCompat}.
 */
@SmallTest
public class TelephonyManagerCompatTest {

    private Context mContext;
    private TelephonyManager mTelephonyManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTelephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Test
    public void testGetSubscriptionId() {
        SubscriptionManager subscriptionManager = (SubscriptionManager) mContext.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        int defaultSubId = TelephonyManagerCompat.getSubscriptionId(mTelephonyManager);
        assertSubIdsEqual(DEFAULT_SUBSCRIPTION_ID, defaultSubId);

        if (VERSION.SDK_INT >= 24) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(DEFAULT_SUBSCRIPTION_ID);
            assertSubIdsEqual(DEFAULT_SUBSCRIPTION_ID,
                    TelephonyManagerCompat.getSubscriptionId(mTelephonyManager));

            List<SubscriptionInfo> subInfos = subscriptionManager.getActiveSubscriptionInfoList();
            if (subInfos != null) {
                for (SubscriptionInfo subInfo : subInfos) {
                    mTelephonyManager =
                            mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId());
                    assertSubIdsEqual(subInfo.getSubscriptionId(),
                            TelephonyManagerCompat.getSubscriptionId(mTelephonyManager));
                }
            }
        }
    }

    // from 29+ the api requires privileged permissions
    @SdkSuppress(maxSdkVersion = 28)
    @Test
    public void testGetImei() {
        // check this doesn't crash at least
        TelephonyManagerCompat.getImei(mTelephonyManager);

        if (VERSION.SDK_INT >= 24) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(DEFAULT_SUBSCRIPTION_ID);
            TelephonyManagerCompat.getImei(mTelephonyManager);

            SubscriptionManager subscriptionManager =
                    mContext.getSystemService(SubscriptionManager.class);
            List<SubscriptionInfo> subInfos = subscriptionManager.getActiveSubscriptionInfoList();
            if (subInfos != null) {
                for (SubscriptionInfo subInfo : subInfos) {
                    mTelephonyManager =
                            mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId());
                    TelephonyManagerCompat.getImei(mTelephonyManager);
                }
            }
        }
    }

    private void assertSubIdsEqual(int expected, int actual) {
        try {
            assertEquals(expected, actual);
        } catch (AssertionError e) {
            if (expected == DEFAULT_SUBSCRIPTION_ID) {
                assertEquals(getDefaultSubscriptionId(), actual);
            } else if (actual == DEFAULT_SUBSCRIPTION_ID) {
                assertEquals(expected, getDefaultSubscriptionId());
            } else {
                throw e;
            }
        }
    }
}
