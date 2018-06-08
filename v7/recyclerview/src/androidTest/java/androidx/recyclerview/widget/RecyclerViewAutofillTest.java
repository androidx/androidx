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

package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewAutofillTest  {
    static Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void initializeWithAutofillDisabled()  {
        RecyclerView recyclerView = new RecyclerView(getContext());
        int importance = ViewCompat.getImportantForAutofill(recyclerView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS, importance);
        } else {
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, importance);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testXmlValues() {
        ViewGroup parent = (ViewGroup) LayoutInflater.from(getContext())
                .inflate(R.layout.autofill_rv, null);
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_YES,
                parent.findViewById(R.id.autofill_yes).getImportantForAutofill());
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS,
                parent.findViewById(R.id.autofill_yesExcludeDescendants).getImportantForAutofill());
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO,
                parent.findViewById(R.id.autofill_no).getImportantForAutofill());

        // NOTE: RV overrides auto specifically
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,
                parent.findViewById(R.id.autofill_auto).getImportantForAutofill());
    }
}
