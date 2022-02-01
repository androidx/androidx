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
package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.support.v4.BaseInstrumentationTestCase;

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ComponentActivityTest extends BaseInstrumentationTestCase<TestComponentActivity> {
    private ComponentActivity mComponentActivity;
    private TestExtraData mTestExtraData;

    public ComponentActivityTest() {
        super(TestComponentActivity.class);
    }

    @Before
    public void setUp() {
        mComponentActivity = mActivityTestRule.getActivity();
        mTestExtraData = new TestExtraData();
        mComponentActivity.putExtraData(mTestExtraData);
    }

    @Test
    public void testGetExtraData_returnsNullForNotAdded() {
        assertNull(mComponentActivity.getExtraData(NeverAddedExtraData.class));
    }

    @Test
    public void testGetExtraData() {
        assertEquals(mTestExtraData, mComponentActivity.getExtraData(TestExtraData.class));
    }

    @Test
    public void testShouldDumpInternalState_nullArgs() {
        assertTrue(mComponentActivity.shouldDumpInternalState(null));
    }

    @Test
    public void testShouldDumpInternalState_emptyArgs() {
        assertTrue(mComponentActivity.shouldDumpInternalState(new String[0]));
    }

    @Test
    public void testShouldDumpInternalState_nonSpecialArg() {
        String[] args = { "--I-cant-believe-Actitivy-cares-about-this-arg" };

        assertTrue(mComponentActivity.shouldDumpInternalState(args));
    }

    @Test
    public void testShouldDumpInternalState_autofill() {
        shouldNotDumpSpecialArgOnVersion("--autofill", Build.VERSION_CODES.O);
    }

    @Test
    public void testShouldDumpInternalState_contentCapture() {
        shouldNotDumpSpecialArgOnVersion("--contentcapture", Build.VERSION_CODES.Q);
    }

    @Test
    public void testShouldDumpInternalState_translation() {
        shouldNotDumpSpecialArgOnVersion("--translation", Build.VERSION_CODES.S);
    }

    @Test
    public void testShouldDumpInternalState_listDumpables() {
        shouldNotDumpSpecialArgOnT("--list-dumpables");
    }

    @Test
    public void testShouldDumpInternalState_dumpDumpable() {
        shouldNotDumpSpecialArgOnT("--dump-dumpable");
    }

    private void shouldNotDumpSpecialArgOnVersion(String specialArg, int minApiVersion) {
        String[] args = { specialArg };
        int actualApiVersion = Build.VERSION.SDK_INT;

        if (actualApiVersion >= minApiVersion) {
            assertFalse(specialArg + " should be skipped on API " + actualApiVersion,
                    mComponentActivity.shouldDumpInternalState(args));
        } else {
            assertTrue(specialArg + " should be ignored on API " + actualApiVersion,
                    mComponentActivity.shouldDumpInternalState(args));
        }
    }

    private void shouldNotDumpSpecialArgOnT(String specialArg) {
        String[] args = { specialArg };
        int actualApiVersion = Build.VERSION.SDK_INT;

        if (BuildCompat.isAtLeastT()) {
            assertFalse(specialArg + " should be skipped on API " + actualApiVersion,
                    mComponentActivity.shouldDumpInternalState(args));
        } else {
            assertTrue(specialArg + " should be ignored on API " + actualApiVersion,
                    mComponentActivity.shouldDumpInternalState(args));
        }
    }

    private class NeverAddedExtraData extends ComponentActivity.ExtraData {
    }

    private class TestExtraData extends ComponentActivity.ExtraData {
    }
}
