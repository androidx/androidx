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

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.GuardedBy;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    private static final Object sInstanceLock = new Object();

    @GuardedBy("sInstanceLock")
    private static volatile EmojiCompat sInstance;

    private final ReadWriteLock mInitLock;

    @GuardedBy("mInitLock")
    private final Set<InitCallback> mInitCallbacks;

    @GuardedBy("mInitLock")
    @LoadState
    private int mLoadState;

    private final Config mConfig;
    private final Handler mMainHandler;

    /**
     * Responsible to process a CharSequence and add the spans. @{code Null} until the time the
     * metadata is loaded.
     */
    private EmojiProcessor mProcessor;

    /**
     * Keeps the information about emojis. Null until the time the data is loaded.
     */
    private MetadataRepo mMetadataRepo;

    private static final int LOAD_STATE_LOADING = 0;
    private static final int LOAD_STATE_SUCCESS = 1;
    private static final int LOAD_STATE_FAIL = 2;

    @IntDef({LOAD_STATE_LOADING, LOAD_STATE_SUCCESS, LOAD_STATE_FAIL})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LoadState {
    }

    /**
     * Private constructor for singleton instance.
     *
     * @see #init(Config)
     */
    private EmojiCompat(@NonNull final Config config) {
        mInitLock = new ReentrantReadWriteLock();
        mConfig = config;
        mMainHandler = new Handler(Looper.getMainLooper());
        mInitCallbacks = new ArraySet<>();
        if (mConfig.mInitCallbacks != null && !mConfig.mInitCallbacks.isEmpty()) {
            mInitCallbacks.addAll(mConfig.mInitCallbacks);
        }
        loadMetadata();
    }

    /**
     * Initialize the singleton instance with a configuration.
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

        try {
            mConfig.mMetadataLoader.load(new LoaderCallback() {
                @Override
                public void onLoaded(@NonNull MetadataRepo metadataRepo) {
                    onMetadataLoadSuccess(metadataRepo);
                }

                @Override
                public void onFailed(@Nullable Throwable throwable) {
                    onMetadataLoadFailed(throwable);
                }
            });
        } catch (Throwable t) {
            onMetadataLoadFailed(t);
        }
    }

    private void onMetadataLoadSuccess(@NonNull final MetadataRepo metadataRepo) {
        if (metadataRepo == null) {
            onMetadataLoadFailed(new IllegalArgumentException("metadataRepo cannot be null"));
            return;
        }

        mMetadataRepo = metadataRepo;
        mProcessor = new EmojiProcessor(mMetadataRepo, new SpanFactory(),
                mConfig.mReplaceAll, mConfig.mMaxEmojiPerText);

        final Collection<InitCallback> initCallbacks = new ArrayList<>();
        mInitLock.writeLock().lock();
        try {
            mLoadState = LOAD_STATE_SUCCESS;
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
            mLoadState = LOAD_STATE_FAIL;
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
     *
     * @param initCallback the initialization callback to register, cannot be {@code null}
     *
     * @see #unregisterInitCallback(InitCallback)
     */
    public void registerInitCallback(@NonNull InitCallback initCallback) {
        Preconditions.checkNotNull(initCallback, "initCallback cannot be null");

        mInitLock.writeLock().lock();
        try {
            if (mLoadState == LOAD_STATE_SUCCESS || mLoadState == LOAD_STATE_FAIL) {
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
     * @return {@code true} if EmojiCompat is successfully initialized
     */
    public boolean isInitialized() {
        mInitLock.readLock().lock();
        try {
            return mLoadState == LOAD_STATE_SUCCESS;
        } finally {
            mInitLock.readLock().unlock();
        }
    }

    /**
     * Handles onKeyDown commands from a {@link KeyListener} and if {@code keyCode} is one of
     * {@link KeyEvent#KEYCODE_DEL} or {@link KeyEvent#KEYCODE_FORWARD_DEL} it tries to delete an
     * {@link EmojiSpan} from an {@link Editable}. Returns {@code true} if an {@link EmojiSpan} is
     * deleted with the characters it covers.
     * <p/>
     * If there is a selection where selection start is not equal to selection end, does not
     * delete.
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
        return EmojiProcessor.handleOnKeyDown(editable, keyCode, event);
    }

    /**
     * Handles deleteSurroundingText commands from {@link InputConnection} and tries to delete an
     * {@link EmojiSpan} from an {@link Editable}. Returns {@code true} if an {@link EmojiSpan} is
     * deleted.
     * <p/>
     * If there is a selection where selection start is not equal to selection end, does not
     * delete.
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
        return EmojiProcessor.handleDeleteSurroundingText(inputConnection, editable, beforeLength,
                afterLength, inCodePoints);
    }

    /**
     * Returns {@code true} if EmojiCompat is capable of rendering an emoji.
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
        return mProcessor.getEmojiMetadata(sequence) != null;
    }

    /**
     * Returns {@code true} if EmojiCompat is capable of rendering an emoji at the given metadata
     * version.
     *
     * @param sequence CharSequence representing the emoji
     * @param metadataVersion the metadata version to check against
     *
     * @return {@code true} if EmojiCompat can render given emoji, cannot be {@code null}
     *
     * @throws IllegalStateException if not initialized yet
     */
    public boolean hasEmojiGlyph(@NonNull final CharSequence sequence, final int metadataVersion) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        Preconditions.checkNotNull(sequence, "sequence cannot be null");
        final EmojiMetadata emojiMetadata = mProcessor.getEmojiMetadata(sequence);
        return emojiMetadata != null && emojiMetadata.getCompatAdded() <= metadataVersion;
    }

    /**
     * Checks a given CharSequence for emojis, and adds EmojiSpans if any emojis are found.
     *
     * @param charSequence CharSequence to add the EmojiSpans
     *
     * @throws IllegalStateException if not initialized yet
     * @see #process(CharSequence, int, int)
     */
    public CharSequence process(@NonNull final CharSequence charSequence) {
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
     *
     * @param charSequence CharSequence to add the EmojiSpans, cannot be {@code null}
     * @param start start index in the charSequence to look for emojis, should be greater than or
     *              equal to {@code 0}, also less than {@code charSequence.length()}
     * @param end end index in the charSequence to look for emojis, should be greater than or
     *            equal to {@code start} parameter, also less than {@code charSequence.length()}
     *
     * @throws IllegalStateException if not initialized yet
     */
    public CharSequence process(@NonNull final CharSequence charSequence,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end) {
        Preconditions.checkState(isInitialized(), "Not initialized yet");
        return mProcessor.process(charSequence, start, end);
    }

    /**
     * Returns the Typeface instance that is created using the emoji font.
     *
     * @return {@link Typeface} instance that is created using the emoji font
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Typeface getTypeface() {
        if (mMetadataRepo != null) {
            return mMetadataRepo.getTypeface();
        }
        return null;
    }

    /**
     * Updates the EditorInfo attributes in order to communicate information to Keyboards.
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
        if (isInitialized() && outAttrs != null && outAttrs.extras != null) {
            outAttrs.extras.putInt(EDITOR_INFO_METAVERSION_KEY, mMetadataRepo.getMetadataVersion());
            outAttrs.extras.putBoolean(EDITOR_INFO_REPLACE_ALL_KEY, mConfig.mReplaceAll);
        }
    }

    /**
     * Factory class that creates the EmojiSpans. By default it creates {@link TypefaceEmojiSpan}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
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
         * Called when EmojiCompat is initialized and the emoji data is loaded.
         */
        public void onInitialized() {
        }

        /**
         * Called when an unrecoverable error occurs during EmojiCompat initialization.
         */
        public void onFailed(@Nullable Throwable throwable) {
        }
    }

    /**
     * Interface to load emoji metadata.
     */
    public interface MetadataLoader {
        /**
         * Start loading the metadata. When the loading operation is finished {@link
         * LoaderCallback#onLoaded(MetadataRepo)} or
         * {@link LoaderCallback#onFailed(Throwable)}
         * should be called.
         *
         * @param loaderCallback callback to signal the loading state
         */
        void load(@NonNull LoaderCallback loaderCallback);
    }

    /**
     * Callback to inform EmojiCompat about the state of the metadata load. Passed to MetadataLoader
     * during {@link MetadataLoader#load(LoaderCallback)} call.
     */
    public abstract static class LoaderCallback {
        /**
         * Called by {@link MetadataLoader} when metadata is loaded successfully.
         *
         * @param metadataRepo MetadataRepo instance, cannot be {@code null}
         */
        public abstract void onLoaded(@NonNull MetadataRepo metadataRepo);

        /**
         * Called by {@link MetadataLoader} if an error occurs while loading the metadata.
         *
         * @param throwable the exception that caused the failure, {@code nullable}
         */
        public abstract void onFailed(@Nullable Throwable throwable);
    }

    /**
     * Configuration class for EmojiCompat.
     *
     * @see #init(EmojiCompat.Config)
     */
    public abstract static class Config {
        private final MetadataLoader mMetadataLoader;
        /**
         * Measurements on Pixel XL, Android N MR2, EditText delete operation takes 7ms for
         * 100 EmojiSpans.
         */
        private int mMaxEmojiPerText = 100;
        private boolean mReplaceAll;
        private Set<InitCallback> mInitCallbacks;

        /**
         * Default constructor.
         *
         * @param metadataLoader MetadataLoader instance, cannot be {@code null}
         */
        protected Config(@NonNull final MetadataLoader metadataLoader) {
            Preconditions.checkNotNull(metadataLoader, "metadataLoader cannot be null.");
            mMetadataLoader = metadataLoader;
        }

        /**
         * Set the limit of EmojiSpans to be added to a CharSequence. The number of spans in a
         * CharSequence affects the performance of the EditText, TextView.
         * <p/>
         * Default value is {@code 100}.
         *
         * @param maxEmojiPerText maximum number of EmojiSpans to be added to a single
         *                        CharSequence, should be equal or greater than 0
         *
         * @return EmojiCompat.Config instance
         */
        public Config setMaxEmojiPerText(@IntRange(from = 0) final int maxEmojiPerText) {
            Preconditions.checkArgumentNonnegative(maxEmojiPerText,
                    "maxEmojiPerText cannot be negative");
            mMaxEmojiPerText = maxEmojiPerText;
            return this;
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
    }

    /**
     * Runnable to call success/failure case for the listeners.
     */
    private static class ListenerDispatcher implements Runnable {
        private final List<InitCallback> mInitCallbacks;
        private final Throwable mThrowable;
        private final int mLoadState;

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
                case LOAD_STATE_SUCCESS:
                    for (int i = 0; i < size; i++) {
                        mInitCallbacks.get(i).onInitialized();
                    }
                    break;
                case LOAD_STATE_FAIL:
                default:
                    for (int i = 0; i < size; i++) {
                        mInitCallbacks.get(i).onFailed(mThrowable);
                    }
                    break;
            }
        }
    }
}
