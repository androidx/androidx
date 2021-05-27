/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.camera.extensions.impl.ExtensionVersionImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExtensionVersionTest {

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field field = VersionName.class.getDeclaredField("CURRENT");
        field.setAccessible(true);
        field.set(null, new VersionName("1.1.0"));
    }

    @Test
    public void testVendorReturnValidVersion() throws NoSuchFieldException, IllegalAccessException {
        String testString = "1.0.1";
        setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        Assert.assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnGreaterMajor() throws NoSuchFieldException, IllegalAccessException {
        String testString = "2.0.0";
        setTestApiVersion(testString);

        assertFalse(ExtensionVersion.isExtensionVersionSupported());
        assertNull(ExtensionVersion.getRuntimeVersion());
    }

    @Test
    public void testVendorReturnGreaterMinor() throws NoSuchFieldException, IllegalAccessException {
        String testString = "1.2.0";
        setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnLesserMinor() throws NoSuchFieldException, IllegalAccessException {
        String testString = "1.0.0";
        setTestApiVersion(testString);

        assertTrue(ExtensionVersion.isExtensionVersionSupported());
        assertEquals(ExtensionVersion.getRuntimeVersion(), Version.parse(testString));
    }

    @Test
    public void testVendorReturnInvalid() throws NoSuchFieldException, IllegalAccessException {
        String testString = "1.0.1.0";
        setTestApiVersion(testString);

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

    private void setTestApiVersion(String testString) throws NoSuchFieldException,
            IllegalAccessException {
        ExtensionVersionImpl mockExtensionVersionImpl = mock(ExtensionVersionImpl.class);
        when(mockExtensionVersionImpl.checkApiVersion(anyString())).thenReturn(testString);

        Class<?> vendorExtenderVersioningClass = null;

        for (Class<?> clazz : ExtensionVersion.class.getDeclaredClasses()) {
            if (clazz.getSimpleName().equals("VendorExtenderVersioning")) {
                vendorExtenderVersioningClass = clazz;
                break;
            }
        }

        Field field = vendorExtenderVersioningClass.getDeclaredField("sImpl");
        field.setAccessible(true);
        field.set(null, mockExtensionVersionImpl);
    }
}
