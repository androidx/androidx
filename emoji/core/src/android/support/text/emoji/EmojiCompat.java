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
package android.support.text.emoji;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.GuardedBy;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;
import android.support.v4.util.Preconditions;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

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
 * to a given {@link CharSequence}. It is a singleton class that can be configured using a {@link
 * EmojiCompat.Config} instance.
 * <p/>
 * EmojiCompat has to be initialized using {@link #init(EmojiCompat.Config)} function before it can
 * process a {@link CharSequence}.
 * <pre><code>EmojiCompat.init(&#47;* a config instance *&#47;);</code></pre>
 * <p/>
 * It is suggested to make the initialization as early as possible in your app. Please check {@link
 * EmojiCompat.Config} for more configuration parameters.
 * <p/>
 * During initialization information about emojis is loaded on a background thread. Before the
 * EmojiCompat instance is initialized, calls to functions such as {@link
 * EmojiCompat#process(CharSequence)} will throw an exception. You can use the {@link InitCallback}
 * class to be informed about the state of initialization.
 * <p/>
 * After initialization the {@link #get()} function can be used to get the configured instance and
 * the {@link #process(CharSequence)} function can be used to update a CharSequence with emoji
 * EmojiSpans.
 * <p/>
 * <pre><code>CharSequence processedSequence = EmojiCompat.get().process("some string")</pre>
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
     * EmojiCompat is initializing.
     */
    public static final int LOAD_STATE_LOADING = 0;

    /**
     * EmojiCompat successfully initialized.
     */
    public static final int LOAD_STATE_SUCCEEDED = 1;

    /**
     * An unrecoverable error occurred during initialization of EmojiCompat. Calls to functions
     * such as {@link #process(CharSequence)} will fail.
     */
    public static final int LOAD_STATE_FAILED = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({LOAD_STATE_LOADING, LOAD_STATE_SUCCEEDED, LOAD_STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadState {
    }

    /**
     * Replace strategy that uses the value given in {@link EmojiCompat.Config}.
     */
    public static final int REPLACE_STRATEGY_DEFAULT = 0;

    /**
     * Replace strategy to add {@link EmojiSpan}s for all emoji that were found.
     */
    public static final int REPLACE_STRATEGY_ALL = 1;

    /**
     * Replace strategy to add {@link EmojiSpan}s only for emoji that do not exist in the system.
     */
    public static final int REPLACE_STRATEGY_NON_EXISTENT = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({REPLACE_STRATEGY_DEFAULT, REPLACE_STRATEGY_NON_EXISTENT, REPLACE_STRATEGY_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReplaceStrategy {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static final int EMOJI_COUNT_UNLIMITED = Integer.MAX_VALUE;

    private static final Object sInstanceLock = new Object();

    @GuardedBy("sInstanceLock")
    private static volatile EmojiCompat sInstance;

    private final ReadWriteLock mInitLock;

    @GuardedBy("mInitLock")
    private final Set<InitCallback> mInitCallbacks;

    @GuardedBy("mInitLock")
    @LoadState
    private int mLoadState;

    /**
     * Handler with main looper to run the callbacks on.
     */
    private final Handler mMainHandler;

    /**
     * Helper class for pre 19 compatibility.
     */
    private final CompatInternal mHelper;

    /**
     * Metadata loader instance given in the Config instance.
     */
    private final MetadataRepoLoader mMetadataLoader;

    /**
     * @see Config#setReplaceAll(boolean)
     */
    private final boolean mReplaceAll;

    /**
     * @see Config#setEmojiSpanIndicatorEnabled(boolean)
     */
    private final boolean mEmojiSpanIndicatorEnabled;

    /**
     * @see Config#setEmojiSpanIndicatorColor(int)
     */
    private final int mEmojiSpanIndicatorColor;

    /**
     * Private constructor for singleton instance.
     *
     * @see #init(Config)
     */
    private EmojiCompat(@NonNull final Config config) {
        mInitLock = new ReentrantReadWriteLock();
        mReplaceAll = config.mReplaceAll;
        mEmojiSpanIndicatorEnabled = config.mEmojiSpanIndicatorEnabled;
        mEmojiSpanIndicatorColor = config.mEmojiSpanIndicatorColor;
        mMetadataLoader = config.mMetadataLoader;
        mMainHandler = new Handler(Looper.getMainLooper());
        mInitCallbacks = new ArraySet<>();
        if (config.mInitCallbacks != null && !config.mInitCallbacks.isEmpty()) {
            mInitCallbacks.addAll(config.mInitCallbacks);
        }
        mHelper = Build.VERSION.SDK_INT < 19 ? new CompatInternal(this) : new CompatInternal19(
                this);
        loadMetadata();
    }

    /**
     * Initialize the singleton instance with a configuration. When used on devices running API 18
     * or below, the singleton instance is immediately moved into {@link #LOAD_STATE_SUCCEEDED}
     * state without loading any metadata.
     *
     * @see EmojiCompat.Config
     */
    public static EmojiCompat init(@NonNull final Config config) {
        if (sInstance == null) {
            synchronized (sInstanceLock) {
                if (sInstance == null) {
                    sInstance = new EmojiCompat(config);
                }
            }
        }
        return sInstance;
    }

    /**
     * Used by the tests to reset EmojiCompat with a new configuration. Every time it is called a
     * new instance is created with the new configuration.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting
    public static EmojiCompat reset(@NonNull final Config config) {
        synchronized (sInstanceLock) {
            sInstance = new EmojiCompat(config);
        }
        return sInstance;
    }

    /**
     * Used by the tests to reset EmojiCompat with a new singleton instance.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting
    public static EmojiCompat reset(final EmojiCompat emojiCompat) {
        synchronized (sInstanceLock) {
            sInstance = emojiCompat;
        }
        return sInstance;
    }

    /**
     * Used by the tests to set GlyphChecker for EmojiProcessor.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting
    void setGlyphChecker(@NonNull final EmojiProcessor.GlyphChecker glyphChecker) {
        mHelper.setGlyphChecker(glyphChecker);
    }

    /**
     * Return singleton EmojiCompat instance. Should be called after
     * {@link #init(EmojiCompat.Config)} is called to initialize the singleton instance.
     *
     * @return EmojiCompat instance
     *
     * @throws IllegalStateException if called before {@link #init(EmojiCompat.Config)}
     */
    public static EmojiCompat get() {
        synchronized (sInstanceLock) {
            Preconditions.checkState(sInstance != null,
                    "EmojiCompat is not initialized. Please call EmojiCompat.init() first");
            return sInstance;
        }
    }

    private void loadMetadata() {
        mInitLock.writeLock().lock();
        try {
            mLoadState = LOAD_STATE_LOADING;
        } finally {
            mInitLock.writeLock().unlock();
        }

        mHelper.loadMetadata();
    }

    private void onMetadataLoadSuccess() {
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

    private void onMetadataLoadFailed(@Nullable final Throwable throwable) {
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
     * @return one of {@link #LOAD_STATE_LOADING}, {@link #LOAD_STATE_SUCCEEDED},
     * {@link #LOAD_STATE_FAILED}
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
     * @return whether a background should be drawn for the emoji.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    boolean isEmojiSpanIndicatorEnabled() {
        return mEmojiSpanIndicatorEnabled;
    }

    /**
     * @return whether a background should be drawn for the emoji.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ColorInt int getEmojiSpanIndicatorColor() {
        return mEmojiSpanIndicatorColor;
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
            final KeyEvent event) {
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
     * @param sequence CharSequence representing the emoji
     *
     * @return {@code true} if EmojiCompat can render given emoji, cannot be {@code null}
     *
     * @throws IllegalStateException if not initialized yet
     */
    public boolean hasEmojiGlyph(@NonNull final CharSequence sequence) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        return mHelper.hasEmojiGlyph(sequence);
    }

    /**
     * Returns {@code true} if EmojiCompat is capable of rendering an emoji at the given metadata
     * version. When used on devices running API 18 or below, always returns {@code false}.
     *
     * @param sequence CharSequence representing the emoji
     * @param metadataVersion the metadata version to check against, should be greater than or
     *                        equal to {@code 0},
     *
     * @return {@code true} if EmojiCompat can render given emoji, cannot be {@code null}
     *
     * @throws IllegalStateException if not initialized yet
     */
    public boolean hasEmojiGlyph(@NonNull final CharSequence sequence,
            @IntRange(from = 0) final int metadataVersion) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        return mHelper.hasEmojiGlyph(sequence, metadataVersion);
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
    @CheckResult
    public CharSequence process(@NonNull final CharSequence charSequence) {
        // since charSequence might be null here we have to check it. Passing through here to the
        // main function so that it can do all the checks including isInitialized. It will also
        // be the main point that decides what to return.
        //noinspection ConstantConditions
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
     *              equal to {@code 0}, also less than {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than {@code charSequence.length()}
     *
     * @throws IllegalStateException if not initialized yet
     * @throws IllegalArgumentException in the following cases:
     *                                  {@code start < 0}, {@code end < 0}, {@code end < start},
     *                                  {@code start > charSequence.length()},
     *                                  {@code end > charSequence.length()}
     */
    @CheckResult
    public CharSequence process(@NonNull final CharSequence charSequence,
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
     *              equal to {@code 0}, also less than {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than {@code charSequence.length()}
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
    @CheckResult
    public CharSequence process(@NonNull final CharSequence charSequence,
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
     *              equal to {@code 0}, also less than {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than {@code charSequence.length()}
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
    @CheckResult
    public CharSequence process(@NonNull final CharSequence charSequence,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end,
            @IntRange(from = 0) final int maxEmojiCount, @ReplaceStrategy int replaceStrategy) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkArgumentNonnegative(start, "start cannot be negative");
        Preconditions.checkArgumentNonnegative(end, "end cannot be negative");
        Preconditions.checkArgumentNonnegative(maxEmojiCount, "maxEmojiCount cannot be negative");
        Preconditions.checkArgument(start <= end, "start should be <= than end");

        // early return since there is nothing to do
        //noinspection ConstantConditions
        if (charSequence == null) {
            return charSequence;
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
     * @param outAttrs EditorInfo instance passed to
     *                 {@link android.widget.TextView#onCreateInputConnection(EditorInfo)}
     *
     * @see #EDITOR_INFO_METAVERSION_KEY
     * @see #EDITOR_INFO_REPLACE_ALL_KEY
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void updateEditorInfoAttrs(@NonNull final EditorInfo outAttrs) {
        //noinspection ConstantConditions
        if (isInitialized() && outAttrs != null && outAttrs.extras != null) {
            mHelper.updateEditorInfoAttrs(outAttrs);
        }
    }

    /**
     * Factory class that creates the EmojiSpans. By default it creates {@link TypefaceEmojiSpan}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @RequiresApi(19)
    static class SpanFactory {
        /**
         * Create EmojiSpan instance.
         *
         * @param metadata EmojiMetadata instance
         *
         * @return EmojiSpan instance
         */
        EmojiSpan createSpan(@NonNull final EmojiMetadata metadata) {
            return new TypefaceEmojiSpan(metadata);
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
        public void onFailed(@Nullable Throwable throwable) {
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
        void load(@NonNull MetadataRepoLoaderCallback loaderCallback);
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
        private final MetadataRepoLoader mMetadataLoader;
        private boolean mReplaceAll;
        private Set<InitCallback> mInitCallbacks;
        private boolean mEmojiSpanIndicatorEnabled;
        private int mEmojiSpanIndicatorColor = Color.GREEN;

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
        public Config setReplaceAll(final boolean replaceAll) {
            mReplaceAll = replaceAll;
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
        public Config setEmojiSpanIndicatorColor(@ColorInt int color) {
            mEmojiSpanIndicatorColor = color;
            return this;
        }

        /**
         * Returns the {@link MetadataRepoLoader}.
         */
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

        CharSequence process(@NonNull final CharSequence charSequence,
                @IntRange(from = 0) final int start, @IntRange(from = 0) final int end,
                @IntRange(from = 0) final int maxEmojiCount, boolean replaceAll) {
            // Returns the given charSequence as it is.
            return charSequence;
        }

        void updateEditorInfoAttrs(@NonNull final EditorInfo outAttrs) {
            // Does not add any EditorInfo attributes.
        }

        void setGlyphChecker(@NonNull EmojiProcessor.GlyphChecker glyphChecker) {
            // intentionally empty
        }

        String getAssetSignature() {
            return "";
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

        private void onMetadataLoadSuccess(@NonNull final MetadataRepo metadataRepo) {
            //noinspection ConstantConditions
            if (metadataRepo == null) {
                mEmojiCompat.onMetadataLoadFailed(
                        new IllegalArgumentException("metadataRepo cannot be null"));
                return;
            }

            mMetadataRepo = metadataRepo;
            mProcessor = new EmojiProcessor(mMetadataRepo, new SpanFactory());

            mEmojiCompat.onMetadataLoadSuccess();
        }

        @Override
        boolean hasEmojiGlyph(@NonNull CharSequence sequence) {
            return mProcessor.getEmojiMetadata(sequence) != null;
        }

        @Override
        boolean hasEmojiGlyph(@NonNull CharSequence sequence, int metadataVersion) {
            final EmojiMetadata emojiMetadata = mProcessor.getEmojiMetadata(sequence);
            return emojiMetadata != null && emojiMetadata.getCompatAdded() <= metadataVersion;
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
        void setGlyphChecker(@NonNull EmojiProcessor.GlyphChecker glyphChecker) {
            mProcessor.setGlyphChecker(glyphChecker);
        }

        @Override
        String getAssetSignature() {
            final String sha = mMetadataRepo.getMetadataList().sourceSha();
            return sha == null ? "" : sha;
        }
    }
}
