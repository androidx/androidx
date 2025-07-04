/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * Test that asserts that all methods on {@link WebSettingsAdapter} are overridden by
 * {@link WebSettingsNoOpAdapter}.
 */

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WebSettingsNoOpAdapterTest {

    @Test
    public void testAllMethodsOverridden()  {
        Method[] declaredMethods = WebSettingsAdapter.class.getDeclaredMethods();

        Class<WebSettingsNoOpAdapter> noOpClass = WebSettingsNoOpAdapter.class;
        for (Method method : declaredMethods) {
            try {
                noOpClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                Assert.fail("Class WebSettingsNoOpAdapter is missing override for method "
                        + method);
            }
        }
    }
}
