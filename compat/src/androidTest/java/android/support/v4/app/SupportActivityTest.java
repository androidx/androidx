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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SupportActivityTest extends BaseInstrumentationTestCase<TestSupportActivity> {
    private SupportActivity mSupportActivity;
    private TestExtraData mTestExtraData;

    public SupportActivityTest() {
        super(TestSupportActivity.class);
    }

    @Before
    public void setUp() {
        mSupportActivity = mActivityTestRule.getActivity();
        mTestExtraData = new TestExtraData();
        mSupportActivity.putExtraData(mTestExtraData);
    }

    @Test
    public void testGetExtraData_returnsNullForNotAdded() {
        assertNull(mSupportActivity.getExtraData(NeverAddedExtraData.class));
    }

    @Test
    public void testGetExtraData() {
        assertEquals(mTestExtraData, mSupportActivity.getExtraData(TestExtraData.class));
    }

    public class NeverAddedExtraData extends SupportActivity.ExtraData {
    }

    public class TestExtraData extends SupportActivity.ExtraData {
    }
}

