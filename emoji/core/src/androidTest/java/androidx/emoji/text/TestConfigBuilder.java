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

import static org.junit.Assert.fail;

import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;

public class TestConfigBuilder {
    public static EmojiCompat.Config config() {
        return new TestConfig().setReplaceAll(true);
    }

    public static class TestConfig extends EmojiCompat.Config {
        TestConfig() {
            super(new TestEmojiDataLoader());
        }

        TestConfig(final EmojiCompat.MetadataRepoLoader metadataLoader) {
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
                            loaderCallback.onLoaded(new MetadataRepo());
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
        static final Object sMetadataRepoLock = new Object();
        // keep a static instance to in order not to slow down the tests
        @GuardedBy("sMetadataRepoLock")
        static volatile MetadataRepo sMetadataRepo;

        TestEmojiDataLoader() {
        }

        @Override
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            if (sMetadataRepo == null) {
                synchronized (sMetadataRepoLock) {
                    if (sMetadataRepo == null) {
                        try {
                            final AssetManager assetManager =
                                    InstrumentationRegistry.getContext().getAssets();
                            sMetadataRepo = MetadataRepo.create(assetManager,
                                    "NotoColorEmojiCompat.ttf");
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

}
