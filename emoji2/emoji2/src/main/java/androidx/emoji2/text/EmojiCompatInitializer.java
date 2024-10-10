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

import android.content.Context;
import android.os.Handler;

import androidx.annotation.WorkerThread;
import androidx.core.os.TraceCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleInitializer;
import androidx.startup.AppInitializer;
import androidx.startup.Initializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Initializer for configuring EmojiCompat with the system installed downloadable font provider.
 *
 * <p>This initializer will initialize EmojiCompat immediately then defer loading the font for a
 * short delay to avoid delaying application startup. Typically, the font will be loaded shortly
 * after the first screen of your application loads, which means users may see system emoji
 * briefly prior to the compat font loading.</p>
 *
 * <p>This is the recommended configuration for all apps that don't need specialized configuration,
 * and don't need to control the background thread that initialization runs on. For more information
 * see {@link androidx.emoji2.text.DefaultEmojiCompatConfig}.</p>
 *
 * <p>In addition to the reasons listed in {@code DefaultEmojiCompatConfig} you may wish to disable
 * this automatic configuration if you intend to call initialization from an existing background
 * thread pool in your application.</p>
 *
 * <p>This is enabled by default by including the {@code :emoji2:emoji2} gradle artifact. To
 * disable the default configuration (and allow manual configuration) add this to your manifest:</p>
 *
 * <pre>
 *     &lt;provider
 *         android:name="androidx.startup.InitializationProvider"
 *         android:authorities="${applicationId}.androidx-startup"
 *         android:exported="false"
 *         tools:node="merge"&gt;
 *         &lt;meta-data android:name="androidx.emoji2.text.EmojiCompatInitializer"
 *                   tools:node="remove" /&gt;
 *     &lt;/provider&gt;
 * </pre>
 *
 * This initializer depends on {@link ProcessLifecycleInitializer}.
 *
 * @see androidx.emoji2.text.DefaultEmojiCompatConfig
 */
public class EmojiCompatInitializer implements Initializer<Boolean> {
    private static final long STARTUP_THREAD_CREATION_DELAY_MS = 500L;
    private static final String S_INITIALIZER_THREAD_NAME = "EmojiCompatInitializer";

    /**
     * Initialize EmojiCompat with the app's context.
     *
     * @param context application context
     * @return result of default init
     */
    @SuppressWarnings("AutoBoxing")
    @Override
    public @NonNull Boolean create(@NonNull Context context) {
        EmojiCompat.init(new BackgroundDefaultConfig(context));
        delayUntilFirstResume(context);
        return true;
    }

    /**
     * Wait until the first frame of the application to do anything.
     *
     * This allows startup code to run before the delay is scheduled.
     */
    void delayUntilFirstResume(@NonNull Context context) {
        // schedule delay after first Activity resumes
        AppInitializer appInitializer = AppInitializer.getInstance(context);
        LifecycleOwner lifecycleOwner = appInitializer
                .initializeComponent(ProcessLifecycleInitializer.class);
        Lifecycle lifecycle = lifecycleOwner.getLifecycle();
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                loadEmojiCompatAfterDelay();
                lifecycle.removeObserver(this);
            }
        });
    }

    void loadEmojiCompatAfterDelay() {
        final Handler mainHandler = ConcurrencyHelpers.mainHandlerAsync();
        mainHandler.postDelayed(new LoadEmojiCompatRunnable(), STARTUP_THREAD_CREATION_DELAY_MS);
    }

    /**
     * Dependes on ProcessLifecycleInitializer
     */
    @Override
    public @NonNull List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.singletonList(ProcessLifecycleInitializer.class);
    }

    static class LoadEmojiCompatRunnable implements Runnable {
        @Override
        public void run() {
            try {
                // this is main thread, so mark what we're doing (this trace includes thread
                // start time in BackgroundLoadingLoader.load
                TraceCompat.beginSection("EmojiCompat.EmojiCompatInitializer.run");
                if (EmojiCompat.isConfigured()) {
                    EmojiCompat.get().load();
                }
            } finally {
                TraceCompat.endSection();
            }
        }
    }

    static class BackgroundDefaultConfig extends EmojiCompat.Config {
        protected BackgroundDefaultConfig(Context context) {
            super(new BackgroundDefaultLoader(context));
            setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);
        }
    }

    static class BackgroundDefaultLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context mContext;

        BackgroundDefaultLoader(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public void load(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
            ThreadPoolExecutor executor = ConcurrencyHelpers.createBackgroundPriorityExecutor(
                            S_INITIALIZER_THREAD_NAME);
            executor.execute(() -> doLoad(loaderCallback, executor));
        }

        @WorkerThread
        void doLoad(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback,
                @NonNull ThreadPoolExecutor executor) {
            try {
                FontRequestEmojiCompatConfig config = DefaultEmojiCompatConfig.create(mContext);
                if (config == null) {
                    throw new RuntimeException("EmojiCompat font provider not available on this "
                            + "device.");
                }
                config.setLoadingExecutor(executor);
                config.getMetadataRepoLoader().load(new EmojiCompat.MetadataRepoLoaderCallback() {
                    @Override
                    public void onLoaded(@NonNull MetadataRepo metadataRepo) {
                        try {
                            // main thread is notified before returning, so we can quit now
                            loaderCallback.onLoaded(metadataRepo);
                        } finally {
                            executor.shutdown();
                        }
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        try {
                            // main thread is notified before returning, so we can quit now
                            loaderCallback.onFailed(throwable);
                        } finally {
                            executor.shutdown();
                        }
                    }
                });
            } catch (Throwable t) {
                loaderCallback.onFailed(t);
                executor.shutdown();
            }
        }
    }

}
