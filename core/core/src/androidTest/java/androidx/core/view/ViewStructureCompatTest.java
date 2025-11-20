/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import android.support.v4.BaseInstrumentationTestCase;
import android.view.ViewStructure;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewStructureCompatTest extends
        BaseInstrumentationTestCase<ViewCompatActivity> {

    public ViewStructureCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Test
    public void testToViewStructure_returnsViewStructure() {
        ViewStructure viewStructure = mock(ViewStructure.class);
        ViewStructureCompat compat = ViewStructureCompat.toViewStructureCompat(viewStructure);

        ViewStructure result = compat.toViewStructure();

        assertEquals(viewStructure, result);
    }

    @Test
    public void testSetText() {
        ViewStructure viewStructure = mock(ViewStructure.class);
        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        doNothing().when(viewStructure).setText(captor.capture());

        ViewStructureCompat compat = ViewStructureCompat.toViewStructureCompat(viewStructure);
        compat.setText("foo");

        assertEquals("foo", captor.getValue());
    }

    @Test
    public void testSetClassName() {
        ViewStructure viewStructure = mock(ViewStructure.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        doNothing().when(viewStructure).setClassName(captor.capture());

        ViewStructureCompat compat = ViewStructureCompat.toViewStructureCompat(viewStructure);
        compat.setClassName("foo");

        assertEquals("foo", captor.getValue());
    }

    @Test
    public void testSetContentDescription() {
        ViewStructure viewStructure = mock(ViewStructure.class);
        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        doNothing().when(viewStructure).setContentDescription(captor.capture());

        ViewStructureCompat compat = ViewStructureCompat.toViewStructureCompat(viewStructure);
        compat.setContentDescription("foo");

        assertEquals("foo", captor.getValue());
    }

    @Test
    public void testSetDimens() {
        ViewStructure viewStructure = mock(ViewStructure.class);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(viewStructure).setDimens(
                captor.capture(),
                captor.capture(),
                captor.capture(),
                captor.capture(),
                captor.capture(),
                captor.capture()
        );

        ViewStructureCompat compat = ViewStructureCompat.toViewStructureCompat(viewStructure);
        compat.setDimens(1, 2, 3, 4, 5, 6);

        List<Integer> expected = new ArrayList<>();
        expected.add(1);
        expected.add(2);
        expected.add(3);
        expected.add(4);
        expected.add(5);
        expected.add(6);
        List<Integer> results = captor.getAllValues();
        assertEquals(expected.size(), results.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(expected.get(i), results.get(i));
        }
    }
}

