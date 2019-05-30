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

package androidx.core.database;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.CursorWindow;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CursorWindowCompatTest {
    @Test
    public void create() {
        CursorWindow cursorWindow = CursorWindowCompat.create("foo", 10000);
        cursorWindow.setNumColumns(1);
        byte[] bytes = new byte[8000];
        Arrays.fill(bytes, (byte) 1);

        if (Build.VERSION.SDK_INT >= 28) {
            // On 28, we can successfully control alloc size
            assertTrue(cursorWindow.allocRow());
            assertTrue("Allocation of 1st row should succeed", cursorWindow.putBlob(bytes, 0, 0));
            assertTrue(cursorWindow.allocRow());
            assertFalse("Allocation of 2nd row should fail", cursorWindow.putBlob(bytes, 1, 0));
        } else {
            // Before, we can't, so we'll get something much bigger
            for (int i = 0; i < 50; i++) {
                assertTrue(cursorWindow.allocRow());
                assertTrue("Allocation of row #" + (i + 1) + " should succeed",
                        cursorWindow.putBlob(bytes, i, 0));
            }
        }
    }
}
