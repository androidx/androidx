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
import java.util.concurrent.TimeUnit;

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
            return LocaleListCompat.create(conf.locale);
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

    /**
     * Resets the number of received configuration changes.
     * <p>
     * Call this method before {@link #expectOnConfigurationChange(long)} to ensure only future
     * configuration changes are counted.
     */
    public void resetOnConfigurationChange() {
        mOnConfigurationChangeSemaphore.drainPermits();
    }

    /**
     * Blocks until a single configuration change has been received.
     * <p>
     * Configuration changes are sticky; if any configuration changes were received prior to
     * calling this method and {@link #resetOnConfigurationChange()} has not been called, this
     * method will return immediately.
     *
     * @param timeout maximum amount of time (in ms) to wait for a configuration change
     * @throws InterruptedException if the lock is interrupted
     */
    public void expectOnConfigurationChange(long timeout) throws InterruptedException {
        if (Thread.currentThread() == getMainLooper().getThread()) {
            throw new IllegalStateException("Method cannot be called on the Activity's UI thread");
        }

        mOnConfigurationChangeSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the number of received onCreate lifecycle events.
     * <p>
     * Call this method before {@link #expectOnCreate(long)} to ensure only future
     * onCreate lifecycle events are counted.
     */
    public void resetOnCreate() {
        mOnCreateSemaphore.drainPermits();
    }

    /**
     * Blocks until a single onCreate lifecycle event has been received.
     * <p>
     * Lifecycle events are sticky; if any events were received prior to calling this method and
     * an event has been received, this method will return immediately.
     *
     * @param timeout maximum amount of time (in ms) to wait for an onCreate event
     * @throws InterruptedException if the lock is interrupted
     */
    public void expectOnCreate(long timeout) throws InterruptedException {
        if (Thread.currentThread() == getMainLooper().getThread()) {
            throw new IllegalStateException("Method cannot be called on the Activity's UI thread");
        }

        mOnCreateSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the number of received onDestroy lifecycle events.
     * <p>
     * Call this method before {@link #expectOnDestroy(long)} to ensure only future
     * onDestroy lifecycle events are counted.
     */
    public void resetOnDestroy() {
        mOnDestroySemaphore.drainPermits();
    }

    /**
     * Blocks until a single onDestroy lifecycle event has been received.
     * <p>
     * Lifecycle events are sticky; if any events were received prior to calling this method and
     * an event has been received, this method will return immediately.
     *
     * @param timeout maximum amount of time (in ms) to wait for an onDestroy event
     * @throws InterruptedException if the lock is interrupted
     */
    public void expectOnDestroy(long timeout) throws InterruptedException {
        if (Thread.currentThread() == getMainLooper().getThread()) {
            throw new IllegalStateException("Method cannot be called on the Activity's UI thread");
        }

        mOnDestroySemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Teardown method to clean up persistent locales in static storage.
     */
    public static void teardown() {
        AppCompatDelegate.resetStaticRequestedAndStoredLocales();
    }

}
