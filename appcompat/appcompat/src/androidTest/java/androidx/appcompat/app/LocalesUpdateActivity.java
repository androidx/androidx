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

package androidx.appcompat.app;

import static androidx.appcompat.app.LocaleOverlayHelper.combineLocalesIfOverlayExists;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.core.os.LocaleListCompat;

import java.util.concurrent.Semaphore;

public class LocalesUpdateActivity extends BaseTestActivity {


    public static final String KEY_TITLE = "title";

    private final Semaphore mOnConfigurationChangeSemaphore = new Semaphore(0);
    private final Semaphore mOnDestroySemaphore = new Semaphore(0);
    private final Semaphore mOnCreateSemaphore = new Semaphore(0);

    private LocaleListCompat mLastLocales = LocaleListCompat.getEmptyLocaleList();

    private Configuration mEffectiveConfiguration;
    private Configuration mLastConfigurationChange;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.activity_locales;
    }

    @Override
    public void onLocalesChanged(@NonNull LocaleListCompat locales) {
        mLastLocales = locales;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mLastConfigurationChange = new Configuration(newConfig);
        mEffectiveConfiguration = mLastConfigurationChange;
        mOnConfigurationChangeSemaphore.release();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String title = getIntent().getStringExtra(KEY_TITLE);
        if (title != null) {
            setTitle(title);
        }

        mEffectiveConfiguration = new Configuration(getResources().getConfiguration());
        mOnCreateSemaphore.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mOnDestroySemaphore.release();
    }

    @Nullable
    Configuration getLastConfigurationChangeAndClear() {
        final Configuration config = mLastConfigurationChange;
        mLastConfigurationChange = null;
        return config;
    }

    /**
     * @return a copy of the {@link Configuration} from the most recent call to {@link #onCreate} or
     * {@link #onConfigurationChanged}, or {@code null} if neither has been called yet
     */
    @Nullable
    Configuration getEffectiveConfiguration() {
        return mEffectiveConfiguration;
    }

    LocaleListCompat getLastLocalesAndReset() {
        final LocaleListCompat locales = mLastLocales;
        mLastLocales = LocaleListCompat.getEmptyLocaleList();
        return locales;
    }

    public static LocaleListCompat getConfigLocales(Configuration conf) {
        if (Build.VERSION.SDK_INT >= 24) {
            return AppCompatDelegateImpl.Api24Impl.getLocales(conf);
        } else if (Build.VERSION.SDK_INT >= 21) {
            return LocaleListCompat.forLanguageTags(AppCompatDelegateImpl.Api21Impl
                    .toLanguageTag(conf.locale));
        } else {
            return LocaleListCompat.forLanguageTags(conf.locale.getLanguage());
        }
    }

    public static LocaleListCompat overlayCustomAndSystemLocales(LocaleListCompat customLocales,
            LocaleListCompat baseLocales) {
        if (Build.VERSION.SDK_INT >= 24) {
            return combineLocalesIfOverlayExists(customLocales, baseLocales);
        } else {
            return LocaleListCompat.create(customLocales.get(0));
        }
    }
}
