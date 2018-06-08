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

package androidx.core.content;

import static org.junit.Assert.assertEquals;

import android.Manifest;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link PermissionChecker}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PermissionCheckerTest {
    private Context mContext;

    @Before
    public void setup() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testCheckPermission() throws Exception {
        assertEquals(PermissionChecker.PERMISSION_DENIED, PermissionChecker.checkSelfPermission(
                mContext, Manifest.permission.ANSWER_PHONE_CALLS));
        assertEquals(PermissionChecker.PERMISSION_GRANTED, PermissionChecker.checkSelfPermission(
                mContext, Manifest.permission.VIBRATE));
    }
}
