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

package androidx.wear.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WearApiVersionHelperTest {
    @Mock
    WearApiVersionHelper.AbstractApiVersion mMockApiVersion;

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();

    @Before
    public void setUp() {
        mMockApiVersion = mock(WearApiVersionHelper.AbstractApiVersion.class);
        WearApiVersionHelper.sTestApiVersion = mMockApiVersion;
        when(mMockApiVersion.compareTo(any())).thenCallRealMethod();
    }

    @Test
    public void test_samePlatformLevelSameIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(3);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_samePlatformLevelHigherIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(5);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_samePlatformLevelLowerIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_higherPlatformLevelLowerIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(40);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_higherPlatformLevelHigherIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(40);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelSameIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(30);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(3);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelLowerIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(30);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelHigherIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(29);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(5);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }


    @Test
    public void test_invalidVersion_exception() {
        assertThrows(IllegalArgumentException.class,
                () -> WearApiVersionHelper.isApiVersionAtLeast(
                "marmalade"));
        assertThrows(IllegalArgumentException.class,
                () -> WearApiVersionHelper.isApiVersionAtLeast(
                "33-3"));
    }
}


