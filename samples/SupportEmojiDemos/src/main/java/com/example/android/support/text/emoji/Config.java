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
import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;

import java.util.HashSet;
import java.util.Set;

class Config {
    private static final String TAG = "EmojiDemo";

    public static final String PREF_NAME = "emojicompat";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_REPLACE_ALL = "replaceAll";
    public static final String KEY_DOWNLOADABLE = "downloadable";
    public static final String KEY_INDICATOR = "indicator";
    private static Config sInstance;

    private SharedPreferences mSharedPref;
    private Context mContext;
    private boolean mCompatEnabled;
    private boolean mReplaceAll;
    private boolean mDownloadable;
    private boolean mIndicator;

    private Set<Listener> mListeners = new HashSet<>();

    private Config() {
    }

    void init(Context context) {
        this.mContext = context;
        mSharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mCompatEnabled = mSharedPref.getBoolean(KEY_ENABLED, false);
        mReplaceAll = mSharedPref.getBoolean(KEY_REPLACE_ALL, false);
        mDownloadable = mSharedPref.getBoolean(KEY_DOWNLOADABLE, false);
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

    void update(boolean compatEnabled, boolean replaceAll, boolean downloadable,
            boolean indicator) {
        mCompatEnabled = compatEnabled;
        mReplaceAll = replaceAll;
        mDownloadable = downloadable;
        mIndicator = indicator;
        mSharedPref.edit().putBoolean(KEY_ENABLED, mCompatEnabled).apply();
        mSharedPref.edit().putBoolean(KEY_REPLACE_ALL, mReplaceAll).apply();
        mSharedPref.edit().putBoolean(KEY_DOWNLOADABLE, mDownloadable).apply();
        mSharedPref.edit().putBoolean(KEY_INDICATOR, mIndicator).apply();
        resetEmojiCompat();
        for (Listener listener : mListeners) {
            listener.onEmojiCompatUpdated();
        }
    }

    private void resetEmojiCompat() {
        final EmojiCompat.Config config;
        if (mCompatEnabled) {
            if (mDownloadable) {
                final FontRequest fontRequest = new FontRequest(
                        mContext.getString(R.string.provider_authority),
                        mContext.getString(R.string.provider_package),
                        mContext.getString(R.string.font_query),
                        R.array.com_google_android_gms_fonts_certs);

                config = new FontRequestEmojiCompatConfig(mContext, fontRequest);
            } else {
                config = new BundledEmojiCompatConfig(mContext);
            }
        } else {
            config = new EmojiCompat.Config(new EmojiCompat.MetadataRepoLoader() {
                @Override
                public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
                    loaderCallback.onFailed(new RuntimeException("Disable"));
                }
            }) {
            };
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

    boolean isCompatEnabled() {
        return mCompatEnabled;
    }

    boolean isReplaceAll() {
        return mCompatEnabled && mReplaceAll;
    }

    boolean isDownloadable() {
        return mCompatEnabled && mDownloadable;
    }

    boolean isIndicator() {
        return mCompatEnabled && mIndicator;
    }

    interface Listener {
        void onEmojiCompatUpdated();
    }
}
