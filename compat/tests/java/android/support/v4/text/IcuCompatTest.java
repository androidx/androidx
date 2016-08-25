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
 * limitations under the License
 */

package android.support.v4.text;

import android.os.Build;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import java.util.Locale;

@SmallTest
public class IcuCompatTest extends TestCase {
    public void testMaximizeAndGetScript() {
        assertEquals("Latn", ICUCompat.maximizeAndGetScript(new Locale("en", "US")));

        // Script tags were added to java.util.Locale only on API 21.
        if (Build.VERSION.SDK_INT >= 21) {
            assertEquals(
                    "Visp", ICUCompat.maximizeAndGetScript(Locale.forLanguageTag("en-Visp-US")));
        }
    }
}
