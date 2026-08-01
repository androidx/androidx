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

package androidx.emoji2.text;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NoOpEmojiCompatTest {

    @Test
    @SdkSuppress(minSdkVersion = 35)
    public void testNoOp_onApi35AndAbove_whenUseAfterUpdatableSystemFontsFalse() {
        final AtomicBoolean successCalled = new AtomicBoolean(false);
        EmojiCompat.Config config =
                NoFontTestEmojiConfig.neverLoadsConfig()
                        .setUseAfterUpdatableSystemFonts(false)
                        .registerInitCallback(
                                Runnable::run,
                                new EmojiCompat.InitCallback() {
                                    @Override
                                    public void onInitialized() {
                                        successCalled.set(true);
                                    }

                                    @Override
                                    public void onFailed(Throwable throwable) {
                                        fail("Should not call onFailed");
                                    }
                                });

        EmojiCompat.reset(config);

        assertThat(successCalled.get()).isTrue();
        assertThat(EmojiCompat.get().getLoadState()).isEqualTo(EmojiCompat.LOAD_STATE_SUCCEEDED);

        CharSequence input = "hello 🐣";
        CharSequence output = EmojiCompat.get().process(input);
        assertThat(output).isSameInstanceAs(input);
    }

    @Test
    @SdkSuppress(minSdkVersion = 35)
    public void testNoOp_onApi35AndAbove_byDefault() {
        final AtomicBoolean successCalled = new AtomicBoolean(false);
        EmojiCompat.Config config =
                NoFontTestEmojiConfig.neverLoadsConfig()
                        .registerInitCallback(
                                Runnable::run,
                                new EmojiCompat.InitCallback() {
                                    @Override
                                    public void onInitialized() {
                                        successCalled.set(true);
                                    }

                                    @Override
                                    public void onFailed(Throwable throwable) {
                                        fail("Should not call onFailed");
                                    }
                                });

        EmojiCompat.reset(config);

        assertThat(successCalled.get()).isTrue();
        assertThat(EmojiCompat.get().getLoadState()).isEqualTo(EmojiCompat.LOAD_STATE_SUCCEEDED);

        CharSequence input = "hello 🐣";
        CharSequence output = EmojiCompat.get().process(input);
        assertThat(output).isSameInstanceAs(input);
    }

    @Test
    @SdkSuppress(minSdkVersion = 35)
    public void testNoNoOp_onApi35AndAbove_whenUseAfterUpdatableSystemFontsTrue() {
        final AtomicBoolean successCalled = new AtomicBoolean(false);
        EmojiCompat.Config config =
                NoFontTestEmojiConfig.neverLoadsConfig()
                        .setUseAfterUpdatableSystemFonts(true)
                        .registerInitCallback(
                                Runnable::run,
                                new EmojiCompat.InitCallback() {
                                    @Override
                                    public void onInitialized() {
                                        successCalled.set(true);
                                    }

                                    @Override
                                    public void onFailed(Throwable throwable) {
                                        fail("Should not call onFailed");
                                    }
                                });

        EmojiCompat.reset(config);

        assertThat(successCalled.get()).isFalse();
        assertThat(EmojiCompat.get().getLoadState()).isEqualTo(EmojiCompat.LOAD_STATE_LOADING);

        try {
            EmojiCompat.get().process("hello 🐣");
            fail("Expected IllegalStateException since load is pending");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34)
    public void testNoNoOp_onApi34AndBelow_whenUseAfterUpdatableSystemFontsFalse() {
        final AtomicBoolean successCalled = new AtomicBoolean(false);
        EmojiCompat.Config config =
                NoFontTestEmojiConfig.neverLoadsConfig()
                        .setUseAfterUpdatableSystemFonts(false)
                        .registerInitCallback(
                                Runnable::run,
                                new EmojiCompat.InitCallback() {
                                    @Override
                                    public void onInitialized() {
                                        successCalled.set(true);
                                    }

                                    @Override
                                    public void onFailed(Throwable throwable) {
                                        fail("Should not call onFailed");
                                    }
                                });

        EmojiCompat.reset(config);

        assertThat(successCalled.get()).isFalse();
        assertThat(EmojiCompat.get().getLoadState()).isEqualTo(EmojiCompat.LOAD_STATE_LOADING);
    }
}
