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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.emoji2.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class InitCallbackTest {

    @Test
    public void testRegisterInitCallback_callsSuccessCallback() {
        final EmojiCompat.InitCallback initCallback1 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.InitCallback initCallback2 = mock(EmojiCompat.InitCallback.class);

        final EmojiCompat.Config config = TestConfigBuilder.config();
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        emojiCompat.registerInitCallback(initCallback1);
        emojiCompat.registerInitCallback(initCallback2);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback1, times(1)).onInitialized();
        verify(initCallback2, times(1)).onInitialized();
    }

    @Test
    public void testRegisterInitCallback_callsFailCallback() {
        final EmojiCompat.InitCallback initCallback1 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.InitCallback initCallback2 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.MetadataRepoLoader loader = mock(EmojiCompat.MetadataRepoLoader.class);
        doThrow(new RuntimeException("")).when(loader)
                .load(any(EmojiCompat.MetadataRepoLoaderCallback.class));

        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        emojiCompat.registerInitCallback(initCallback1);
        emojiCompat.registerInitCallback(initCallback2);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback1, times(1)).onFailed(nullable(Throwable.class));
        verify(initCallback2, times(1)).onFailed(nullable(Throwable.class));
    }

    @Test
    public void testRegisterInitCallback_callsFailCallback_whenOnFailCalledByLoader() {
        final EmojiCompat.InitCallback initCallback = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.MetadataRepoLoader loader = new EmojiCompat.MetadataRepoLoader() {
            @Override
            public void load(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
                loaderCallback.onFailed(new RuntimeException(""));
            }
        };

        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        emojiCompat.registerInitCallback(initCallback);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback, times(1)).onFailed(nullable(Throwable.class));
    }

    @Test
    public void testRegisterInitCallback_callsFailCallback_whenMetadataRepoIsNull() {
        final EmojiCompat.InitCallback initCallback = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.MetadataRepoLoader loader = new EmojiCompat.MetadataRepoLoader() {
            @Override
            public void load(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
                loaderCallback.onLoaded(null);
            }
        };

        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        emojiCompat.registerInitCallback(initCallback);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback, times(1)).onFailed(nullable(Throwable.class));
    }

    @Test
    public void testUnregisterInitCallback_doesNotInteractWithCallback()
            throws InterruptedException {
        // will be registered
        final EmojiCompat.InitCallback callback = mock(EmojiCompat.InitCallback.class);
        // will be registered, and then unregistered before metadata load is complete
        final EmojiCompat.InitCallback callbackUnregister = mock(EmojiCompat.InitCallback.class);
        // will be registered to config
        final EmojiCompat.InitCallback callbackConfigUnregister = mock(
                EmojiCompat.InitCallback.class);
        // will be registered to config and then unregistered
        final EmojiCompat.InitCallback callbackConfig = mock(EmojiCompat.InitCallback.class);

        //make sure that loader does not load before unregister
        final TestConfigBuilder.WaitingDataLoader
                metadataLoader = new TestConfigBuilder.WaitingDataLoader(false/*fail*/);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(metadataLoader)
                .registerInitCallback(callbackConfig)
                .registerInitCallback(callbackConfigUnregister)
                .unregisterInitCallback(callbackConfigUnregister);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        // register before metadata is loaded
        emojiCompat.registerInitCallback(callbackUnregister);
        emojiCompat.registerInitCallback(callback);

        // unregister before metadata is loaded
        emojiCompat.unregisterInitCallback(callbackUnregister);

        // fire metadata loaded event
        metadataLoader.getLoaderLatch().countDown();
        metadataLoader.getTestLatch().await();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(callbackUnregister, times(0)).onFailed(any(Throwable.class));
        verify(callbackConfigUnregister, times(0)).onFailed(nullable(Throwable.class));
        verify(callback, times(1)).onFailed(nullable(Throwable.class));
        verify(callbackConfig, times(1)).onFailed(nullable(Throwable.class));
    }

    @Test
    public void testInitCallback_addedToConfigAndInstance_callsSuccess() {
        final EmojiCompat.InitCallback initCallback1 = mock(EmojiCompat.InitCallback.class);
        final EmojiCompat.InitCallback initCallback2 = mock(EmojiCompat.InitCallback.class);

        final EmojiCompat.Config config = TestConfigBuilder.config()
                .registerInitCallback(initCallback1);
        final EmojiCompat emojiCompat = EmojiCompat.reset(config);
        emojiCompat.registerInitCallback(initCallback2);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(initCallback1, times(1)).onInitialized();
        verify(initCallback2, times(1)).onInitialized();
    }

    @Test
    public void testInitCallback_dispatchesOnExecutor() {
        boolean[] didRun = new boolean[] { false };
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable r) {
                didRun[0] = true;
            }
        };
        EmojiCompat.Config config = TestConfigBuilder.config();
        config.registerInitCallback(executor, mock(EmojiCompat.InitCallback.class));
        EmojiCompat.reset(config);

        assertEquals(didRun[0], true);
    }

}
