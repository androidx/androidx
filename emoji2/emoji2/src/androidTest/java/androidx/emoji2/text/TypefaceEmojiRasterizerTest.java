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

package androidx.emoji2.text;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class TypefaceEmojiRasterizerTest extends TestCase {

    @Test
    public void defaultUnknown_hasGlyph() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        assertFalse(subject.isPreferredSystemRender());
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_UNKNOWN, subject.getHasGlyph());
    }

    @Test
    public void set_hasGlyphTrue_saved() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        subject.setHasGlyph(true);
        assertFalse(subject.isPreferredSystemRender());
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_EXISTS, subject.getHasGlyph());
    }

    @Test
    public void set_hasGlyphFalse_saved() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        subject.setHasGlyph(false);
        assertFalse(subject.isPreferredSystemRender());
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_ABSENT, subject.getHasGlyph());
    }

    @Test
    public void set_hasGlyph_doesNotResetExclusion() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        subject.setExclusion(true);
        subject.setHasGlyph(true);
        assertTrue(subject.isPreferredSystemRender());
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_EXISTS, subject.getHasGlyph());
    }

    @Test
    public void setExclusion_doesNotResetHasGlyph() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        subject.setHasGlyph(true);
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_EXISTS, subject.getHasGlyph());
        subject.setExclusion(true);
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_EXISTS, subject.getHasGlyph());
    }

    @Test
    public void setExclusion_doesNotSetHasGlyph() {
        TestTypefaceEmojiRasterizer subject = new TestTypefaceEmojiRasterizer(new int[] { 42 });
        subject.setExclusion(true);
        assertEquals(TypefaceEmojiRasterizer.HAS_GLYPH_UNKNOWN, subject.getHasGlyph());
        assertTrue(subject.isPreferredSystemRender());
    }

}
