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
package androidx.emoji.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.Editable;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.emoji.text.EmojiSpan;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * When setSpan functions is called on EmojiSpannableBuilder, it checks if the mObject is instance
 * of the DynamicLayout$ChangeWatcher. if so, it wraps it into another listener mObject
 * (WatcherWrapper) that implements the same interfaces.
 * <p>
 * During a span change event WatcherWrapper’s functions are fired, it checks if the span is an
 * EmojiSpan, and prevents the ChangeWatcher being fired for that span. WatcherWrapper informs
 * ChangeWatcher only once at the end of the edit. Important point is, the block operation is
 * applied only for EmojiSpans. Therefore any other span change operation works the same way as in
 * the framework.
 *
 * @hide
 * @see EmojiEditableFactory
 */
@RestrictTo(LIBRARY_GROUP)
public final class SpannableBuilder extends SpannableStringBuilder {
    /**
     * DynamicLayout$ChangeWatcher class.
     */
    private final Class<?> mWatcherClass;

    /**
     * All WatcherWrappers.
     */
    private final List<WatcherWrapper> mWatchers = new ArrayList<>();

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SpannableBuilder(@NonNull Class<?> watcherClass) {
        Preconditions.checkNotNull(watcherClass, "watcherClass cannot be null");
        mWatcherClass = watcherClass;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SpannableBuilder(@NonNull Class<?> watcherClass, @NonNull CharSequence text) {
        super(text);
        Preconditions.checkNotNull(watcherClass, "watcherClass cannot be null");
        mWatcherClass = watcherClass;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SpannableBuilder(@NonNull Class<?> watcherClass, @NonNull CharSequence text, int start,
            int end) {
        super(text, start, end);
        Preconditions.checkNotNull(watcherClass, "watcherClass cannot be null");
        mWatcherClass = watcherClass;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static SpannableBuilder create(@NonNull Class<?> clazz, @NonNull CharSequence text) {
        return new SpannableBuilder(clazz, text);
    }

    /**
     * Checks whether the mObject is instance of the DynamicLayout$ChangeWatcher.
     *
     * @param object mObject to be checked
     *
     * @return true if mObject is instance of the DynamicLayout$ChangeWatcher.
     */
    private boolean isWatcher(@Nullable Object object) {
        return object != null && isWatcher(object.getClass());
    }

    /**
     * Checks whether the class is DynamicLayout$ChangeWatcher.
     *
     * @param clazz class to be checked
     *
     * @return true if class is DynamicLayout$ChangeWatcher.
     */
    private boolean isWatcher(@NonNull Class<?> clazz) {
        return mWatcherClass == clazz;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableBuilder(mWatcherClass, this, start, end);
    }

    /**
     * If the span being added is instance of DynamicLayout$ChangeWatcher, wrap the watcher in
     * another internal watcher that will prevent EmojiSpan events to be fired to DynamicLayout. Set
     * this new mObject as the span.
     */
    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        if (isWatcher(what)) {
            final WatcherWrapper span = new WatcherWrapper(what);
            mWatchers.add(span);
            what = span;
        }
        super.setSpan(what, start, end, flags);
    }

    /**
     * If previously a DynamicLayout$ChangeWatcher was wrapped in a WatcherWrapper, return the
     * correct Object that the client has set.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
        if (isWatcher(kind)) {
            final WatcherWrapper[] spans = super.getSpans(queryStart, queryEnd,
                    WatcherWrapper.class);
            final T[] result = (T[]) Array.newInstance(kind, spans.length);
            for (int i = 0; i < spans.length; i++) {
                result[i] = (T) spans[i].mObject;
            }
            return result;
        }
        return super.getSpans(queryStart, queryEnd, kind);
    }

    /**
     * If the client wants to remove the DynamicLayout$ChangeWatcher span, remove the WatcherWrapper
     * instead.
     */
    @Override
    public void removeSpan(Object what) {
        final WatcherWrapper watcher;
        if (isWatcher(what)) {
            watcher = getWatcherFor(what);
            if (watcher != null) {
                what = watcher;
            }
        } else {
            watcher = null;
        }

        super.removeSpan(what);

        if (watcher != null) {
            mWatchers.remove(watcher);
        }
    }

    /**
     * Return the correct start for the DynamicLayout$ChangeWatcher span.
     */
    @Override
    public int getSpanStart(Object tag) {
        if (isWatcher(tag)) {
            final WatcherWrapper watcher = getWatcherFor(tag);
            if (watcher != null) {
                tag = watcher;
            }
        }
        return super.getSpanStart(tag);
    }

    /**
     * Return the correct end for the DynamicLayout$ChangeWatcher span.
     */
    @Override
    public int getSpanEnd(Object tag) {
        if (isWatcher(tag)) {
            final WatcherWrapper watcher = getWatcherFor(tag);
            if (watcher != null) {
                tag = watcher;
            }
        }
        return super.getSpanEnd(tag);
    }

    /**
     * Return the correct flags for the DynamicLayout$ChangeWatcher span.
     */
    @Override
    public int getSpanFlags(Object tag) {
        if (isWatcher(tag)) {
            final WatcherWrapper watcher = getWatcherFor(tag);
            if (watcher != null) {
                tag = watcher;
            }
        }
        return super.getSpanFlags(tag);
    }

    /**
     * Return the correct transition for the DynamicLayout$ChangeWatcher span.
     */
    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        if (isWatcher(type)) {
            type = WatcherWrapper.class;
        }
        return super.nextSpanTransition(start, limit, type);
    }

    /**
     * Find the WatcherWrapper for a given DynamicLayout$ChangeWatcher.
     *
     * @param object DynamicLayout$ChangeWatcher mObject
     *
     * @return WatcherWrapper that wraps the mObject.
     */
    private WatcherWrapper getWatcherFor(Object object) {
        for (int i = 0; i < mWatchers.size(); i++) {
            WatcherWrapper watcher = mWatchers.get(i);
            if (watcher.mObject == object) {
                return watcher;
            }
        }
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void beginBatchEdit() {
        blockWatchers();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void endBatchEdit() {
        unblockwatchers();
        fireWatchers();
    }

    /**
     * Block all watcher wrapper events.
     */
    private void blockWatchers() {
        for (int i = 0; i < mWatchers.size(); i++) {
            mWatchers.get(i).blockCalls();
        }
    }

    /**
     * Unblock all watcher wrapper events.
     */
    private void unblockwatchers() {
        for (int i = 0; i < mWatchers.size(); i++) {
            mWatchers.get(i).unblockCalls();
        }
    }

    /**
     * Unblock all watcher wrapper events. Called by editing operations, namely
     * {@link SpannableStringBuilder#replace(int, int, CharSequence)}.
     */
    private void fireWatchers() {
        for (int i = 0; i < mWatchers.size(); i++) {
            mWatchers.get(i).onTextChanged(this, 0, this.length(), this.length());
        }
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb) {
        blockWatchers();
        super.replace(start, end, tb);
        unblockwatchers();
        return this;
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart,
            int tbend) {
        blockWatchers();
        super.replace(start, end, tb, tbstart, tbend);
        unblockwatchers();
        return this;
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb) {
        super.insert(where, tb);
        return this;
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb, int start, int end) {
        super.insert(where, tb, start, end);
        return this;
    }

    @Override
    public SpannableStringBuilder delete(int start, int end) {
        super.delete(start, end);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text) {
        super.append(text);
        return this;
    }

    @Override
    public SpannableStringBuilder append(char text) {
        super.append(text);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, int start, int end) {
        super.append(text, start, end);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, Object what, int flags) {
        super.append(text, what, flags);
        return this;
    }

    /**
     * Wraps a DynamicLayout$ChangeWatcher in order to prevent firing of events to DynamicLayout.
     */
    private static class WatcherWrapper implements TextWatcher, SpanWatcher {
        private final Object mObject;
        private final AtomicInteger mBlockCalls = new AtomicInteger(0);

        WatcherWrapper(Object object) {
            this.mObject = object;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            ((TextWatcher) mObject).beforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            ((TextWatcher) mObject).onTextChanged(s, start, before, count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            ((TextWatcher) mObject).afterTextChanged(s);
        }

        /**
         * Prevent the onSpanAdded calls to DynamicLayout$ChangeWatcher if in a replace operation
         * (mBlockCalls is set) and the span that is added is an EmojiSpan.
         */
        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end) {
            if (mBlockCalls.get() > 0 && isEmojiSpan(what)) {
                return;
            }
            ((SpanWatcher) mObject).onSpanAdded(text, what, start, end);
        }

        /**
         * Prevent the onSpanRemoved calls to DynamicLayout$ChangeWatcher if in a replace operation
         * (mBlockCalls is set) and the span that is added is an EmojiSpan.
         */
        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end) {
            if (mBlockCalls.get() > 0 && isEmojiSpan(what)) {
                return;
            }
            ((SpanWatcher) mObject).onSpanRemoved(text, what, start, end);
        }

        /**
         * Prevent the onSpanChanged calls to DynamicLayout$ChangeWatcher if in a replace operation
         * (mBlockCalls is set) and the span that is added is an EmojiSpan.
         */
        @Override
        public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart,
                int nend) {
            if (mBlockCalls.get() > 0 && isEmojiSpan(what)) {
                return;
            }
            ((SpanWatcher) mObject).onSpanChanged(text, what, ostart, oend, nstart, nend);
        }

        final void blockCalls() {
            mBlockCalls.incrementAndGet();
        }

        final void unblockCalls() {
            mBlockCalls.decrementAndGet();
        }

        private boolean isEmojiSpan(final Object span) {
            return span instanceof EmojiSpan;
        }
    }

}
