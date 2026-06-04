/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.graphics.Typeface;

import androidx.compose.remote.player.core.platform.FontInstance;
import androidx.compose.remote.player.core.platform.TypefaceResolver;
import androidx.test.filters.SdkSuppress;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@SdkSuppress(minSdkVersion = 28)
public class TypefaceResolverTest {

    @Test
    public void testRemappingTypefaceResolver() {
        TypefaceResolver next =
                new TypefaceResolver() {
                    @NonNull
                    @Override
                    public FontInstance resolve(
                            int fontType,
                            int weight,
                            boolean italic,
                            Typeface fallbackTypeface,
                            int fallbackWeight,
                            boolean fallbackItalic) {
                        return null;
                    }

                    @NonNull
                    @Override
                    public FontInstance resolve(
                            @NonNull String fontName,
                            int weight,
                            boolean italic,
                            Typeface fallbackTypeface,
                            int fallbackWeight,
                            boolean fallbackItalic) {
                        if (fontName.equals("mapped-name")) {
                            return new SimpleFontInstance(Typeface.SERIF);
                        }
                        return null;
                    }
                };

        RemappingTypefaceResolver resolver = new RemappingTypefaceResolver(next);
        resolver.remapType(0, "mapped-name");

        FontInstance instance = resolver.resolve(0, 400, false, null, 400, false);
        assertNotNull(instance);
        assertEquals(Typeface.SERIF, instance.getTypeface());
    }

    @Test
    public void testFallbackCreateTypefaceResolver() {
        FallbackCreateTypefaceResolver resolver = new FallbackCreateTypefaceResolver();

        FontInstance instance = resolver.resolve("sans-serif", 400, false, null, 400, false);
        assertNotNull(instance);
        assertFalse(instance.getTypeface().isBold());
        assertFalse(instance.getTypeface().isItalic());
    }
}
