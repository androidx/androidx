/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view.autofill;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.autofill.AutofillId;

import androidx.core.view.ViewCompatActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 26)
public class AutofillIdCompatTest extends
        BaseInstrumentationTestCase<ViewCompatActivity> {

    private View mView;

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mView = activity.findViewById(androidx.core.test.R.id.view);
    }

    public AutofillIdCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Test
    public void testToAutofillId_returnAutofillId() {
        AutofillId autofillId = mView.getAutofillId();
        AutofillIdCompat autofillIdCompat =
                AutofillIdCompat.toAutofillIdCompat(autofillId);

        AutofillId result = autofillIdCompat.toAutofillId();

        assertEquals(autofillId, result);
    }
}
