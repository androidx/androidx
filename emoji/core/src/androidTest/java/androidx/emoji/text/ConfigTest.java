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
package androidx.emoji.text;

import static androidx.emoji.util.Emoji.EMOJI_SINGLE_CODEPOINT;
import static androidx.emoji.util.EmojiMatcher.hasEmoji;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.emoji.util.TestString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConfigTest {

    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_throwsExceptionIfMetadataLoaderNull() {
        new TestConfigBuilder.TestConfig(null);
    }

    @Test(expected = NullPointerException.class)
    public void testInitCallback_throwsExceptionIfNull() {
        new ValidTestConfig().registerInitCallback(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUnregisterInitCallback_throwsExceptionIfNull() {
        new ValidTestConfig().unregisterInitCallback(null);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testBuild_withDefaultValues() {
        final EmojiCompat.Config config = new ValidTestConfig().setReplaceAll(true);

        final EmojiCompat emojiCompat = EmojiCompat.reset(config);

        final CharSequence processed = emojiCompat.process(new TestString(EMOJI_SINGLE_CODEPOINT)
                .toString());
        assertThat(processed, hasEmojiCount(1));
        assertThat(processed, hasEmoji(EMOJI_SINGLE_CODEPOINT));
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

    private static class ValidTestConfig extends EmojiCompat.Config {
        ValidTestConfig() {
            super(new TestConfigBuilder.TestEmojiDataLoader());
        }

        ValidTestConfig(EmojiCompat.MetadataRepoLoader loader) {
            super(loader);
        }
    }
}
