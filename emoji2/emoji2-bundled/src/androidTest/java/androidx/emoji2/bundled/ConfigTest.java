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
package androidx.emoji2.bundled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;

import androidx.emoji2.bundled.util.Emoji;
import androidx.emoji2.bundled.util.EmojiMatcher;
import androidx.emoji2.bundled.util.TestString;
import androidx.emoji2.text.EmojiCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ConfigTest {

    Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_throwsExceptionIfMetadataLoaderNull() {
        //noinspection ConstantConditions
        new TestConfigBuilder.TestConfig(null);
    }

    @Test(expected = NullPointerException.class)
    public void testInitCallback_throwsExceptionIfNull() {
        //noinspection ConstantConditions
        new ValidTestConfig().registerInitCallback(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUnregisterInitCallback_throwsExceptionIfNull() {
        //noinspection ConstantConditions
        new ValidTestConfig().unregisterInitCallback(null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testBuild_withDefaultValues() {
        final EmojiCompat.Config config = new ValidTestConfig().setReplaceAll(true);

        final EmojiCompat emojiCompat = EmojiCompat.reset(config);

        final CharSequence processed = emojiCompat.process(new TestString(
                Emoji.EMOJI_SINGLE_CODEPOINT)
                .toString());
        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed, EmojiMatcher.hasEmoji(Emoji.EMOJI_SINGLE_CODEPOINT));
    }

    @Test
    public void testInitCallback_callsSuccessCallback() {
        final EmojiCompat.InitCallback initCallback1 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.InitCallback initCallback2 = mock(EmojiCompat.InitCallback.class);

        final EmojiCompat.Config config = new ValidTestConfig().registerInitCallback(initCallback1)
                .registerInitCallback(initCallback2);
        EmojiCompat.reset(config);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback1, times(1)).onInitialized();
        verify(initCallback2, times(1)).onInitialized();
    }

    @Test
    @SdkSuppress(minSdkVersion = 19) //Fail callback never called for pre 19
    public void testInitCallback_callsFailCallback() {
        final EmojiCompat.InitCallback initCallback1 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.InitCallback initCallback2 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.MetadataRepoLoader loader = mock(EmojiCompat.MetadataRepoLoader.class);
        doThrow(new RuntimeException("")).when(loader)
                .load(any(EmojiCompat.MetadataRepoLoaderCallback.class));

        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader)
                .registerInitCallback(initCallback1)
                .registerInitCallback(initCallback2);
        EmojiCompat.reset(config);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback1, times(1)).onFailed(any(Throwable.class));
        verify(initCallback2, times(1)).onFailed(any(Throwable.class));
    }

    @Test
    public void testBuild_withEmojiSpanIndicator() {
        EmojiCompat.Config config = new ValidTestConfig();
        EmojiCompat emojiCompat = EmojiCompat.reset(config);

        assertFalse(emojiCompat.isEmojiSpanIndicatorEnabled());

        config = new ValidTestConfig().setEmojiSpanIndicatorEnabled(true);
        emojiCompat = EmojiCompat.reset(config);

        assertTrue(emojiCompat.isEmojiSpanIndicatorEnabled());
    }

    @Test
    public void testBuild_withEmojiSpanIndicatorColor() {
        EmojiCompat.Config config = new ValidTestConfig();
        EmojiCompat emojiCompat = EmojiCompat.reset(config);

        assertEquals(Color.GREEN, emojiCompat.getEmojiSpanIndicatorColor());

        config = new ValidTestConfig().setEmojiSpanIndicatorColor(Color.RED);
        emojiCompat = EmojiCompat.reset(config);

        assertEquals(Color.RED, emojiCompat.getEmojiSpanIndicatorColor());
    }

    @Test
    public void testBuild_defaultEmojiSpanIndicatorColor() {
        final EmojiCompat.Config config = new ValidTestConfig().setEmojiSpanIndicatorEnabled(true);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);

        assertTrue(emojiCompat.isEmojiSpanIndicatorEnabled());
    }

    @Test
    public void testBuild_manualLoadStrategy_doesNotCallMetadataLoaderLoad() {
        final EmojiCompat.MetadataRepoLoader loader = mock(EmojiCompat.MetadataRepoLoader.class);
        final EmojiCompat.Config config = new ValidTestConfig(loader)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);

        EmojiCompat.reset(config);

        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_DEFAULT, EmojiCompat.get().getLoadState());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testGlyphCheckerInstance_EmojiSpan_isNotAdded_whenHasGlyph_returnsTrue() {
        final EmojiCompat.GlyphChecker glyphChecker = mock(EmojiCompat.GlyphChecker.class);
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(true);

        final EmojiCompat.Config config = TestConfigBuilder.freshConfig()
                .setReplaceAll(false)
                .setGlyphChecker(glyphChecker);
        EmojiCompat.reset(config);

        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();
        CharSequence processed = EmojiCompat.get().process(original, 0, original.length());

        verify(glyphChecker, times(1))
                .hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt());
        assertThat(processed, Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testGlyphCheckerInstance_EmojiSpan_isAdded_whenHasGlyph_returnsFalse() {
        final EmojiCompat.GlyphChecker glyphChecker = mock(EmojiCompat.GlyphChecker.class);
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(false);

        final EmojiCompat.Config config = TestConfigBuilder.freshConfig()
                .setReplaceAll(false)
                .setGlyphChecker(glyphChecker);
        EmojiCompat.reset(config);

        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();

        CharSequence processed = EmojiCompat.get().process(original, 0, original.length());

        verify(glyphChecker, times(1))
                .hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt());
        assertThat(processed, EmojiMatcher.hasEmoji());
    }

    private static class ValidTestConfig extends EmojiCompat.Config {
        ValidTestConfig() {
            super(new TestConfigBuilder.TestEmojiDataLoader());
        }

        ValidTestConfig(EmojiCompat.MetadataRepoLoader loader) {
            super(loader);
        }
    }
}
