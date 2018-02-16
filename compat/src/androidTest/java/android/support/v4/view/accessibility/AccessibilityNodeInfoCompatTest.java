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

package android.support.v4.view.accessibility;

import static org.junit.Assert.*;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.accessibility.AccessibilityNodeInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AccessibilityNodeInfoCompatTest {
    @Test
    public void testSetCollectionInfoIsNullable() throws Exception {
        AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.wrap(
                accessibilityNodeInfo);
        accessibilityNodeInfoCompat.setCollectionInfo(null);
    }

    @Test
    public void testSetCollectionItemInfoIsNullable() throws Exception {
        AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.wrap(
                accessibilityNodeInfo);
        accessibilityNodeInfoCompat.setCollectionItemInfo(null);
    }
}
