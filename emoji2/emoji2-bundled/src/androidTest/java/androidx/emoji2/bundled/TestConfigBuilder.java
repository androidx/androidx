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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;
import androidx.test.core.app.ApplicationProvider;

import java.util.concurrent.CountDownLatch;

public class TestConfigBuilder {
    private static final String FONT_FILE = "NotoColorEmojiCompat.ttf";

    private TestConfigBuilder() { }

    public static EmojiCompat.Config config() {
        return new TestConfig().setReplaceAll(true);
    }

    /**
     * Forces the creation of Metadata instead of relying on cached metadata. If GlyphChecker is
     * mocked, a new metadata has to be used instead of the statically cached metadata since the
     * result of GlyphChecker on the same device might effect other tests.
     */
    public static EmojiCompat.Config freshConfig() {
        return new TestConfig(new ResettingTestDataLoader()).setReplaceAll(true);
    }

    public static class TestConfig extends EmojiCompat.Config {
        TestConfig() {
            super(new TestEmojiDataLoader());
        }

        TestConfig(@NonNull final EmojiCompat.MetadataRepoLoader metadataLoader) {
            super(metadataLoader);
        }
    }

    public static class WaitingDataLoader implements EmojiCompat.MetadataRepoLoader {
        private final CountDownLatch mLoaderLatch;
        private final CountDownLatch mTestLatch;
        private final boolean mSuccess;

        public WaitingDataLoader(boolean success) {
            mLoaderLatch = new CountDownLatch(1);
            mTestLatch = new CountDownLatch(1);
            mSuccess = success;
        }

        public WaitingDataLoader() {
            this(true);
        }

        public CountDownLatch getLoaderLatch() {
            return mLoaderLatch;
        }

        public CountDownLatch getTestLatch() {
            return mTestLatch;
        }

        @Override
        public void load(@NonNull final EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mLoaderLatch.await();
                        if (mSuccess) {
                            loaderCallback.onLoaded(MetadataRepo.create(mock(Typeface.class)));
                        } else {
                            loaderCallback.onFailed(null);
                        }

                        mTestLatch.countDown();
                    } catch (Throwable e) {
                        fail();
                    }
                }
            }).start();
        }
    }

    public static class TestEmojiDataLoader implements EmojiCompat.MetadataRepoLoader {
        static final Object S_METADATA_REPO_LOCK = new Object();
        // keep a static instance to in order not to slow down the tests
        @GuardedBy("sMetadataRepoLock")
        static volatile MetadataRepo sMetadataRepo;

        TestEmojiDataLoader() {
        }

        @Override
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            if (sMetadataRepo == null) {
                synchronized (S_METADATA_REPO_LOCK) {
                    if (sMetadataRepo == null) {
                        try {
                            final Context context = ApplicationProvider.getApplicationContext();
                            final AssetManager assetManager = context.getAssets();
                            sMetadataRepo = MetadataRepo.create(assetManager, FONT_FILE);
                        } catch (Throwable e) {
                            loaderCallback.onFailed(e);
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            loaderCallback.onLoaded(sMetadataRepo);
        }
    }

    public static class ResettingTestDataLoader implements EmojiCompat.MetadataRepoLoader {
        private MetadataRepo mMetadataRepo;

        ResettingTestDataLoader() {
        }

        @Override
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            if (mMetadataRepo == null) {
                try {
                    final Context context = ApplicationProvider.getApplicationContext();
                    final AssetManager assetManager = context.getAssets();
                    mMetadataRepo = MetadataRepo.create(assetManager, FONT_FILE);
                } catch (Throwable e) {
                    loaderCallback.onFailed(e);
                    throw new RuntimeException(e);
                }
            }

            loaderCallback.onLoaded(mMetadataRepo);
        }
    }

}
