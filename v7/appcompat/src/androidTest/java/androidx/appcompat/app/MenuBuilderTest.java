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

package androidx.appcompat.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.appcompat.view.menu.MenuBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MenuBuilderTest {

    @Test
    public void setOptionalIconsVisibleMethodShouldRemainPublic() throws Exception {
        // This test is to verify workaround for bug in the ROM of Explay Fresh devices with 4.2.2 ROM.
        // Manufacturer has modified ROM and added a public method setOptionalIconsVisible
        // to android.view.Menu interface. Because of that the runtime can't load MenuBuilder class
        // because it had no such public method (it was package local)
        Method method = MenuBuilder.class
                .getMethod("setOptionalIconsVisible", boolean.class);
        assertNotNull(method);
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }
}

