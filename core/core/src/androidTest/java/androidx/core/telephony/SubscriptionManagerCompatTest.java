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
import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Test for {@link androidx.core.telephony.SubscriptionManagerCompat}.
 */
@SmallTest
public class SubscriptionManagerCompatTest {

    private SubscriptionManager mSubscriptionManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @Test
    public void testGetSlotIndex() {
        assertEquals(INVALID_SIM_SLOT_INDEX,
                SubscriptionManagerCompat.getSlotIndex(INVALID_SUBSCRIPTION_ID));

        // check this doesn't crash at least
        SubscriptionManagerCompat.getSlotIndex(DEFAULT_SUBSCRIPTION_ID);

        List<SubscriptionInfo> subInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfos != null) {
            for (SubscriptionInfo subInfo : subInfos) {
                assertEquals(subInfo.getSimSlotIndex(),
                        SubscriptionManagerCompat.getSlotIndex(subInfo.getSubscriptionId()));
            }
        }
    }
}
