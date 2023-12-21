/*
 * Copyright 2022 The Android Open Source Project
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import android.annotation.TargetApi;
import android.graphics.Region;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AccessibilityWindowInfoCompatTest {

    @TargetApi(21)
    private AccessibilityWindowInfoCompat obtainedWrappedWindowCompat() {
        AccessibilityWindowInfo accessibilityWindowInfo = AccessibilityWindowInfo.obtain();
        return AccessibilityWindowInfoCompat.wrapNonNullInstance(accessibilityWindowInfo);
    }

    @SdkSuppress(minSdkVersion = 30)
    @SmallTest
    @Test
    public void testConstructor() {
        AccessibilityWindowInfoCompat infoCompat = new AccessibilityWindowInfoCompat();
        AccessibilityWindowInfo info = new AccessibilityWindowInfo();

        assertThat(infoCompat.unwrap(), is(not(equalTo(null))));
        assertThat(infoCompat.unwrap(), equalTo(info));
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testUnwrap() {
        AccessibilityWindowInfo accessibilityWindowInfo = AccessibilityWindowInfo.obtain();
        AccessibilityWindowInfoCompat accessibilityWindowInfoCompat =
                AccessibilityWindowInfoCompat.wrapNonNullInstance(accessibilityWindowInfo);
        assertThat(accessibilityWindowInfoCompat.unwrap(), equalTo(accessibilityWindowInfo));
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testIsPictureInPictureMode() {
        AccessibilityWindowInfoCompat windowInfoCompat = obtainedWrappedWindowCompat();
        assertThat(windowInfoCompat.isInPictureInPictureMode(), equalTo(
                windowInfoCompat.unwrap().isInPictureInPictureMode()));
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testGetDisplayId() {
        AccessibilityWindowInfoCompat windowInfoCompat = obtainedWrappedWindowCompat();
        assertThat(windowInfoCompat.getDisplayId(), equalTo(
                windowInfoCompat.unwrap().getDisplayId()));
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testGetRegionInScreen() {
        AccessibilityWindowInfoCompat windowInfoCompat = obtainedWrappedWindowCompat();
        Region region = new Region();
        Region compatRegion = new Region();
        windowInfoCompat.unwrap().getRegionInScreen(region);
        windowInfoCompat.getRegionInScreen(region);
        assertThat(region, equalTo(compatRegion));
    }
}
