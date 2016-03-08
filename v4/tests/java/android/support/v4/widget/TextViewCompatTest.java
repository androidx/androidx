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


package android.support.v4.widget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.support.test.InstrumentationRegistry;
import android.support.v4.widget.TextViewCompat;
import android.util.Log;
import android.widget.TextView;

import android.support.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class TextViewCompatTest extends ActivityInstrumentationTestCase2<TestActivity> {
    private boolean mDebug;

    Throwable mainThreadException;

    Thread mInstrumentationThread;

    public TextViewCompatTest() {
        super("android.support.v4.widget", TestActivity.class);
        mDebug = false;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mInstrumentationThread = Thread.currentThread();

        // Note that injectInstrumentation was added in v5. Since this is v4 we have to use
        // the misspelled (and deprecated) inject API.
        injectInsrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        getInstrumentation().waitForIdleSync();
        super.tearDown();
    }

    @Test
    public void testMaxLines() throws Throwable {
        final TextView textView = new TextView(getActivity());
        textView.setMaxLines(4);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(textView);
            }
        });

        assertEquals("Max lines must match", TextViewCompat.getMaxLines(textView), 4);
    }
}
