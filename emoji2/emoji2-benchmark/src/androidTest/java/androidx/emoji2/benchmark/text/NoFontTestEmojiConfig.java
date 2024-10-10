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

package androidx.emoji2.benchmark.text;

import static org.mockito.Mockito.mock;

import android.graphics.Typeface;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;

import org.jspecify.annotations.NonNull;

public class NoFontTestEmojiConfig extends EmojiCompat.Config {

    static EmojiCompat.Config emptyConfig() {
        return new NoFontTestEmojiConfig(new EmptyEmojiDataLoader());
    }

    static EmojiCompat.Config neverLoadsConfig() {
        return new NoFontTestEmojiConfig(new NeverCompletesMetadataRepoLoader());
    }

    static EmojiCompat.Config fromLoader(EmojiCompat.MetadataRepoLoader loader) {
        return new NoFontTestEmojiConfig(loader);
    }

    private NoFontTestEmojiConfig(EmojiCompat.MetadataRepoLoader loader) {
        super(loader);
    }

    private static class EmptyEmojiDataLoader implements EmojiCompat.MetadataRepoLoader {
        @Override
        public void load(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
            loaderCallback.onLoaded(MetadataRepo.create(mock(Typeface.class)));
        }
    }

    private static class NeverCompletesMetadataRepoLoader
            implements EmojiCompat.MetadataRepoLoader {
        @Override
        public void load(final EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
            // do nothing, this will be called on the test thread and is a no-op
        }
    }
}
