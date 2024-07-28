/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.aidl;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.graphics.Point;
import android.os.Build;

import androidx.pdf.models.SelectionBoundary;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class SelectionBoundaryTest {

    @Test
    public void testAtIndex_selectionBoundaryCreatedWithCorrectValues() {
        assertThat(SelectionBoundary.atIndex(4)).isEqualTo(new SelectionBoundary(4, -1, -1, false));
    }
    @Test
    public void testAtPoint_selectionBoundaryCreatedWithCorrectValues() {
        assertThat(SelectionBoundary.atPoint(new Point(3, 4))).isEqualTo(
                new SelectionBoundary(-1, 3, 4, false));
    }

    @Test
    public void testAtPoint_pointContainsXAndY_selectionBoundaryCreatedWithCorrectValues() {
        assertThat(SelectionBoundary.atPoint(1, 2)).isEqualTo(
                new SelectionBoundary(-1, 1, 2, false));
    }
    @Test
    public void testClassFields() {
        List<String> fields = new ArrayList<>();
        fields.add("PAGE_START");
        fields.add("PAGE_END");
        fields.add("CREATOR");
        fields.add("mIndex");
        fields.add("mX");
        fields.add("mY");
        fields.add("mIsRtl");

        List<String> declaredFields = new ArrayList<>();
        for (Field field : SelectionBoundary.class.getDeclaredFields()) {
            declaredFields.add(field.getName());
        }

        assertTrue(fields.containsAll(declaredFields));
    }
}
