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

package androidx.car.cluster.navigation;

import static androidx.car.cluster.navigation.utils.Assertions.assertImmutable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Tests for {@link RichText} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RichTextTest {
    private static final String TEST_TEXT = "foo";
    private static final String TEST_TEXT_ELEMENT = "bar";

    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        RichText expected = createSampleRichText();
        RichTextElement element = new RichTextElement.Builder().setText(TEST_TEXT_ELEMENT).build();

        assertEquals(expected, createSampleRichText());
        assertNotEquals(expected, new RichText.Builder().build(TEST_TEXT));
        assertNotEquals(expected, new RichText.Builder()
                .addElement(element)
                .addElement(element)
                .build(TEST_TEXT));

        assertEquals(expected.hashCode(), createSampleRichText().hashCode());
    }

    /**
     * Tests that lists returned by {@link RichText} are immutable.
     */
    @Test
    public void immutability() {
        assertImmutable(new RichText.Builder().build(TEST_TEXT).getElements());
        assertImmutable(new RichText().getElements());
    }

    /**
     * Tests that even if we receive a null list of {@link RichTextElement}s, we return an empty
     * list to the consumers.
     */
    @Test
    public void nullability_elementsListIsNeverNull() {
        assertEquals(new ArrayList<>(), new RichText().getElements());
    }

    /**
     * Test that a text representation must not be null.
     */
    @Test(expected = NullPointerException.class)
    public void builder_textIsMandatory() {
        new RichText.Builder().build(null);
    }

    /**
     * Builder doesn't accept null elements
     */
    @Test(expected = NullPointerException.class)
    public void builder_elementsCantBeNull() {
        new RichText.Builder().addElement(null);
    }

    /**
     * Returns a sample {@link RichText} instance for testing.
     */
    public static RichText createSampleRichText() {
        return new RichText.Builder()
                .addElement(new RichTextElement.Builder().setText(TEST_TEXT_ELEMENT).build())
                .build(TEST_TEXT);
    }
}
