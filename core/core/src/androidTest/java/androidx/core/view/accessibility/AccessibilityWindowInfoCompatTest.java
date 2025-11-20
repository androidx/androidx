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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Region;
import android.os.Build;
import android.os.LocaleList;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.core.os.LocaleListCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AccessibilityWindowInfoCompatTest {

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

        assertThat(infoCompat.unwrap()).isNotNull();
        assertThat(infoCompat.unwrap()).isEqualTo(info);
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testUnwrap() {
        AccessibilityWindowInfo accessibilityWindowInfo = AccessibilityWindowInfo.obtain();
        AccessibilityWindowInfoCompat accessibilityWindowInfoCompat =
                AccessibilityWindowInfoCompat.wrapNonNullInstance(accessibilityWindowInfo);
        assertThat(accessibilityWindowInfoCompat.unwrap()).isEqualTo(accessibilityWindowInfo);
    }

    @SdkSuppress(minSdkVersion = 26)
    @SmallTest
    @Test
    public void testIsPictureInPictureMode() {
        AccessibilityWindowInfoCompat windowInfoCompat = obtainedWrappedWindowCompat();
        assertThat(windowInfoCompat.isInPictureInPictureMode()).isEqualTo(
                windowInfoCompat.unwrap().isInPictureInPictureMode());
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testGetDisplayId() {
        AccessibilityWindowInfoCompat windowInfoCompat = obtainedWrappedWindowCompat();
        assertThat(windowInfoCompat.getDisplayId()).isEqualTo(
                windowInfoCompat.unwrap().getDisplayId());
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
        assertThat(region).isEqualTo(compatRegion);
    }
    @SmallTest
    @Test
    public void testGetTransitionTimeMillis() {
        boolean supportsPlatformTransitionMillis = Build.VERSION.SDK_INT >= 34;
        AccessibilityWindowInfoCompat windowInfoCompat;
        if (supportsPlatformTransitionMillis) {
            AccessibilityWindowInfo mockInfo = mock(AccessibilityWindowInfo.class);
            when(mockInfo.getTransitionTimeMillis()).thenReturn(100L);
            windowInfoCompat =
                    AccessibilityWindowInfoCompat.wrapNonNullInstance(mockInfo);
        } else {
            windowInfoCompat = obtainedWrappedWindowCompat();
        }

        long transitionMillis = supportsPlatformTransitionMillis ? 100L : 0;
        assertThat(windowInfoCompat.getTransitionTimeMillis()).isEqualTo(transitionMillis);
    }

    @SmallTest
    @Test
    public void testGetLocales() {
        boolean supportsPlatformLocales = Build.VERSION.SDK_INT >= 34;
        AccessibilityWindowInfoCompat windowInfoCompat;
        if (supportsPlatformLocales) {
            AccessibilityWindowInfo mockInfo = mock(AccessibilityWindowInfo.class);
            when(mockInfo.getLocales()).thenReturn(new LocaleList(Locale.ENGLISH));
            windowInfoCompat = AccessibilityWindowInfoCompat.wrapNonNullInstance(mockInfo);
        } else {
            windowInfoCompat = obtainedWrappedWindowCompat();
        }

        LocaleListCompat localeListCompat = supportsPlatformLocales
                ? LocaleListCompat.wrap(new LocaleList(Locale.ENGLISH))
                : LocaleListCompat.getEmptyLocaleList();
        assertThat(windowInfoCompat.getLocales()).isEqualTo(localeListCompat);
    }

    @SdkSuppress(minSdkVersion = 33)
    @SmallTest
    @Test
    public void testGetRoot_withPrefetchingStrategy_returnsRoot() {
        AccessibilityWindowInfo accessibilityWindowInfo = mock(AccessibilityWindowInfo.class);
        AccessibilityWindowInfoCompat windowCompat =
                AccessibilityWindowInfoCompat.wrapNonNullInstance(accessibilityWindowInfo);
        windowCompat.getRoot(AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS);
        verify(accessibilityWindowInfo).getRoot(
                AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS);
    }
}
