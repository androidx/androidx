/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.res.AssetManager;

import androidx.core.util.Preconditions;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * {@link EmojiCompat.Config} implementation that loads the metadata using AssetManager and
 * bundled resources.
 * <p/>
 * <pre><code>EmojiCompat.init(new BundledEmojiCompatConfig(context));</code></pre>
 *
 * Including the emoji2-bundled artifact disables the
 * {@link androidx.emoji2.text.EmojiCompatInitializer}. You must manually call EmojiCompat.init
 * when using the bundled configuration.
 *
 * @see EmojiCompat
 */
public class BundledEmojiCompatConfig extends EmojiCompat.Config {

    /**
     * Font will be loaded on a new temporary Thread.
     *
     * @param context Context instance
     * @deprecated please call BundledEmojiCompatConfig(context, executor) to control the
     * font loading thread. This constructor will spin up a new temporary thread.
     */
    @Deprecated
    public BundledEmojiCompatConfig(@NonNull Context context) {
        super(new BundledMetadataLoader(context, null));
    }

    /**
     * Controls the executor font is loaded on.
     *
     * @param context Context instance
     * @param fontLoadExecutor Executor to load font on
     */
    public BundledEmojiCompatConfig(@NonNull Context context, @NonNull Executor fontLoadExecutor) {
        super(new BundledMetadataLoader(context, fontLoadExecutor));
    }

    private static class BundledMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private final @NonNull Context mContext;

        private final @Nullable Executor mExecutor;

        BundledMetadataLoader(@NonNull Context context, @Nullable Executor executor) {
            mContext = context.getApplicationContext();
            mExecutor = executor;
        }

        @Override
        public void load(EmojiCompat.@NonNull MetadataRepoLoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "loaderCallback cannot be null");
            final InitRunnable runnable = new InitRunnable(mContext, loaderCallback);
            if (mExecutor != null) {
                mExecutor.execute(runnable);
            } else {
                final Thread thread = new Thread(runnable);
                thread.setDaemon(false);
                thread.start();
            }
        }
    }

    private static class InitRunnable implements Runnable {
        private static final String FONT_NAME = "NotoColorEmojiCompat.ttf";
        private final EmojiCompat.MetadataRepoLoaderCallback mLoaderCallback;
        private final Context mContext;

        InitRunnable(Context context, EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            mContext = context;
            mLoaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            try {
                final AssetManager assetManager = mContext.getAssets();
                final MetadataRepo resourceIndex = MetadataRepo.create(assetManager, FONT_NAME);
                mLoaderCallback.onLoaded(resourceIndex);
            } catch (Throwable t) {
                mLoaderCallback.onFailed(t);
            }
        }
    }
}
