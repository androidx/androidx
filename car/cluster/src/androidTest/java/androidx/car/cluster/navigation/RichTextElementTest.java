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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link RichText} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RichTextElementTest {
    private static final String TEST_TEXT = "foo";
    private static final ImageReference TEST_IMAGE = ImageReferenceTest.createSampleImage();

    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        RichTextElement element = createSampleElement();

        assertEquals(element, createSampleElement());
        assertNotEquals(element, new RichTextElement.Builder()
                .setImage(TEST_IMAGE)
                .setText("")
                .build());
        assertNotEquals(element, new RichTextElement.Builder()
                .setImage(null)
                .setText(TEST_TEXT)
                .build());

        assertEquals(element.hashCode(), createSampleElement().hashCode());
    }

    /**
     * Test that an empty string will be returned from {@link RichTextElement#getText()} to the
     * consumer, even if no string was received.
     */
    @Test
    public void text_isEmptyIfNotProvided() {
        assertEquals("", new RichTextElement().getText());
    }

    /**
     * Returns a sample {@link RichTextElement} for testing.
     */
    public RichTextElement createSampleElement() {
        return new RichTextElement.Builder()
                .setImage(TEST_IMAGE)
                .setText(TEST_TEXT)
                .build();
    }
}
