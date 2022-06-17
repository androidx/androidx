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

package androidx.test.uiautomator.testapp;

import static org.junit.Assert.assertTrue;

import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;

import org.junit.Ignore;
import org.junit.Test;

public class MultiWindowTests extends BaseTest {

    @Test
    @Ignore
    @SdkSuppress(minSdkVersion = 21)
    public void testHasBackButton() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "back")));
    }

    @Test
    @Ignore
    @SdkSuppress(minSdkVersion = 21)
    public void testHasHomeButton() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "home")));
    }

    @Test
    @Ignore
    @SdkSuppress(minSdkVersion = 21)
    public void testHasRecentsButton() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "recent_apps")));
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testHasStatusBar() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "status_bar")));
    }
}
