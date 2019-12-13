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

package androidx.browser.trusted;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link Token}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TokenTest {
    /** The name of some other package that will be installed on the device. */
    private static final String OTHER_PACKAGE_NAME = "android";

    private Context mContext;
    private String mPackageName;
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageName = mContext.getPackageName();
        mPackageManager = mContext.getPackageManager();
    }

    @Test
    public void matches_basic() {
        Token token = Token.create(mPackageName, mPackageManager);
        assertTrue(token.matches(mPackageName, mPackageManager));
    }

    @Test
    public void matches_serialized() {
        Token token = Token.create(mPackageName, mPackageManager);
        byte[] serialized = token.serialize();
        assertTrue(Token.deserialize(serialized).matches(mPackageName, mPackageManager));
    }

    @Test
    public void doesntMatch_basic() {
        Token token = Token.create(OTHER_PACKAGE_NAME, mPackageManager);
        assertFalse(token.matches(mPackageName, mPackageManager));
    }

    @Test
    public void doesntMatch_serialized() {
        Token token = Token.create(OTHER_PACKAGE_NAME, mPackageManager);
        byte[] serialized = token.serialize();
        assertFalse(Token.deserialize(serialized).matches(mPackageName, mPackageManager));
    }
}
