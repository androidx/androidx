/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.junit.Assert.assertSame;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;

import androidx.core.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DialogCompatTest extends BaseInstrumentationTestCase<TestActivity> {

    private Context mContext;

    public DialogCompatTest() {
        super(TestActivity.class);
    }

    @Before
    public void setup() {
        mContext = mActivityTestRule.getActivity();
    }

    @Test
    @SmallTest
    public void testRequireViewByIdFound() throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = new Dialog(mContext);
                dialog.setContentView(androidx.core.R.layout.custom_dialog);
                View button = dialog.findViewById(androidx.core.R.id.dialog_button);
                assertSame(button, DialogCompat.requireViewById(dialog,
                        androidx.core.R.id.dialog_button));
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testRequireViewByIdMissing() throws Throwable {
        // container isn't present inside dialog.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = new Dialog(mContext);
                dialog.setContentView(androidx.core.R.layout.custom_dialog);
                DialogCompat.requireViewById(dialog, R.id.container);
            }
        });

    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testRequireViewByIdInvalid() throws Throwable {
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = new Dialog(mContext);
                dialog.setContentView(androidx.core.R.layout.custom_dialog);
                DialogCompat.requireViewById(dialog, View.NO_ID);
            }
        });
    }
}
