/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager.constraints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.constraints.receivers.BaseConstraintsReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BaseConstraintsReceiverTest {

    private TestBaseConstraintsReceiver mReceiver;

    @Before
    public void setUp() {
        mReceiver = new TestBaseConstraintsReceiver(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        try {
            mReceiver.unregisterReceiver();
        } catch (IllegalArgumentException e) {
            // Ignore any exceptions if the receiver isn't registered.
        }
    }

    @Test
    public void testStartTracking_setsInitialState() {
        ConstraintsState constraintsState = mock(ConstraintsState.class);
        mReceiver.startTracking(constraintsState);
        assertThat(mReceiver.mSetupInitialState, is(true));
    }

    @Test
    public void testTracking_registersOnSizeEqualsOne() {
        ConstraintsState constraintsState1 = mock(ConstraintsState.class);
        mReceiver.startTracking(constraintsState1);
        assertThat(mReceiver.mRegistered, is(true));
        assertThat(mReceiver.mRegisterCount, is(1));
        ConstraintsState constraintsState2 = mock(ConstraintsState.class);
        mReceiver.startTracking(constraintsState2);
        assertThat(mReceiver.mRegistered, is(true));
        assertThat(mReceiver.mRegisterCount, is(1));
    }

    @Test
    public void testTracking_unregistersOnSizeEqualsZero() {
        ConstraintsState constraintsState1 = mock(ConstraintsState.class);
        mReceiver.startTracking(constraintsState1);
        ConstraintsState constraintsState2 = mock(ConstraintsState.class);
        mReceiver.startTracking(constraintsState2);

        mReceiver.stopTracking(constraintsState1);
        assertThat(mReceiver.mRegistered, is(true));
        assertThat(mReceiver.mUnregisterCount, is(0));
        mReceiver.stopTracking(constraintsState2);
        assertThat(mReceiver.mRegistered, is(false));
        assertThat(mReceiver.mUnregisterCount, is(1));
    }

    private static class TestBaseConstraintsReceiver extends BaseConstraintsReceiver {

        private boolean mSetupInitialState;
        private boolean mRegistered;
        private int mRegisterCount;
        private int mUnregisterCount;

        TestBaseConstraintsReceiver(Context context) {
            super(context);
        }

        @Override
        public void setUpInitialState(ConstraintsState constraintsState) {
            mSetupInitialState = true;
        }

        @Override
        public IntentFilter getIntentFilter() {
            return new IntentFilter();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Do nothing.
        }

        @Override
        public void registerReceiver() {
            super.registerReceiver();
            mRegistered = true;
            ++mRegisterCount;
        }

        @Override
        public void unregisterReceiver() {
            super.unregisterReceiver();
            mRegistered = false;
            ++mUnregisterCount;
        }
    };
}
