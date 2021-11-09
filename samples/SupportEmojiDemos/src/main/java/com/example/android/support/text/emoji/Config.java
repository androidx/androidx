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

package com.example.android.support.text.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.FontRequest;
import androidx.emoji2.bundled.BundledEmojiCompatConfig;
import androidx.emoji2.text.DefaultEmojiCompatConfig;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.FontRequestEmojiCompatConfig;

import java.util.HashSet;
import java.util.Set;

class Config {
    private static final String TAG = "EmojiDemo";

    public static final String PREF_NAME = "emojicompat";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_REPLACE_ALL = "replaceAll";
    public static final String KEY_INDICATOR = "indicator";
    private static Config sInstance;

    private SharedPreferences mSharedPref;
    private Context mContext;
    private Source mSource;
    private boolean mReplaceAll;
    private boolean mIndicator;

    private final Set<Listener> mListeners = new HashSet<>();

    private Config() {
    }

    public enum Source {
        DEFAULT(0), BUNDLED(1), DOWNLOADABLE(2), DISABLED(3);

        private final int mValue;

        Source(int value) {
            mValue = value;
        }

        @NonNull
        public static Source forPosition(int value) {
            for (Source source : Source.values()) {
                if (source.getPosition() == value) {
                    return source;
                }
            }
            return Source.DEFAULT;
        }

        public int getPosition() {
            return mValue;
        }

        public boolean isEnabled() {
            return this != DISABLED;
        }
    }

    void init(Context context) {
        this.mContext = context;
        mSharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mReplaceAll = mSharedPref.getBoolean(KEY_REPLACE_ALL, false);
        int sourcePos = mSharedPref.getInt(KEY_SOURCE, Source.DEFAULT.getPosition());
        mSource = Source.forPosition(sourcePos);
        mIndicator = mSharedPref.getBoolean(KEY_INDICATOR, false);
        resetEmojiCompat();
    }

    static synchronized Config get() {
        if (sInstance == null) {
            sInstance = new Config();
        }
        return sInstance;
    }

    void registerListener(Listener listener) {
        mListeners.add(listener);
    }

    void unregisterListener(Listener listener) {
        mListeners.remove(listener);
    }

    void update(@NonNull Source compatSource, boolean replaceAll, boolean indicator) {
        mSource = compatSource;
        mReplaceAll = replaceAll;
        mIndicator = indicator;
        mSharedPref.edit().putInt(KEY_SOURCE, mSource.getPosition()).apply();
        mSharedPref.edit().putBoolean(KEY_REPLACE_ALL, mReplaceAll).apply();
        mSharedPref.edit().putBoolean(KEY_INDICATOR, mIndicator).apply();
        resetEmojiCompat();
        for (Listener listener : mListeners) {
            listener.onEmojiCompatUpdated();
        }
    }

    private void resetEmojiCompat() {
        final EmojiCompat.Config config;
        switch (mSource) {
            case DEFAULT: {
                EmojiCompat.Config defaultConfig = DefaultEmojiCompatConfig.create(mContext);
                if (defaultConfig != null) {
                    config = defaultConfig;
                } else {
                    // don't let it be null for simplicity
                    config = failingConfig();
                }
                break;
            }
            case DOWNLOADABLE: {
                final FontRequest fontRequest = new FontRequest(
                        mContext.getString(R.string.provider_authority),
                        mContext.getString(R.string.provider_package),
                        mContext.getString(R.string.font_query),
                        R.array.com_google_android_gms_fonts_certs);

                config = new FontRequestEmojiCompatConfig(mContext, fontRequest);
                break;
            }
            case BUNDLED: {
                config = new BundledEmojiCompatConfig(mContext);
                break;
            }
            case DISABLED: {
                config = failingConfig();
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected source");
            }
        }

        config.setReplaceAll(mReplaceAll)
                .setEmojiSpanIndicatorEnabled(mIndicator)
                .registerInitCallback(new EmojiCompat.InitCallback() {
                    @Override
                    public void onInitialized() {
                        Log.i(TAG, "EmojiCompat initialized");
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        Log.e(TAG, "EmojiCompat initialization failed", throwable);
                    }
                });

        EmojiCompat.reset(config);
    }

    private EmojiCompat.Config failingConfig() {
        return new EmojiCompat.Config(new EmojiCompat.MetadataRepoLoader() {
            @Override
            public void load(
                    @NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
                loaderCallback.onFailed(new RuntimeException("Disable"));
            }
        }) {
        };
    }

    boolean isCompatEnabled() {
        return mSource.isEnabled();
    }

    boolean isReplaceAll() {
        return isCompatEnabled() && mReplaceAll;
    }

    boolean isDownloadable() {
        return isCompatEnabled() && mSource == Source.DOWNLOADABLE;
    }

    @NonNull
    Source getSource() {
        return mSource;
    }

    boolean isIndicator() {
        return isCompatEnabled() && mIndicator;
    }

    interface Listener {
        void onEmojiCompatUpdated();
    }
}
