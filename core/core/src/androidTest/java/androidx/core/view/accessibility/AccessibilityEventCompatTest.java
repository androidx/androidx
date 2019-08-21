/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.view.accessibility;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import android.view.accessibility.AccessibilityEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = 28)
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AccessibilityEventCompatTest {

    @Test
    public void testObtain() {
        final int scrollX = 20;
        AccessibilityEvent event = AccessibilityEvent.obtain();
        event.setScrollDeltaX(scrollX);
        AccessibilityEvent newEvent = AccessibilityEventCompat.obtain(event);
        assertThat(newEvent.getScrollDeltaX(), equalTo(scrollX));
    }
}
