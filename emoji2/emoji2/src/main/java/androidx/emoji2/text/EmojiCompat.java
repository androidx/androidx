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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.TESTS;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.AnyThread;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main class to keep Android devices up to date with the newest emojis by adding {@link EmojiSpan}s
 * to a given {@link CharSequence}.
 * <p/>
 * By default, EmojiCompat is initialized by {@link EmojiCompatInitializer}, which performs
 * deferred font loading to avoid potential app startup delays. The default behavior is to load
 * the font shortly after the first Activity resumes. EmojiCompatInitializer will configure
 * EmojiCompat to use the system emoji font provider via {@link DefaultEmojiCompatConfig} and
 * always creates a new background thread for font loading.
 * <p/>
 * EmojiCompat will only allow one instance to be initialized and any calls to
 * {@link #init(Config)} after the first one will have no effect. As a result, configuration options
 * may not be provided when using {@link EmojiCompatInitializer}. To provide a custom configuration,
 * disable {@link EmojiCompatInitializer} in the manifest with:
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
 * When not using EmojiCompatInitializer, EmojiCompat must to be initialized manually using
 * {@link #init(EmojiCompat.Config)}. It is recommended to make the initialization as early as
 * possible in your app, such as from {@link Application#onCreate()}.
 * <p/>
 * {@link #init(Config)} is fast and may be called from the main thread on the path to
 * displaying the first activity. However, loading the emoji font takes significant resources on a
 * background thread, so it is suggested to use {@link #LOAD_STRATEGY_MANUAL} in all manual
 * configurations to defer font loading until after the first screen displays. Font loading may
 * be started by calling {@link #load()}}. See the implementation {@link EmojiCompatInitializer}
 * for ideas when building a manual configuration.
 * <p/>
 * After initialization the {@link #get()} function can be used to get the configured instance and
 * the {@link #process(CharSequence)} function can be used to update a CharSequence with emoji
 * EmojiSpans.
 * <p/>
 * <pre><code>CharSequence processedSequence = EmojiCompat.get().process("some string")</pre>
 * <p/>
 * During loading information about emojis is not available. Before the
 * EmojiCompat instance has finished loading, calls to functions such as {@link
 * EmojiCompat#process(CharSequence)} will throw an exception. It is safe to call process when
 * {@link #getLoadState()} returns {@link #LOAD_STATE_SUCCEEDED}. To register a callback when
 * loading completes use {@link InitCallback}.
 * <p/>

 */
@AnyThread
public class EmojiCompat {
    /**
     * Key in {@link EditorInfo#extras} that represents the emoji metadata version used by the
     * widget. The existence of the value means that the widget is using EmojiCompat.
     * <p/>
     * If exists, the value for the key is an {@code int} and can be used to query EmojiCompat to
     * see whether the widget has the ability to display a certain emoji using
     * {@link #hasEmojiGlyph(CharSequence, int)}.
     */
    public static final String EDITOR_INFO_METAVERSION_KEY =
            "android.support.text.emoji.emojiCompat_metadataVersion";

    /**
     * Key in {@link EditorInfo#extras} that represents {@link
     * EmojiCompat.Config#setReplaceAll(boolean)} configuration parameter. The key is added only if
     * EmojiCompat is used by the widget. If exists, the value is a boolean.
     */
    public static final String EDITOR_INFO_REPLACE_ALL_KEY =
            "android.support.text.emoji.emojiCompat_replaceAll";

    /**
     * EmojiCompat instance is constructed, however the initialization did not start yet.
     *
     * @see #getLoadState()
     */
    public static final int LOAD_STATE_DEFAULT = 3;

    /**
     * EmojiCompat is initializing.
     *
     * @see #getLoadState()
     */
    // note: this may be returned as the value of mLoadState before constructor finishes due to
    // double-check lock
    public static final int LOAD_STATE_LOADING = 0;

    /**
     * EmojiCompat successfully initialized.
     *
     * @see #getLoadState()
     */
    public static final int LOAD_STATE_SUCCEEDED = 1;

    /**
     * An unrecoverable error occurred during initialization of EmojiCompat. Calls to functions
     * such as {@link #process(CharSequence)} will fail.
     *
     * @see #getLoadState()
     */
    public static final int LOAD_STATE_FAILED = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({LOAD_STATE_DEFAULT, LOAD_STATE_LOADING, LOAD_STATE_SUCCEEDED, LOAD_STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LoadState {
    }

    /**
     * Replace strategy that uses the value given in {@link EmojiCompat.Config}.
     *
     * @see #process(CharSequence, int, int, int, int)
     */
    public static final int REPLACE_STRATEGY_DEFAULT = 0;

    /**
     * Replace strategy to add {@link EmojiSpan}s for all emoji that were found.
     *
     * @see #process(CharSequence, int, int, int, int)
     */
    public static final int REPLACE_STRATEGY_ALL = 1;

    /**
     * Replace strategy to add {@link EmojiSpan}s only for emoji that do not exist in the system.
     */
    public static final int REPLACE_STRATEGY_NON_EXISTENT = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({REPLACE_STRATEGY_DEFAULT, REPLACE_STRATEGY_NON_EXISTENT, REPLACE_STRATEGY_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReplaceStrategy {
    }

    /**
     * {@link EmojiCompat} will start loading metadata when {@link #init(Config)} is called.
     *
     * @see Config#setMetadataLoadStrategy(int)
     */
    public static final int LOAD_STRATEGY_DEFAULT = 0;

    /**
     * {@link EmojiCompat} will wait for {@link #load()} to be called by developer in order to
     * start loading metadata.
     *
     * @see Config#setMetadataLoadStrategy(int)
     */
    public static final int LOAD_STRATEGY_MANUAL = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({LOAD_STRATEGY_DEFAULT, LOAD_STRATEGY_MANUAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadStrategy {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({EMOJI_UNSUPPORTED, EMOJI_SUPPORTED,
            EMOJI_FALLBACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CodepointSequenceMatchResult {
    }

    /**
     * Result of {@link #getEmojiMatch(CharSequence, int)} that means no part of this codepoint
     * sequence will ever generate an {@link EmojiSpan} at the requested metadata level.
     *
     * This return value implies:
     * - EmojiCompat will always defer to system emoji font
     * - System emoji font may or may not support this emoji
     * - This application MAY render this emoji
     *
     * This can be used by keyboards to learn that EmojiCompat does not support this codepoint
     * sequence at this metadata version. The system emoji font is not checked by this method,
     * and this result will be returned even if the system emoji font supports the emoji. This may
     * happen if the application is using an older version of the emoji compat font than the
     * system emoji font.
     *
     * Keyboards may optionally determine that the system emoji font will support the emoji, for
     * example by building a internal lookup table or calling
     * {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} to query the system
     * emoji font. Keyboards may use a lookup table to optimize this check, however they should be
     * aware that OEMs may add or remove emoji from the system emoji font.
     *
     * Keyboards may finally decide:
     * - If the system emoji font DOES NOT support the emoji, then the emoji IS NOT supported by
     * this application.
     * - If the system emoji font DOES support the emoji, then the emoji IS supported by this
     * application.
     * - If system emoji font is support is UNKNOWN, then assume the emoji IS NOT supported by
     * this application.
     */
    public static final int EMOJI_UNSUPPORTED = 0;

    /**
     * Result of {@link #getEmojiMatch(CharSequence, int)} that means this codepoint can be drawn
     * by an {@link EmojiSpan} at this metadata level.
     *
     * No further checks are required by keyboards for this result. The emoji is always supported
     * by this application.
     *
     * This return value implies:
     * - EmojiCompat can draw this emoji
     * - System emoji font may or may not support this emoji
     * - This application WILL render this emoji
     *
     * This result implies that EmojiCompat can successfully display this emoji. The system emoji
     * font is not checked by this method, and this result may be returned even if the platform
     * also supports the emoji sequence.
     *
     * If the application passes {@link EmojiCompat#REPLACE_STRATEGY_ALL} of true, then an
     * {@link EmojiSpan} will always be generated for this emoji.
     *
     * If the application passes {@link EmojiCompat#REPLACE_STRATEGY_ALL} of false, then an
     * {@link EmojiSpan} will only be generated if
     * {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)}
     * returns false for this emoji.
     */
    public static final int EMOJI_SUPPORTED = 1;

    /**
     * Result of {@link #getEmojiMatch(CharSequence, int)} that means the full codepoint sequence
     * is not known to emojicompat, but at least one subsequence is an emoji that is known at
     * this metadata level.
     *
     * Keyboards may decide that this emoji is not supported by the application when this result is
     * returned, with no further processing.
     *
     * This return value implies:
     * - EmojiCompat will decompose this ZWJ sequence into multiple glyphs when replaceAll=true
     * - EmojiCompat MAY defer to platform when replaceAll=false
     * - System emoji font may or may not support this emoji
     * - This application MAY render this emoji
     *
     * This return value is only ever returned for ZWJ sequences. To understand this result
     * consider when it may be returned for the multi-skin-tone handshake introduced in emoji 14.
     *
     * <pre>
     *     U+1FAF1 // unknown @ requested metadata level
     *     U+1F3FB // metadata level 1
     *     U+200D  // not displayed (ZWJ)
     *     U+1FAF2 // unknown @ requested metadata level
     *     U+1F3FD // metadata level 1
     * </pre>
     *
     * In this codepoint sequence, U+1F3FB and U+1F3FD are known from metadata level 1. When an
     * application is using a metadata level that doesn't understand this ZWJ and provides
     * {@link EmojiCompat#REPLACE_STRATEGY_ALL} true, the color emoji are matched and replaced
     * with {@link EmojiSpan}. The system emoji font, even if it supports this ZWJ sequence, is
     * never queried and the added EmojiSpans force fallback rendering for the ZWJ sequence.
     *
     * The glyph will only display correctly for this application if ALL of the following
     * requirements are met:
     * - {@link EmojiCompat#REPLACE_STRATEGY_ALL} is false
     * - {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} returns true for each
     * emoji subsequence known at this metadata level
     * - {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} returns true for the
     * full sequence
     *
     * Given this return value for the multi-skin-tone handshake above, if
     * {@link EmojiCompat#REPLACE_STRATEGY_ALL} is false then the emoji will display if the
     * entire emoji sequence is matched by
     * {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} because U+1F3FB and
     * U+1F3FD are both in the system emoji font.
     *
     * Keyboards that wish to determine if the glyph will display correctly by the application in
     * response to this return value should consider building an internal lookup for new ZWJ
     * sequences instead of repeatedly calling
     * {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} for each emoji
     * subsequence.
     */
    public static final int EMOJI_FALLBACK = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    static final int EMOJI_COUNT_UNLIMITED = Integer.MAX_VALUE;

    private static final Object INSTANCE_LOCK = new Object();
    private static final Object CONFIG_LOCK = new Object();

    @GuardedBy("INSTANCE_LOCK")
    private static volatile @Nullable EmojiCompat sInstance;
    @GuardedBy("CONFIG_LOCK")
    private static volatile boolean sHasDoneDefaultConfigLookup;

    private final @NonNull ReadWriteLock mInitLock;

    @GuardedBy("mInitLock")
    private final @NonNull Set<InitCallback> mInitCallbacks;

    @GuardedBy("mInitLock")
    @LoadState
    private volatile int mLoadState;

    /**
     * Handler with main looper to run the callbacks on.
     */
    private final @NonNull Handler mMainHandler;

    /**
     * Helper class for pre 19 compatibility.
     */
    private final @NonNull CompatInternal mHelper;

    /**
     * Metadata loader instance given in the Config instance.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull MetadataRepoLoader mMetadataLoader;

    private @NonNull final SpanFactory mSpanFactory;

    /**
     * @see Config#setReplaceAll(boolean)
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final boolean mReplaceAll;

    /**
     * @see Config#setUseEmojiAsDefaultStyle(boolean)
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final boolean mUseEmojiAsDefaultStyle;

    /**
     * @see Config#setUseEmojiAsDefaultStyle(boolean, List)
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @Nullable int[] mEmojiAsDefaultStyleExceptions;

    /**
     * @see Config#setEmojiSpanIndicatorEnabled(boolean)
     */
    private final boolean mEmojiSpanIndicatorEnabled;

    /**
     * @see Config#setEmojiSpanIndicatorColor(int)
     */
    private final int mEmojiSpanIndicatorColor;

    /**
     * @see Config#setMetadataLoadStrategy(int)
     */
    @LoadStrategy private final int mMetadataLoadStrategy;

    /**
     * @see Config#setGlyphChecker(GlyphChecker)
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    private final GlyphChecker mGlyphChecker;

    private static final String NOT_INITIALIZED_ERROR_TEXT = "EmojiCompat is not initialized.\n"
            + "\n"
            + "You must initialize EmojiCompat prior to referencing the EmojiCompat instance.\n"
            + "\n"
            + "The most likely cause of this error is disabling the EmojiCompatInitializer\n"
            + "either explicitly in AndroidManifest.xml, or by including\n"
            + "androidx.emoji2:emoji2-bundled.\n"
            + "\n"
            + "Automatic initialization is typically performed by EmojiCompatInitializer. If\n"
            + "you are not expecting to initialize EmojiCompat manually in your application,\n"
            + "please check to ensure it has not been removed from your APK's manifest. You can\n"
            + "do this in Android Studio using Build > Analyze APK.\n"
            + "\n"
            + "In the APK Analyzer, ensure that the startup entry for\n"
            + "EmojiCompatInitializer and InitializationProvider is present in\n"
            + " AndroidManifest.xml. If it is missing or contains tools:node=\"remove\", and you\n"
            + "intend to use automatic configuration, verify:\n"
            + "\n"
            + "  1. Your application does not include emoji2-bundled\n"
            + "  2. All modules do not contain an exclusion manifest rule for\n"
            + "     EmojiCompatInitializer or InitializationProvider. For more information\n"
            + "     about manifest exclusions see the documentation for the androidx startup\n"
            + "     library.\n"
            + "\n"
            + "If you intend to use emoji2-bundled, please call EmojiCompat.init. You can\n"
            + "learn more in the documentation for BundledEmojiCompatConfig.\n"
            + "\n"
            + "If you intended to perform manual configuration, it is recommended that you call\n"
            + "EmojiCompat.init immediately on application startup.\n"
            + "\n"
            + "If you still cannot resolve this issue, please open a bug with your specific\n"
            + "configuration to help improve error message.";

    /**
     * Private constructor for singleton instance.
     *
     * @see #init(Config)
     */
    private EmojiCompat(@NonNull final Config config) {
        mInitLock = new ReentrantReadWriteLock();
        mLoadState = LOAD_STATE_DEFAULT;
        mReplaceAll = config.mReplaceAll;
        mUseEmojiAsDefaultStyle = config.mUseEmojiAsDefaultStyle;
        mEmojiAsDefaultStyleExceptions = config.mEmojiAsDefaultStyleExceptions;
        mEmojiSpanIndicatorEnabled = config.mEmojiSpanIndicatorEnabled;
        mEmojiSpanIndicatorColor = config.mEmojiSpanIndicatorColor;
        mMetadataLoader = config.mMetadataLoader;
        mMetadataLoadStrategy = config.mMetadataLoadStrategy;
        mGlyphChecker = config.mGlyphChecker;
        mMainHandler = new Handler(Looper.getMainLooper());
        mInitCallbacks = new ArraySet<>();
        SpanFactory localSpanFactory = config.mSpanFactory;
        mSpanFactory = localSpanFactory != null ? localSpanFactory : new DefaultSpanFactory();
        if (config.mInitCallbacks != null && !config.mInitCallbacks.isEmpty()) {
            mInitCallbacks.addAll(config.mInitCallbacks);
        }
        mHelper = Build.VERSION.SDK_INT < 19 ? new CompatInternal(this) : new CompatInternal19(
                this);
        loadMetadata();
    }

    /**
     * Initialize the singleton instance with the default system-provided configuration.
     *
     * <p>This is the recommended configuration for most applications. For more details see
     * {@link DefaultEmojiCompatConfig}.</p>
     *
     * <p>This call will use {@link DefaultEmojiCompatConfig} to lookup the default emoji font
     * provider installed on the system and use that, if present. If there is no default font
     * provider onthe system, this call will have no effect.</p>
     *
     * <p>Note: EmojiCompat may only be initialized once, and will return the same instance
     * afterwords.</p>
     *
     * @return Default EmojiCompat for this device, or null if there is no provider on the system.
     */
    @Nullable
    public static EmojiCompat init(@NonNull Context context) {
        return init(context, null);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    @SuppressWarnings("GuardedBy") /* double-check lock; volatile; threadsafe obj */
    public static EmojiCompat init(@NonNull Context context,
            @Nullable DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory defaultFactory) {
        EmojiCompat.Config config;
        if (sHasDoneDefaultConfigLookup) {
            // sInstance is safe to return outside the lock because
            // 1) static fields are volatile
            // 2) all fields on EmojiCompat are final, or guarded by a lock
            // 3) we only write this after sInstance is settled by the call to `init`
            return sInstance;
        } else {
            DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory =
                    defaultFactory != null ? defaultFactory :
                            new DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(null);
            config = factory.create(context);
        }
        synchronized (CONFIG_LOCK) {
            if (!sHasDoneDefaultConfigLookup) {
                // sDefaultConfigLookup allows us to early-exit above, as well as avoid repeated
                // calls to create in the case where the font provider is not found
                if (config != null) {
                    init(config);
                }
                // write this after init to allow safe early-exit
                sHasDoneDefaultConfigLookup = true;

            }
            return sInstance;
        }
    }

    /**
     * Initialize the singleton instance with a configuration. When used on devices running API 18
     * or below, the singleton instance is immediately moved into {@link #LOAD_STATE_SUCCEEDED}
     * state without loading any metadata. When called for the first time, the library will create
     * the singleton instance and any call after that will not create a new instance and return
     * immediately.
     *
     * @see EmojiCompat.Config
     */
    @SuppressWarnings("GuardedBy") /* double-check lock; volatile sInstance; threadsafe obj */
    @NonNull
    public static EmojiCompat init(@NonNull final Config config) {
        // copy to local for null-checker
        EmojiCompat localInstance = sInstance;
        if (localInstance == null) {
            synchronized (INSTANCE_LOCK) {
                localInstance = sInstance;
                if (localInstance == null) {
                    localInstance = new EmojiCompat(config);
                    sInstance = localInstance;
                }
            }
        }
        return localInstance;
    }

    /**
     * Return true if EmojiCompat has been configured by a successful call to
     * {@link EmojiCompat#init}.
     *
     * You can use this to check if {@link EmojiCompat#get()} will return a valid EmojiCompat
     * instance.
     *
     * This function does not check the {@link #getLoadState()} and will return true even if the
     * font is still loading, or has failed to load.
     *
     * @return true if EmojiCompat has been successfully initialized.
     */
    @SuppressWarnings("GuardedBy") // same rationale as double-check lock
    public static boolean isConfigured() {
        // Note: this is true immediately after calling .init(Config).
        //
        // These are three situations this may return false
        //   1) An app has disabled EmojiCompatInitializer and does not intend to call .init.
        //   2) EmojiCompatInitializer did not find a configuration
        //   3) EmojiCompatInitializer was disable or failed, and the app will call .init. In the
        //   future it will return true.
        //
        // In case one and two, this method will always return false for the duration of the
        // application lifecycle.
        //
        // In case three, this will return true at some future point. There is no callback
        // mechanism to learn about the init call due to the high potential for leaked references
        // in a static context if it's actually case 2 (when using manual callback registration).
        //
        // It is recommended that applications call init prior to creating any screens that
        // may show emoji or user generated content.
        return sInstance != null;
    }


    /**
     * Used by the tests to reset EmojiCompat with a new configuration. Every time it is called a
     * new instance is created with the new configuration.
     *
     * @hide
     */
    @NonNull
    public static EmojiCompat reset(@NonNull final Config config) {
        synchronized (INSTANCE_LOCK) {
            EmojiCompat localInstance = new EmojiCompat(config);
            sInstance = localInstance;
            return localInstance;
        }
    }

    /**
     * Used by the tests to reset EmojiCompat with a new singleton instance.
     *
     * @hide
     */
    @RestrictTo(TESTS)
    @Nullable
    public static EmojiCompat reset(@Nullable final EmojiCompat emojiCompat) {
        synchronized (INSTANCE_LOCK) {
            sInstance = emojiCompat;
            return sInstance;
        }
    }

    /**
     * Reset default configuration lookup flag, for tests.
     *
     * @hide
     */
    @RestrictTo(TESTS)
    public static void skipDefaultConfigurationLookup(boolean shouldSkip) {
        synchronized (CONFIG_LOCK) {
            sHasDoneDefaultConfigLookup = shouldSkip;
        }
    }

    /**
     * Return singleton EmojiCompat instance. Should be called after
     * {@link #init(EmojiCompat.Config)} is called to initialize the singleton instance.
     *
     * @return EmojiCompat instance
     *
     * @throws IllegalStateException if called before {@link #init(EmojiCompat.Config)}
     */
    @NonNull
    public static EmojiCompat get() {
        synchronized (INSTANCE_LOCK) {
            EmojiCompat localInstance = sInstance;
            Preconditions.checkState(localInstance != null, NOT_INITIALIZED_ERROR_TEXT);
            return localInstance;
        }
    }

    /**
     * When {@link Config#setMetadataLoadStrategy(int)} is set to {@link #LOAD_STRATEGY_MANUAL},
     * this function starts loading the metadata. Calling the function when
     * {@link Config#setMetadataLoadStrategy(int)} is {@code not} set to
     * {@link #LOAD_STRATEGY_MANUAL} will throw an exception. The load will {@code not} start if:
     * <ul>
     *     <li>the metadata is already loaded successfully and {@link #getLoadState()} is
     *     {@link #LOAD_STATE_SUCCEEDED}.
     *     </li>
     *      <li>a previous load attempt is not finished yet and {@link #getLoadState()} is
     *     {@link #LOAD_STATE_LOADING}.</li>
     * </ul>
     *
     * @throws IllegalStateException when {@link Config#setMetadataLoadStrategy(int)} is not set
     * to {@link #LOAD_STRATEGY_MANUAL}
     */
    public void load() {
        Preconditions.checkState(mMetadataLoadStrategy == LOAD_STRATEGY_MANUAL,
                "Set metadataLoadStrategy to LOAD_STRATEGY_MANUAL to execute manual loading");
        if (isInitialized()) return;

        mInitLock.writeLock().lock();
        try {
            if (mLoadState == LOAD_STATE_LOADING) return;
            mLoadState = LOAD_STATE_LOADING;
        } finally {
            mInitLock.writeLock().unlock();
        }

        mHelper.loadMetadata();
    }

    private void loadMetadata() {
        mInitLock.writeLock().lock();
        try {
            if (mMetadataLoadStrategy == LOAD_STRATEGY_DEFAULT) {
                mLoadState = LOAD_STATE_LOADING;
            }
        } finally {
            mInitLock.writeLock().unlock();
        }

        if (getLoadState() == LOAD_STATE_LOADING) {
            mHelper.loadMetadata();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onMetadataLoadSuccess() {
        final Collection<InitCallback> initCallbacks = new ArrayList<>();
        mInitLock.writeLock().lock();
        try {
            mLoadState = LOAD_STATE_SUCCEEDED;
            initCallbacks.addAll(mInitCallbacks);
            mInitCallbacks.clear();
        } finally {
            mInitLock.writeLock().unlock();
        }

        mMainHandler.post(new ListenerDispatcher(initCallbacks, mLoadState));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onMetadataLoadFailed(@Nullable final Throwable throwable) {
        final Collection<InitCallback> initCallbacks = new ArrayList<>();
        mInitLock.writeLock().lock();
        try {
            mLoadState = LOAD_STATE_FAILED;
            initCallbacks.addAll(mInitCallbacks);
            mInitCallbacks.clear();
        } finally {
            mInitLock.writeLock().unlock();
        }
        mMainHandler.post(new ListenerDispatcher(initCallbacks, mLoadState, throwable));
    }

    /**
     * Registers an initialization callback. If the initialization is already completed by the time
     * the listener is added, the callback functions are called immediately. Callbacks are called on
     * the main looper.
     * <p/>
     * When used on devices running API 18 or below, {@link InitCallback#onInitialized()} is called
     * without loading any metadata. In such cases {@link InitCallback#onFailed(Throwable)} is never
     * called.
     *
     * @param initCallback the initialization callback to register, cannot be {@code null}
     *
     * @see #unregisterInitCallback(InitCallback)
     */
    @SuppressWarnings("ExecutorRegistration")
    public void registerInitCallback(@NonNull InitCallback initCallback) {
        Preconditions.checkNotNull(initCallback, "initCallback cannot be null");

        mInitLock.writeLock().lock();
        try {
            if (mLoadState == LOAD_STATE_SUCCEEDED || mLoadState == LOAD_STATE_FAILED) {
                mMainHandler.post(new ListenerDispatcher(initCallback, mLoadState));
            } else {
                mInitCallbacks.add(initCallback);
            }
        } finally {
            mInitLock.writeLock().unlock();
        }
    }

    /**
     * Unregisters a callback that was added before.
     *
     * @param initCallback the callback to be removed, cannot be {@code null}
     */
    public void unregisterInitCallback(@NonNull InitCallback initCallback) {
        Preconditions.checkNotNull(initCallback, "initCallback cannot be null");
        mInitLock.writeLock().lock();
        try {
            mInitCallbacks.remove(initCallback);
        } finally {
            mInitLock.writeLock().unlock();
        }
    }

    /**
     * Returns loading state of the EmojiCompat instance. When used on devices running API 18 or
     * below always returns {@link #LOAD_STATE_SUCCEEDED}.
     *
     * @return one of {@link #LOAD_STATE_DEFAULT}, {@link #LOAD_STATE_LOADING},
     * {@link #LOAD_STATE_SUCCEEDED}, {@link #LOAD_STATE_FAILED}
     */
    public @LoadState int getLoadState() {
        mInitLock.readLock().lock();
        try {
            return mLoadState;
        } finally {
            mInitLock.readLock().unlock();
        }
    }

    /**
     * @return {@code true} if EmojiCompat is successfully initialized
     */
    private boolean isInitialized() {
        return getLoadState() == LOAD_STATE_SUCCEEDED;
    }

    /**
     * @return whether a background should be drawn for the emoji for debugging
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isEmojiSpanIndicatorEnabled() {
        return mEmojiSpanIndicatorEnabled;
    }

    /**
     * @return color of background drawn if {@link EmojiCompat#isEmojiSpanIndicatorEnabled} is true
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public @ColorInt int getEmojiSpanIndicatorColor() {
        return mEmojiSpanIndicatorColor;
    }

    /**
     * Together with {@link #getEmojiEnd(CharSequence, int)}, if the character at {@code offset} is
     * part of an emoji, returns the index range of that emoji, start index inclusively/end index
     * exclusively so that {@code charSequence.subSequence(start, end)} will return that emoji.
     * E.g., getEmojiStart/End("AB😀", 1) will return (-1,-1) since 'B' is not part an emoji;
     *       getEmojiStart/End("AB😀", 3) will return [2,4), note that "😀" contains 2 Chars.
     * Returns -1 otherwise.
     * @param charSequence the whole sequence
     * @param offset index of the emoji to look up
     * @return the start index inclusively/end index exclusively
     */
    public int getEmojiStart(@NonNull final CharSequence charSequence,
            @IntRange(from = 0) int offset) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(charSequence, "charSequence cannot be null");
        return mHelper.getEmojiStart(charSequence, offset);
    }

    /**
     * see {@link #getEmojiStart(CharSequence, int)}.
     */
    public int getEmojiEnd(@NonNull final CharSequence charSequence,
            @IntRange(from = 0) int offset) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(charSequence, "charSequence cannot be null");
        return mHelper.getEmojiEnd(charSequence, offset);
    }

    /**
     * Handles onKeyDown commands from a {@link KeyListener} and if {@code keyCode} is one of
     * {@link KeyEvent#KEYCODE_DEL} or {@link KeyEvent#KEYCODE_FORWARD_DEL} it tries to delete an
     * {@link EmojiSpan} from an {@link Editable}. Returns {@code true} if an {@link EmojiSpan} is
     * deleted with the characters it covers.
     * <p/>
     * If there is a selection where selection start is not equal to selection end, does not
     * delete.
     * <p/>
     * When used on devices running API 18 or below, always returns {@code false}.
     *
     * @param editable Editable instance passed to {@link KeyListener#onKeyDown(android.view.View,
     *                 Editable, int, KeyEvent)}
     * @param keyCode keyCode passed to {@link KeyListener#onKeyDown(android.view.View, Editable,
     *                int, KeyEvent)}
     * @param event KeyEvent passed to {@link KeyListener#onKeyDown(android.view.View, Editable,
     *              int, KeyEvent)}
     *
     * @return {@code true} if an {@link EmojiSpan} is deleted
     */
    public static boolean handleOnKeyDown(@NonNull final Editable editable, final int keyCode,
            @NonNull final KeyEvent event) {
        if (Build.VERSION.SDK_INT >= 19) {
            return EmojiProcessor.handleOnKeyDown(editable, keyCode, event);
        } else {
            return false;
        }
    }

    /**
     * Handles deleteSurroundingText commands from {@link InputConnection} and tries to delete an
     * {@link EmojiSpan} from an {@link Editable}. Returns {@code true} if an {@link EmojiSpan} is
     * deleted.
     * <p/>
     * If there is a selection where selection start is not equal to selection end, does not
     * delete.
     * <p/>
     * When used on devices running API 18 or below, always returns {@code false}.
     *
     * @param inputConnection InputConnection instance
     * @param editable TextView.Editable instance
     * @param beforeLength the number of characters before the cursor to be deleted
     * @param afterLength the number of characters after the cursor to be deleted
     * @param inCodePoints {@code true} if length parameters are in codepoints
     *
     * @return {@code true} if an {@link EmojiSpan} is deleted
     */
    public static boolean handleDeleteSurroundingText(
            @NonNull final InputConnection inputConnection, @NonNull final Editable editable,
            @IntRange(from = 0) final int beforeLength, @IntRange(from = 0) final int afterLength,
            final boolean inCodePoints) {
        if (Build.VERSION.SDK_INT >= 19) {
            return EmojiProcessor.handleDeleteSurroundingText(inputConnection, editable,
                    beforeLength, afterLength, inCodePoints);
        } else {
            return false;
        }
    }

    /**
     * Returns {@code true} if EmojiCompat is capable of rendering an emoji. When used on devices
     * running API 18 or below, always returns {@code false}.
     *
     * @deprecated use getEmojiMatch which returns more accurate lookup information.
     *
     * @param sequence CharSequence representing the emoji
     *
     * @return {@code true} if EmojiCompat can render given emoji, cannot be {@code null}
     *
     * @throws IllegalStateException if not initialized yet
     */
    @Deprecated
    public boolean hasEmojiGlyph(@NonNull final CharSequence sequence) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        return mHelper.hasEmojiGlyph(sequence);
    }

    /**
     * Returns {@code true} if EmojiCompat is capable of rendering an emoji at the given metadata
     * version. When used on devices running API 18 or below, always returns {@code false}.
     *
     * @deprecated use getEmojiMatch which returns more accurate lookup information.
     *
     * @param sequence CharSequence representing the emoji
     * @param metadataVersion the metadata version to check against, should be greater than or
     *                        equal to {@code 0},
     *
     * @return {@code true} if EmojiCompat can render given emoji, cannot be {@code null}
     *
     * @throws IllegalStateException if not initialized yet
     */
    @Deprecated
    public boolean hasEmojiGlyph(@NonNull final CharSequence sequence,
            @IntRange(from = 0) final int metadataVersion) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        return mHelper.hasEmojiGlyph(sequence, metadataVersion);
    }

    /**
     * Attempts to lookup the entire sequence at the specified metadata version and returns what
     * the runtime match behavior would be.
     *
     * To be used by keyboards to show or hide emoji in response to specific metadata support.
     *
     * @see #EMOJI_SUPPORTED
     * @see #EMOJI_UNSUPPORTED
     * @see #EMOJI_FALLBACK
     *
     * @param sequence CharSequence representing an emoji
     * @param metadataVersion the metada version to check against, should be greater than or
     *                        equal to {@code 0},
     * @return A match result, or decomposes if replaceAll would cause partial subsequence matches.
     */
    @CodepointSequenceMatchResult
    public int getEmojiMatch(@NonNull CharSequence sequence,
            @IntRange(from = 0) final int metadataVersion) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        return mHelper.getEmojiMatch(sequence, metadataVersion);
    }

    /**
     * Checks a given CharSequence for emojis, and adds EmojiSpans if any emojis are found. When
     * used on devices running API 18 or below, returns the given {@code charSequence} without
     * processing it.
     *
     * @param charSequence CharSequence to add the EmojiSpans
     *
     * @throws IllegalStateException if not initialized yet
     * @see #process(CharSequence, int, int)
     */
    @Nullable
    @CheckResult
    public CharSequence process(@Nullable final CharSequence charSequence) {
        // since charSequence might be null here we have to check it. Passing through here to the
        // main function so that it can do all the checks including isInitialized. It will also
        // be the main point that decides what to return.

        @IntRange(from = 0) final int length = charSequence == null ? 0 : charSequence.length();
        return process(charSequence, 0, length);
    }

    /**
     * Checks a given CharSequence for emojis, and adds EmojiSpans if any emojis are found.
     * <p>
     * <ul>
     * <li>If no emojis are found, {@code charSequence} given as the input is returned without
     * any changes. i.e. charSequence is a String, and no emojis are found, the same String is
     * returned.</li>
     * <li>If the given input is not a Spannable (such as String), and at least one emoji is found
     * a new {@link android.text.Spannable} instance is returned. </li>
     * <li>If the given input is a Spannable, the same instance is returned. </li>
     * </ul>
     * When used on devices running API 18 or below, returns the given {@code charSequence} without
     * processing it.
     *
     * @param charSequence CharSequence to add the EmojiSpans, cannot be {@code null}
     * @param start start index in the charSequence to look for emojis, should be greater than or
     *              equal to {@code 0}, also less than or equal to {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or equal
     *              to {@code start} parameter, also less than or equal to
     *              {@code charSequence.length()}
     *
     * @throws IllegalStateException if not initialized yet
     * @throws IllegalArgumentException in the following cases:
     *                                  {@code start < 0}, {@code end < 0}, {@code end < start},
     *                                  {@code start > charSequence.length()},
     *                                  {@code end > charSequence.length()}
     */
    @Nullable
    @CheckResult
    public CharSequence process(@Nullable final CharSequence charSequence,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end) {
        return process(charSequence, start, end, EMOJI_COUNT_UNLIMITED);
    }

    /**
     * Checks a given CharSequence for emojis, and adds EmojiSpans if any emojis are found.
     * <p>
     * <ul>
     * <li>If no emojis are found, {@code charSequence} given as the input is returned without
     * any changes. i.e. charSequence is a String, and no emojis are found, the same String is
     * returned.</li>
     * <li>If the given input is not a Spannable (such as String), and at least one emoji is found
     * a new {@link android.text.Spannable} instance is returned. </li>
     * <li>If the given input is a Spannable, the same instance is returned. </li>
     * </ul>
     * When used on devices running API 18 or below, returns the given {@code charSequence} without
     * processing it.
     *
     * @param charSequence CharSequence to add the EmojiSpans, cannot be {@code null}
     * @param start start index in the charSequence to look for emojis, should be greater than or
     *              equal to {@code 0}, also less than or equal to {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than or equal to
     *            {@code charSequence.length()}
     * @param maxEmojiCount maximum number of emojis in the {@code charSequence}, should be greater
     *                      than or equal to {@code 0}
     *
     * @throws IllegalStateException if not initialized yet
     * @throws IllegalArgumentException in the following cases:
     *                                  {@code start < 0}, {@code end < 0}, {@code end < start},
     *                                  {@code start > charSequence.length()},
     *                                  {@code end > charSequence.length()}
     *                                  {@code maxEmojiCount < 0}
     */
    @Nullable
    @CheckResult
    public CharSequence process(@Nullable final CharSequence charSequence,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end,
            @IntRange(from = 0) final int maxEmojiCount) {
        return process(charSequence, start, end, maxEmojiCount, REPLACE_STRATEGY_DEFAULT);
    }

    /**
     * Checks a given CharSequence for emojis, and adds EmojiSpans if any emojis are found.
     * <p>
     * <ul>
     * <li>If no emojis are found, {@code charSequence} given as the input is returned without
     * any changes. i.e. charSequence is a String, and no emojis are found, the same String is
     * returned.</li>
     * <li>If the given input is not a Spannable (such as String), and at least one emoji is found
     * a new {@link android.text.Spannable} instance is returned. </li>
     * <li>If the given input is a Spannable, the same instance is returned. </li>
     * </ul>
     * When used on devices running API 18 or below, returns the given {@code charSequence} without
     * processing it.
     *
     * @param charSequence CharSequence to add the EmojiSpans, cannot be {@code null}
     * @param start start index in the charSequence to look for emojis, should be greater than or
     *              equal to {@code 0}, also less than or equal to {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than or equal to
     *            {@code charSequence.length()}
     * @param maxEmojiCount maximum number of emojis in the {@code charSequence}, should be greater
     *                      than or equal to {@code 0}
     * @param replaceStrategy whether to replace all emoji with {@link EmojiSpan}s, should be one of
     *                        {@link #REPLACE_STRATEGY_DEFAULT},
     *                        {@link #REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link #REPLACE_STRATEGY_ALL}
     *
     * @throws IllegalStateException if not initialized yet
     * @throws IllegalArgumentException in the following cases:
     *                                  {@code start < 0}, {@code end < 0}, {@code end < start},
     *                                  {@code start > charSequence.length()},
     *                                  {@code end > charSequence.length()}
     *                                  {@code maxEmojiCount < 0}
     */
    @Nullable
    @CheckResult
    public CharSequence process(@Nullable final CharSequence charSequence,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end,
            @IntRange(from = 0) final int maxEmojiCount, @ReplaceStrategy int replaceStrategy) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkArgumentNonnegative(start, "start cannot be negative");
        Preconditions.checkArgumentNonnegative(end, "end cannot be negative");
        Preconditions.checkArgumentNonnegative(maxEmojiCount, "maxEmojiCount cannot be negative");
        Preconditions.checkArgument(start <= end, "start should be <= than end");

        // early return since there is nothing to do
        if (charSequence == null) {
            return null;
        }

        Preconditions.checkArgument(start <= charSequence.length(),
                "start should be < than charSequence length");
        Preconditions.checkArgument(end <= charSequence.length(),
                "end should be < than charSequence length");

        // early return since there is nothing to do
        if (charSequence.length() == 0 || start == end) {
            return charSequence;
        }

        final boolean replaceAll;
        switch (replaceStrategy) {
            case REPLACE_STRATEGY_ALL:
                replaceAll = true;
                break;
            case REPLACE_STRATEGY_NON_EXISTENT:
                replaceAll = false;
                break;
            case REPLACE_STRATEGY_DEFAULT:
            default:
                replaceAll = mReplaceAll;
                break;
        }

        return mHelper.process(charSequence, start, end, maxEmojiCount, replaceAll);
    }

    /**
     * Returns signature for the currently loaded emoji assets. The signature is a SHA that is
     * constructed using emoji assets. Can be used to detect if currently loaded asset is different
     * then previous executions. When used on devices running API 18 or below, returns empty string.
     *
     * @throws IllegalStateException if not initialized yet
     */
    @NonNull
    public String getAssetSignature() {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        return mHelper.getAssetSignature();
    }

    /**
     * Updates the EditorInfo attributes in order to communicate information to Keyboards. When
     * used on devices running API 18 or below, does not update EditorInfo attributes.
     *
     * This is called from EditText integrations that use EmojiEditTextHelper. Custom
     * widgets that allow IME not subclassing EditText should call this method when creating an
     * input connection.
     *
     * When EmojiCompat is not in {@link #LOAD_STATE_SUCCEEDED}, this method has no effect.
     *
     * Calling this method on API levels below API 19 will have no effect, as EmojiCompat may
     * never be configured. However, it is always safe to call, even on older API levels.
     *
     * @param outAttrs EditorInfo instance passed to
     *                 {@link android.widget.TextView#onCreateInputConnection(EditorInfo)}
     *
     * @see #EDITOR_INFO_METAVERSION_KEY
     * @see #EDITOR_INFO_REPLACE_ALL_KEY
     */
    public void updateEditorInfo(@NonNull final EditorInfo outAttrs) {
        //noinspection ConstantConditions
        if (!isInitialized() || outAttrs == null) {
            return;
        }
        if (outAttrs.extras == null) {
            outAttrs.extras = new Bundle();
        }
        mHelper.updateEditorInfoAttrs(outAttrs);
    }

    /**
     * Factory class that creates the EmojiSpans.
     *
     * By default it creates {@link TypefaceEmojiSpan}.
     *
     * Apps should use this only if they want to control the drawing of EmojiSpans for non-standard
     * emoji display (for example, resizing or repositioning emoji).
     */
    public interface SpanFactory {
        /**
         * Create EmojiSpan instance.
         *
         * @param rasterizer TypefaceEmojiRasterizer instance, which can draw the emoji onto a
         *                   Canvas.
         *
         * @return EmojiSpan instance that can use TypefaceEmojiRasterizer to draw emoji.
         */
        @RequiresApi(19)
        @NonNull
        EmojiSpan createSpan(@NonNull TypefaceEmojiRasterizer rasterizer);
    }


    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class DefaultSpanFactory implements SpanFactory {

        /**
         * Returns a TypefaceEmojiSpan.
         *
         * @param rasterizer TypefaceEmojiRasterizer instance, which can draw the emoji onto a
         *                   Canvas.
         *
         * @return {@link TypefaceEmojiSpan}
         */
        @RequiresApi(19)
        @NonNull
        @Override
        public EmojiSpan createSpan(@NonNull TypefaceEmojiRasterizer rasterizer) {
            return new TypefaceEmojiSpan(rasterizer);
        }
    }

    /**
     * Listener class for the initialization of the EmojiCompat.
     */
    public abstract static class InitCallback {
        /**
         * Called when EmojiCompat is initialized and the emoji data is loaded. When used on devices
         * running API 18 or below, this function is always called.
         */
        public void onInitialized() {
        }

        /**
         * Called when an unrecoverable error occurs during EmojiCompat initialization. When used on
         * devices running API 18 or below, this function is never called.
         */
        public void onFailed(@SuppressWarnings("unused") @Nullable Throwable throwable) {
        }
    }

    /**
     * Interface to load emoji metadata.
     */
    public interface MetadataRepoLoader {
        /**
         * Start loading the metadata. When the loading operation is finished {@link
         * MetadataRepoLoaderCallback#onLoaded(MetadataRepo)} or
         * {@link MetadataRepoLoaderCallback#onFailed(Throwable)} should be called. When used on
         * devices running API 18 or below, this function is never called.
         *
         * @param loaderCallback callback to signal the loading state
         */
        @SuppressWarnings("ExecutorRegistration")
        void load(@NonNull MetadataRepoLoaderCallback loaderCallback);
    }

    /**
     * Interface to check if a given emoji exists on the system.
     */
    public interface GlyphChecker {
        /**
         * Return {@code true} if the emoji that is in {@code charSequence} between
         * {@code start}(inclusive) and {@code end}(exclusive) can be rendered on the system
         * using the default Typeface.
         *
         * <p>This function is called after an emoji is identified in the given {@code charSequence}
         * and EmojiCompat wants to know if that emoji can be rendered on the system. The result
         * of this call will be cached and the same emoji sequence won't be asked for the same
         * EmojiCompat instance.
         *
         * <p>When the function returns {@code true}, it will mean that the system can render the
         * emoji. In that case if {@link Config#setReplaceAll} is set to {@code false}, then no
         * {@link EmojiSpan} will be added in the final emoji processing result.
         *
         * <p>When the function returns {@code false}, it will mean that the system cannot render
         * the given emoji, therefore an {@link EmojiSpan} will be added to the final emoji
         * processing result.
         *
         * <p>The default implementation of this class uses
         * {@link androidx.core.graphics.PaintCompat#hasGlyph(Paint, String)} function to check
         * if the emoji can be rendered on the system. This is required even if EmojiCompat
         * knows about the SDK Version that the emoji was added on AOSP. Just the {@code sdkAdded}
         * information is not enough to reliably decide if emoji can be rendered since this
         * information may not be consistent across all the OEMs and all the Android versions.
         *
         * <p>With this interface you can apply your own heuristics to check if the emoji can be
         * rendered on the system. For example, if you'd like to rely on the {@code sdkAdded}
         * information, and some predefined OEMs, it is possible to write the following code
         * snippet.
         *
         * {@sample frameworks/support/samples/SupportEmojiDemos/src/main/java/com/example/android/support/text/emoji/sample/GlyphCheckerSample.java glyphchecker}
         *
         * @param charSequence the CharSequence that is being processed
         * @param start the inclusive starting offset for the emoji in the {@code charSequence}
         * @param end the exclusive end offset for the emoji in the {@code charSequence}
         * @param sdkAdded the API version that the emoji was added in AOSP
         *
         * @return true if the given sequence can be rendered as a single glyph, otherwise false.
         */
        boolean hasGlyph(
                @NonNull CharSequence charSequence,
                @IntRange(from = 0) int start,
                @IntRange(from = 0) int end,
                @IntRange(from = 0) int sdkAdded
        );
    }

    /**
     * Callback to inform EmojiCompat about the state of the metadata load. Passed to
     * MetadataRepoLoader during {@link MetadataRepoLoader#load(MetadataRepoLoaderCallback)} call.
     */
    public abstract static class MetadataRepoLoaderCallback {
        /**
         * Called by {@link MetadataRepoLoader} when metadata is loaded successfully.
         *
         * @param metadataRepo MetadataRepo instance, cannot be {@code null}
         */
        public abstract void onLoaded(@NonNull MetadataRepo metadataRepo);

        /**
         * Called by {@link MetadataRepoLoader} if an error occurs while loading the metadata.
         *
         * @param throwable the exception that caused the failure, {@code nullable}
         */
        public abstract void onFailed(@Nullable Throwable throwable);
    }

    /**
     * Configuration class for EmojiCompat. Changes to the values will be ignored after
     * {@link #init(Config)} is called.
     *
     * @see #init(EmojiCompat.Config)
     */
    public abstract static class Config {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull
        final MetadataRepoLoader mMetadataLoader;

        /**
         * Used to create new EmojiSpans.
         *
         * May be set by developer using config to fully customize emoji display.
         */
        SpanFactory mSpanFactory;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        boolean mReplaceAll;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        boolean mUseEmojiAsDefaultStyle;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Nullable
        int[] mEmojiAsDefaultStyleExceptions;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @Nullable
        Set<InitCallback> mInitCallbacks;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        boolean mEmojiSpanIndicatorEnabled;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        int mEmojiSpanIndicatorColor = Color.GREEN;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @LoadStrategy int mMetadataLoadStrategy = LOAD_STRATEGY_DEFAULT;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull
        GlyphChecker mGlyphChecker = new DefaultGlyphChecker();

        /**
         * Default constructor.
         *
         * @param metadataLoader MetadataRepoLoader instance, cannot be {@code null}
         */
        protected Config(@NonNull final MetadataRepoLoader metadataLoader) {
            Preconditions.checkNotNull(metadataLoader, "metadataLoader cannot be null.");
            mMetadataLoader = metadataLoader;
        }

        /**
         * Registers an initialization callback.
         *
         * @param initCallback the initialization callback to register, cannot be {@code null}
         *
         * @return EmojiCompat.Config instance
         */
        @SuppressWarnings("ExecutorRegistration")
        @NonNull
        public Config registerInitCallback(@NonNull InitCallback initCallback) {
            Preconditions.checkNotNull(initCallback, "initCallback cannot be null");
            if (mInitCallbacks == null) {
                mInitCallbacks = new ArraySet<>();
            }

            mInitCallbacks.add(initCallback);

            return this;
        }

        /**
         * Unregisters a callback that was added before.
         *
         * @param initCallback the initialization callback to be removed, cannot be {@code null}
         *
         * @return EmojiCompat.Config instance
         */
        @NonNull
        public Config unregisterInitCallback(@NonNull InitCallback initCallback) {
            Preconditions.checkNotNull(initCallback, "initCallback cannot be null");
            if (mInitCallbacks != null) {
                mInitCallbacks.remove(initCallback);
            }
            return this;
        }

        /**
         * Determines whether EmojiCompat should replace all the emojis it finds with the
         * EmojiSpans. By default EmojiCompat tries its best to understand if the system already
         * can render an emoji and do not replace those emojis.
         *
         * @param replaceAll replace all emojis found with EmojiSpans
         *
         * @return EmojiCompat.Config instance
         */
        @NonNull
        public Config setReplaceAll(final boolean replaceAll) {
            mReplaceAll = replaceAll;
            return this;
        }

        /**
         * Determines whether EmojiCompat should use the emoji presentation style for emojis
         * that have text style as default. By default, the text style would be used, unless these
         * are followed by the U+FE0F variation selector.
         * Details about emoji presentation and text presentation styles can be found here:
         * http://unicode.org/reports/tr51/#Presentation_Style
         * If useEmojiAsDefaultStyle is true, the emoji presentation style will be used for all
         * emojis, including potentially unexpected ones (such as digits or other keycap emojis). If
         * this is not the expected behaviour, method
         * {@link #setUseEmojiAsDefaultStyle(boolean, List)} can be used to specify the
         * exception emojis that should be still presented as text style.
         *
         * @param useEmojiAsDefaultStyle whether to use the emoji style presentation for all emojis
         *                               that would be presented as text style by default
         */
        @NonNull
        public Config setUseEmojiAsDefaultStyle(final boolean useEmojiAsDefaultStyle) {
            return setUseEmojiAsDefaultStyle(useEmojiAsDefaultStyle,
                    null);
        }

        /**
         * @see #setUseEmojiAsDefaultStyle(boolean)
         *
         * @param emojiAsDefaultStyleExceptions Contains the exception emojis which will be still
         *                                      presented as text style even if the
         *                                      useEmojiAsDefaultStyle flag is set to {@code true}.
         *                                      This list will be ignored if useEmojiAsDefaultStyle
         *                                      is {@code false}. Note that emojis with default
         *                                      emoji style presentation will remain emoji style
         *                                      regardless the value of useEmojiAsDefaultStyle or
         *                                      whether they are included in the exceptions list or
         *                                      not. When no exception is wanted, the method
         *                                      {@link #setUseEmojiAsDefaultStyle(boolean)} should
         *                                      be used instead.
         */
        @NonNull
        public Config setUseEmojiAsDefaultStyle(final boolean useEmojiAsDefaultStyle,
                @Nullable final List<Integer> emojiAsDefaultStyleExceptions) {
            mUseEmojiAsDefaultStyle = useEmojiAsDefaultStyle;
            if (mUseEmojiAsDefaultStyle && emojiAsDefaultStyleExceptions != null) {
                mEmojiAsDefaultStyleExceptions = new int[emojiAsDefaultStyleExceptions.size()];
                int i = 0;
                for (Integer exception : emojiAsDefaultStyleExceptions) {
                    mEmojiAsDefaultStyleExceptions[i++] = exception;
                }
                Arrays.sort(mEmojiAsDefaultStyleExceptions);
            } else {
                mEmojiAsDefaultStyleExceptions = null;
            }
            return this;
        }

        /**
         * Determines whether a background will be drawn for the emojis that are found and
         * replaced by EmojiCompat. Should be used only for debugging purposes. The indicator color
         * can be set using {@link #setEmojiSpanIndicatorColor(int)}.
         *
         * @param emojiSpanIndicatorEnabled when {@code true} a background is drawn for each emoji
         *                                  that is replaced
         */
        @NonNull
        public Config setEmojiSpanIndicatorEnabled(boolean emojiSpanIndicatorEnabled) {
            mEmojiSpanIndicatorEnabled = emojiSpanIndicatorEnabled;
            return this;
        }

        /**
         * Sets the color used as emoji span indicator. The default value is
         * {@link Color#GREEN Color.GREEN}.
         *
         * @see #setEmojiSpanIndicatorEnabled(boolean)
         */
        @NonNull
        public Config setEmojiSpanIndicatorColor(@ColorInt int color) {
            mEmojiSpanIndicatorColor = color;
            return this;
        }

        /**
         * Determines the strategy to start loading the metadata. By default {@link EmojiCompat}
         * will start loading the metadata during {@link EmojiCompat#init(Config)}. When set to
         * {@link EmojiCompat#LOAD_STRATEGY_MANUAL}, you should call {@link EmojiCompat#load()} to
         * initiate metadata loading.
         * <p/>
         * Default implementations of {@link EmojiCompat.MetadataRepoLoader} start a thread
         * during their {@link EmojiCompat.MetadataRepoLoader#load} functions. Just instantiating
         * and starting a thread might take time especially in older devices. Since
         * {@link EmojiCompat#init(Config)} has to be called before any EmojiCompat widgets are
         * inflated, this results in time spent either on your Application.onCreate or Activity
         * .onCreate. If you'd like to gain more control on when to start loading the metadata
         * and be able to call {@link EmojiCompat#init(Config)} with absolute minimum time cost you
         * can use {@link EmojiCompat#LOAD_STRATEGY_MANUAL}.
         * <p/>
         * When set to {@link EmojiCompat#LOAD_STRATEGY_MANUAL}, {@link EmojiCompat} will wait
         * for {@link #load()} to be called by the developer in order to start loading metadata,
         * therefore you should call {@link EmojiCompat#load()} to initiate metadata loading.
         * {@link #load()} can be called from any thread.
         * <pre>
         * EmojiCompat.Config config = new FontRequestEmojiCompatConfig(context, fontRequest)
         *         .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);
         *
         * // EmojiCompat will not start loading metadata and MetadataRepoLoader#load(...)
         * // will not be called
         * EmojiCompat.init(config);
         *
         * // At any time (i.e. idle time or executorService is ready)
         * // call EmojiCompat#load() to start loading metadata.
         * executorService.execute(() -> EmojiCompat.get().load());
         * </pre>
         *
         * @param strategy one of {@link EmojiCompat#LOAD_STRATEGY_DEFAULT},
         *                  {@link EmojiCompat#LOAD_STRATEGY_MANUAL}
         *
         */
        @NonNull
        public Config setMetadataLoadStrategy(@LoadStrategy int strategy) {
            mMetadataLoadStrategy = strategy;
            return this;
        }

        /**
         * Set the span factory used to actually draw emoji replacements.
         *
         * @param factory custum span factory that can draw the emoji replacements
         * @return this
         */
        @NonNull
        public Config setSpanFactory(@NonNull SpanFactory factory) {
            mSpanFactory = factory;
            return this;
        }

        /**
         * The interface that is used by EmojiCompat in order to check if a given emoji can be
         * rendered by the system.
         *
         * @param glyphChecker {@link GlyphChecker} instance to be used.
         */
        @NonNull
        public Config setGlyphChecker(@NonNull GlyphChecker glyphChecker) {
            Preconditions.checkNotNull(glyphChecker, "GlyphChecker cannot be null");
            mGlyphChecker = glyphChecker;
            return this;
        }

        /**
         * Returns the {@link MetadataRepoLoader}.
         */
        @NonNull
        protected final MetadataRepoLoader getMetadataRepoLoader() {
            return mMetadataLoader;
        }
    }

    /**
     * Runnable to call success/failure case for the listeners.
     */
    private static class ListenerDispatcher implements Runnable {
        private final List<InitCallback> mInitCallbacks;
        private final Throwable mThrowable;
        private final int mLoadState;

        @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
        ListenerDispatcher(@NonNull final InitCallback initCallback,
                @LoadState final int loadState) {
            this(Arrays.asList(Preconditions.checkNotNull(initCallback,
                    "initCallback cannot be null")), loadState, null);
        }

        ListenerDispatcher(@NonNull final Collection<InitCallback> initCallbacks,
                @LoadState final int loadState) {
            this(initCallbacks, loadState, null);
        }

        ListenerDispatcher(@NonNull final Collection<InitCallback> initCallbacks,
                @LoadState final int loadState,
                @Nullable final Throwable throwable) {
            Preconditions.checkNotNull(initCallbacks, "initCallbacks cannot be null");
            mInitCallbacks = new ArrayList<>(initCallbacks);
            mLoadState = loadState;
            mThrowable = throwable;
        }

        @Override
        public void run() {
            final int size = mInitCallbacks.size();
            switch (mLoadState) {
                case LOAD_STATE_SUCCEEDED:
                    for (int i = 0; i < size; i++) {
                        mInitCallbacks.get(i).onInitialized();
                    }
                    break;
                case LOAD_STATE_FAILED:
                default:
                    for (int i = 0; i < size; i++) {
                        mInitCallbacks.get(i).onFailed(mThrowable);
                    }
                    break;
            }
        }
    }

    /**
     * Internal helper class to behave no-op for certain functions.
     */
    private static class CompatInternal {
        final EmojiCompat mEmojiCompat;

        CompatInternal(EmojiCompat emojiCompat) {
            mEmojiCompat = emojiCompat;
        }

        void loadMetadata() {
            // Moves into LOAD_STATE_SUCCESS state immediately.
            mEmojiCompat.onMetadataLoadSuccess();
        }

        boolean hasEmojiGlyph(@NonNull final CharSequence sequence) {
            // Since no metadata is loaded, EmojiCompat cannot detect or render any emojis.
            return false;
        }

        boolean hasEmojiGlyph(@NonNull final CharSequence sequence, final int metadataVersion) {
            // Since no metadata is loaded, EmojiCompat cannot detect or render any emojis.
            return false;
        }

        int getEmojiStart(@NonNull final CharSequence cs, @IntRange(from = 0) final int offset) {
            // Since no metadata is loaded, EmojiCompat cannot detect any emojis.
            return -1;
        }

        int getEmojiEnd(@NonNull final CharSequence cs, @IntRange(from = 0) final int offset) {
            // Since no metadata is loaded, EmojiCompat cannot detect any emojis.
            return -1;
        }

        CharSequence process(@NonNull final CharSequence charSequence,
                @IntRange(from = 0) final int start, @IntRange(from = 0) final int end,
                @IntRange(from = 0) final int maxEmojiCount, boolean replaceAll) {
            // Returns the given charSequence as it is.
            return charSequence;
        }

        void updateEditorInfoAttrs(@NonNull final EditorInfo outAttrs) {
            // Does not add any EditorInfo attributes.
        }

        String getAssetSignature() {
            return "";
        }

        @CodepointSequenceMatchResult
        public int getEmojiMatch(CharSequence sequence, int metadataVersion) {
            return EMOJI_UNSUPPORTED;
        }
    }

    @RequiresApi(19)
    private static final class CompatInternal19 extends CompatInternal {
        /**
         * Responsible to process a CharSequence and add the spans. @{code Null} until the time the
         * metadata is loaded.
         */
        private volatile EmojiProcessor mProcessor;

        /**
         * Keeps the information about emojis. Null until the time the data is loaded.
         */
        private volatile MetadataRepo mMetadataRepo;


        CompatInternal19(EmojiCompat emojiCompat) {
            super(emojiCompat);
        }

        @Override
        void loadMetadata() {
            try {
                final MetadataRepoLoaderCallback callback = new MetadataRepoLoaderCallback() {
                    @Override
                    public void onLoaded(@NonNull MetadataRepo metadataRepo) {
                        onMetadataLoadSuccess(metadataRepo);
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        mEmojiCompat.onMetadataLoadFailed(throwable);
                    }
                };
                mEmojiCompat.mMetadataLoader.load(callback);
            } catch (Throwable t) {
                mEmojiCompat.onMetadataLoadFailed(t);
            }
        }

        @SuppressWarnings("SyntheticAccessor")
        void onMetadataLoadSuccess(@NonNull final MetadataRepo metadataRepo) {
            //noinspection ConstantConditions
            if (metadataRepo == null) {
                mEmojiCompat.onMetadataLoadFailed(
                        new IllegalArgumentException("metadataRepo cannot be null"));
                return;
            }

            mMetadataRepo = metadataRepo;
            mProcessor = new EmojiProcessor(
                    mMetadataRepo,
                    mEmojiCompat.mSpanFactory,
                    mEmojiCompat.mGlyphChecker,
                    mEmojiCompat.mUseEmojiAsDefaultStyle,
                    mEmojiCompat.mEmojiAsDefaultStyleExceptions,
                    EmojiExclusions.getEmojiExclusions()
            );

            mEmojiCompat.onMetadataLoadSuccess();
        }

        @Override
        boolean hasEmojiGlyph(@NonNull CharSequence sequence) {
            return mProcessor.getEmojiMatch(sequence) == EMOJI_SUPPORTED;
        }

        @Override
        boolean hasEmojiGlyph(@NonNull CharSequence sequence, int metadataVersion) {
            int emojiMatch = mProcessor.getEmojiMatch(sequence, metadataVersion);
            return emojiMatch == EMOJI_SUPPORTED;
        }

        @Override
        public int getEmojiMatch(CharSequence sequence, int metadataVersion) {
            return mProcessor.getEmojiMatch(sequence, metadataVersion);
        }

        @Override
        int getEmojiStart(@NonNull final CharSequence sequence, final int offset) {
            return mProcessor.getEmojiStart(sequence, offset);
        }

        @Override
        int getEmojiEnd(@NonNull final CharSequence sequence, final int offset) {
            return mProcessor.getEmojiEnd(sequence, offset);
        }

        @Override
        CharSequence process(@NonNull CharSequence charSequence, int start, int end,
                int maxEmojiCount, boolean replaceAll) {
            return mProcessor.process(charSequence, start, end, maxEmojiCount, replaceAll);
        }

        @Override
        void updateEditorInfoAttrs(@NonNull EditorInfo outAttrs) {
            outAttrs.extras.putInt(EDITOR_INFO_METAVERSION_KEY, mMetadataRepo.getMetadataVersion());
            outAttrs.extras.putBoolean(EDITOR_INFO_REPLACE_ALL_KEY, mEmojiCompat.mReplaceAll);
        }

        @Override
        String getAssetSignature() {
            final String sha = mMetadataRepo.getMetadataList().sourceSha();
            return sha == null ? "" : sha;
        }
    }
}
