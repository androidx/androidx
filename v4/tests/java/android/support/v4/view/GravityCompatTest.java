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
package android.support.v4.view;

import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.Gravity;
import android.view.View;

public class GravityCompatTest extends AndroidTestCase {
    @SmallTest
    public void testConstants() {
        // Compat constants must match core constants since they can be OR'd with
        // other core constants.
        assertEquals("Start constants", GravityCompat.START, Gravity.START);
        assertEquals("End constants", GravityCompat.END, Gravity.END);
    }

    @SmallTest
    public void testGetAbsoluteGravity() {
        assertEquals("Left under LTR",
                GravityCompat.getAbsoluteGravity(Gravity.LEFT, ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.LEFT);
        assertEquals("Right under LTR",
                GravityCompat.getAbsoluteGravity(Gravity.RIGHT, ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.RIGHT);
        assertEquals("Left under RTL",
                GravityCompat.getAbsoluteGravity(Gravity.LEFT, ViewCompat.LAYOUT_DIRECTION_RTL),
                Gravity.LEFT);
        assertEquals("Right under RTL",
                GravityCompat.getAbsoluteGravity(Gravity.RIGHT, ViewCompat.LAYOUT_DIRECTION_RTL),
                Gravity.RIGHT);

        assertEquals("Start under LTR",
                GravityCompat.getAbsoluteGravity(GravityCompat.START,
                        ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.LEFT);
        assertEquals("End under LTR",
                GravityCompat.getAbsoluteGravity(GravityCompat.END,
                        ViewCompat.LAYOUT_DIRECTION_LTR),
                Gravity.RIGHT);

        if (Build.VERSION.SDK_INT >= 17) {
            // The following tests are only expected to pass on v17+ devices
            assertEquals("Start under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.START,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.RIGHT);
            assertEquals("End under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.END,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.LEFT);
        } else {
            // And on older devices START is always LEFT, END is always RIGHT
            assertEquals("Start under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.START,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.LEFT);
            assertEquals("End under RTL",
                    GravityCompat.getAbsoluteGravity(GravityCompat.END,
                            ViewCompat.LAYOUT_DIRECTION_RTL),
                    Gravity.RIGHT);
        }
    }
}
