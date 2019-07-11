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

package androidx.camera.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.extensions.impl.ExtensionVersionImpl;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(
        sdk = Build.VERSION_CODES.LOLLIPOP,
        manifest = Config.NONE,
        shadows = {
                ExtensionVersionTest.ShadowExtenderVersioningImpl.class,
                ExtensionVersionTest.ShadowBuildConfig.class})
public class ExtensionVersionTest {

    @Test
    public void testVendorReturnValidVersion() {
        String testString = "1.0.1";
        ShadowExtenderVersioningImpl.setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnGreaterMajor() {
        String testString = "2.0.0";
        ShadowExtenderVersioningImpl.setTestApiVersion(testString);

        assertFalse(ExtensionVersion.isExtensionVersionSupported());
        assertNull(ExtensionVersion.getRuntimeVersion());
    }

    @Test
    public void testVendorReturnGreaterMinor() {
        String testString = "1.2.0";
        ShadowExtenderVersioningImpl.setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnLesserMinor() {
        String testString = "1.0.0";
        ShadowExtenderVersioningImpl.setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnInvalid() {
        String testString = "1.0.1.0";
        ShadowExtenderVersioningImpl.setTestApiVersion(testString);

        assertFalse(ExtensionVersion.isExtensionVersionSupported());
        assertNull(ExtensionVersion.getRuntimeVersion());
    }

    @After
    public void clear() {
        resetSingleton(ExtensionVersion.class, "sExtensionVersion");
    }

    private void resetSingleton(Class clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * A Shadow of {@link ExtensionVersionImpl} which return a test version for testing.
     */
    @Implements(
            value = ExtensionVersionImpl.class,
            minSdk = 21
    )
    static class ShadowExtenderVersioningImpl {

        private static String sTestVersionString;

        public static void setTestApiVersion(String s) {
            sTestVersionString = s;
        }

        @NonNull
        @Implementation
        public String checkApiVersion(String s) {
            return sTestVersionString;
        }
    }

    /**
     * A Shadow of {@link BuildConfig} which include a fake CAMERA_VERSION for testing.
     */
    @Implements(
            value = BuildConfig.class,
            minSdk = 21
    )
    static final class ShadowBuildConfig {

        @RealObject
        public static final String CAMERA_VERSION = "1.1.0-alpha02";
    }
}
