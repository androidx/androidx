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

import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Processes the CharSequence and adds the emojis.
 *
 */
@AnyThread
@RestrictTo(LIBRARY)
final class EmojiProcessor {

    /**
     * State transition commands.
     */
    @IntDef({ACTION_ADVANCE_BOTH, ACTION_ADVANCE_END, ACTION_FLUSH})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Action {
    }

    private interface EmojiProcessCallback<T> {
        /**
         * Invoked on every emoji found during {@link #process}.
         * Returning {@code false} can abort this {@link #process} loop.
         */
        boolean handleEmoji(@NonNull CharSequence charSequence, int start, int end,
                TypefaceEmojiRasterizer metadata);

        /**
         * @return the result after process.
         */
        T getResult();
    }

    /**
     * Advance the end pointer in CharSequence and reset the start to be the end.
     */
    private static final int ACTION_ADVANCE_BOTH = 1;

    /**
     * Advance end pointer in CharSequence.
     */
    private static final int ACTION_ADVANCE_END = 2;

    /**
     * Add a new emoji with the metadata in {@link ProcessorSm#getFlushMetadata()}. Advance end
     * pointer in CharSequence and reset the start to be the end.
     */
    private static final int ACTION_FLUSH = 3;

    /**
     * The max number of characters look around in {@link #getEmojiStart(CharSequence, int)} and
     * {@link #getEmojiEnd(CharSequence, int)}.
     */
    private static final int MAX_LOOK_AROUND_CHARACTER = 16;

    /**
     * Factory used to create EmojiSpans.
     */
    private final EmojiCompat.@NonNull SpanFactory mSpanFactory;

    /**
     * Emoji metadata repository.
     */
    private final @NonNull MetadataRepo mMetadataRepo;

    /**
     * Utility class that checks if the system can render a given glyph.
     */
    private EmojiCompat.@NonNull GlyphChecker mGlyphChecker;

    /**
     * @see EmojiCompat.Config#setUseEmojiAsDefaultStyle(boolean)
     */
    private final boolean mUseEmojiAsDefaultStyle;

    /**
     * @see EmojiCompat.Config#setUseEmojiAsDefaultStyle(boolean, List)
     */
    private final int @Nullable [] mEmojiAsDefaultStyleExceptions;

    EmojiProcessor(
            final @NonNull MetadataRepo metadataRepo,
            final EmojiCompat.@NonNull SpanFactory spanFactory,
            final EmojiCompat.@NonNull GlyphChecker glyphChecker,
            final boolean useEmojiAsDefaultStyle,
            final int @Nullable [] emojiAsDefaultStyleExceptions,
            @NonNull Set<int[]> emojiExclusions) {
        mSpanFactory = spanFactory;
        mMetadataRepo = metadataRepo;
        mGlyphChecker = glyphChecker;
        mUseEmojiAsDefaultStyle = useEmojiAsDefaultStyle;
        mEmojiAsDefaultStyleExceptions = emojiAsDefaultStyleExceptions;
        initExclusions(emojiExclusions);
    }

    private void initExclusions(@NonNull Set<int[]> emojiExclusions) {
        if (emojiExclusions.isEmpty()) {
            return;
        }
        for (int[] codepoints : emojiExclusions) {
            String emoji = new String(codepoints, 0, codepoints.length);
            MarkExclusionCallback callback = new MarkExclusionCallback(emoji);
            process(emoji, 0, emoji.length(), 1, true, callback);
        }
    }

    @EmojiCompat.CodepointSequenceMatchResult
    int getEmojiMatch(final @NonNull CharSequence charSequence) {
        return getEmojiMatch(charSequence, mMetadataRepo.getMetadataVersion());
    }

    @EmojiCompat.CodepointSequenceMatchResult
    int getEmojiMatch(final @NonNull CharSequence charSequence,
            final int metadataVersion) {
        final ProcessorSm sm = new ProcessorSm(mMetadataRepo.getRootNode(),
                mUseEmojiAsDefaultStyle, mEmojiAsDefaultStyleExceptions);
        final int end = charSequence.length();
        int currentOffset = 0;
        int potentialSubsequenceMatch = 0;
        int subsequenceMatch = 0;

        while (currentOffset < end) {
            final int codePoint = Character.codePointAt(charSequence, currentOffset);
            final int action = sm.check(codePoint);
            TypefaceEmojiRasterizer currentNode = sm.getCurrentMetadata();
            switch (action) {
                case ACTION_FLUSH: {
                    // this happens when matching new unknown ZWJ sequences that are comprised of
                    // known emoji
                    currentNode = sm.getFlushMetadata();
                    if (currentNode.getCompatAdded() <= metadataVersion) {
                        subsequenceMatch++;
                    }
                    break;
                }
                case ACTION_ADVANCE_BOTH: {
                    currentOffset += Character.charCount(codePoint);
                    // state machine decided to skip previous entries
                    potentialSubsequenceMatch = 0;
                    break;
                } case ACTION_ADVANCE_END: {
                    currentOffset += Character.charCount(codePoint);
                    break;
                }
            }
            if (currentNode != null && currentNode.getCompatAdded() <= metadataVersion) {
                potentialSubsequenceMatch++;
            }
        }

        if (subsequenceMatch != 0) {
            // if we matched multiple emoji on the first pass, then the current emoji font
            // doesn't know about the codepoint sequence, and will decompose when REPLACE_ALL = true
            return EmojiCompat.EMOJI_FALLBACK;
        }

        if (sm.isInFlushableState()) {
            // We matched exactly one emoji
            // EmojiCompat can completely handle this sequence
            TypefaceEmojiRasterizer exactMatch = sm.getCurrentMetadata();
            if (exactMatch.getCompatAdded() <= metadataVersion) {
                return EmojiCompat.EMOJI_SUPPORTED;
            }
        }
        // if we get here than we definitely do not know the emoji, decide if we will decompose
        if (potentialSubsequenceMatch == 0) {
            return EmojiCompat.EMOJI_UNSUPPORTED;
        } else {
            return EmojiCompat.EMOJI_FALLBACK;
        }
    }


    /**
     * see {@link EmojiCompat#getEmojiStart(CharSequence, int)}.
     */
    int getEmojiStart(final @NonNull CharSequence charSequence, @IntRange(from = 0) int offset) {
        if (offset < 0 || offset >= charSequence.length()) {
            return -1;
        }

        if (charSequence instanceof Spanned) {
            final Spanned spanned = (Spanned) charSequence;
            final EmojiSpan[] spans = spanned.getSpans(offset, offset + 1, EmojiSpan.class);
            if (spans.length > 0) {
                return spanned.getSpanStart(spans[0]);
            }
        }

        // TODO: come up with some heuristic logic to better determine the interval
        final int start = Math.max(0, offset - MAX_LOOK_AROUND_CHARACTER);
        final int end = Math.min(charSequence.length(), offset + MAX_LOOK_AROUND_CHARACTER);
        return process(charSequence, start, end, EmojiCompat.EMOJI_COUNT_UNLIMITED, true,
                new EmojiProcessLookupCallback(offset)).start;
    }

    /**
     * see {@link EmojiCompat#getEmojiStart(CharSequence, int)}.
     */
    int getEmojiEnd(final @NonNull CharSequence charSequence, @IntRange(from = 0) int offset) {
        if (offset < 0 || offset >= charSequence.length()) {
            return -1;
        }

        if (charSequence instanceof Spanned) {
            final Spanned spanned = (Spanned) charSequence;
            final EmojiSpan[] spans = spanned.getSpans(offset, offset + 1, EmojiSpan.class);
            if (spans.length > 0) {
                return spanned.getSpanEnd(spans[0]);
            }
        }

        // TODO: come up with some heuristic logic to better determine the interval
        final int start = Math.max(0, offset - MAX_LOOK_AROUND_CHARACTER);
        final int end = Math.min(charSequence.length(), offset + MAX_LOOK_AROUND_CHARACTER);
        return process(charSequence, start, end, EmojiCompat.EMOJI_COUNT_UNLIMITED, true,
                new EmojiProcessLookupCallback(offset)).end;
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
     * @param maxEmojiCount maximum number of emojis in the {@code charSequence}, should be greater
     *                      than or equal to {@code 0}
     * @param replaceAll whether to replace all emoji with {@link EmojiSpan}s
     */
    CharSequence process(final @NonNull CharSequence charSequence, @IntRange(from = 0) int start,
            @IntRange(from = 0) int end, @IntRange(from = 0) int maxEmojiCount,
            final boolean replaceAll) {
        final boolean isSpannableBuilder = charSequence instanceof SpannableBuilder;
        if (isSpannableBuilder) {
            ((SpannableBuilder) charSequence).beginBatchEdit();
        }

        try {
            UnprecomputeTextOnModificationSpannable spannable = null;
            // if it is a spannable already, use the same instance to add/remove EmojiSpans.
            // otherwise wait until the first EmojiSpan found in order to change the result
            // into a Spannable.
            if (isSpannableBuilder || charSequence instanceof Spannable) {
                spannable = new UnprecomputeTextOnModificationSpannable((Spannable) charSequence);
            } else if (charSequence instanceof Spanned) {
                // check if there are any EmojiSpans as cheap as possible
                // start-1, end+1 will return emoji span that starts/ends at start/end indices
                final int nextSpanTransition = ((Spanned) charSequence).nextSpanTransition(
                        start - 1, end + 1, EmojiSpan.class);

                if (nextSpanTransition <= end) {
                    spannable = new UnprecomputeTextOnModificationSpannable(charSequence);
                }
            }

            if (spannable != null) {
                final EmojiSpan[] spans = spannable.getSpans(start, end, EmojiSpan.class);
                if (spans != null && spans.length > 0) {
                    // remove existing spans, and realign the start, end according to spans
                    // if start or end is in the middle of an emoji they should be aligned
                    final int length = spans.length;
                    for (int index = 0; index < length; index++) {
                        final EmojiSpan span = spans[index];
                        final int spanStart = spannable.getSpanStart(span);
                        final int spanEnd = spannable.getSpanEnd(span);
                        // Remove span only when its spanStart is NOT equal to current end.
                        // During add operation an emoji at index 0 is added with 0-1 as start and
                        // end indices. Therefore if there are emoji spans at [0-1] and [1-2]
                        // and end is 1, the span between 0-1 should be deleted, not 1-2.
                        if (spanStart != end) {
                            spannable.removeSpan(span);
                        }
                        start = Math.min(spanStart, start);
                        end = Math.max(spanEnd, end);
                    }
                }
            }

            if (start == end || start >= charSequence.length()) {
                return charSequence;
            }

            // calculate max number of emojis that can be added. since getSpans call is a relatively
            // expensive operation, do it only when maxEmojiCount is not unlimited.
            if (maxEmojiCount != EmojiCompat.EMOJI_COUNT_UNLIMITED && spannable != null) {
                maxEmojiCount -= spannable.getSpans(0, spannable.length(), EmojiSpan.class).length;
            }

            spannable = process(charSequence, start, end, maxEmojiCount, replaceAll,
                    new EmojiProcessAddSpanCallback(spannable, mSpanFactory));

            // if nothing was written, always return the source
            if (spannable != null) {
                return spannable.getUnwrappedSpannable();
            } else {
                return charSequence;
            }
        } finally {
            if (isSpannableBuilder) {
                ((SpannableBuilder) charSequence).endBatchEdit();
            }
        }
    }

    private <T> T process(final @NonNull CharSequence charSequence, @IntRange(from = 0) int start,
            @IntRange(from = 0) int end, @IntRange(from = 0) int maxEmojiCount,
            final boolean processAll, final EmojiProcessCallback<T> emojiProcessCallback) {
        int addedCount = 0;
        final ProcessorSm sm = new ProcessorSm(mMetadataRepo.getRootNode(),
                mUseEmojiAsDefaultStyle, mEmojiAsDefaultStyleExceptions);

        int currentOffset = start;
        int codePoint = Character.codePointAt(charSequence, currentOffset);
        boolean keepProcessing = true;

        while (currentOffset < end && addedCount < maxEmojiCount && keepProcessing) {
            final int action = sm.check(codePoint);

            switch (action) {
                case ACTION_ADVANCE_BOTH:
                    start += Character.charCount(Character.codePointAt(charSequence, start));
                    currentOffset = start;
                    if (currentOffset < end) {
                        codePoint = Character.codePointAt(charSequence, currentOffset);
                    }
                    break;
                case ACTION_ADVANCE_END:
                    currentOffset += Character.charCount(codePoint);
                    if (currentOffset < end) {
                        codePoint = Character.codePointAt(charSequence, currentOffset);
                    }
                    break;
                case ACTION_FLUSH:
                    if (processAll || !hasGlyph(charSequence, start, currentOffset,
                            sm.getFlushMetadata())) {
                        keepProcessing = emojiProcessCallback.handleEmoji(charSequence, start,
                                currentOffset, sm.getFlushMetadata());
                        addedCount++;
                    }
                    start = currentOffset;
                    break;
            }
        }

        // After the last codepoint is consumed the state machine might be in a state where it
        // identified an emoji before. i.e. abc[women-emoji] when the last codepoint is consumed
        // state machine is waiting to see if there is an emoji sequence (i.e. ZWJ).
        // Need to check if it is in such a state.
        if (sm.isInFlushableState() && addedCount < maxEmojiCount && keepProcessing) {
            if (processAll || !hasGlyph(charSequence, start, currentOffset,
                    sm.getCurrentMetadata())) {
                emojiProcessCallback.handleEmoji(charSequence, start,
                        currentOffset, sm.getCurrentMetadata());
                addedCount++;
            }
        }

        return emojiProcessCallback.getResult();
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
    static boolean handleOnKeyDown(final @NonNull Editable editable, final int keyCode,
            final @NonNull KeyEvent event) {
        final boolean handled;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                handled = delete(editable, event, false /*forwardDelete*/);
                break;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                handled = delete(editable, event, true /*forwardDelete*/);
                break;
            default:
                handled = false;
                break;
        }

        if (handled) {
            MetaKeyKeyListener.adjustMetaAfterKeypress(editable);
            return true;
        }

        return false;
    }

    private static boolean delete(final @NonNull Editable content, final @NonNull KeyEvent event,
            final boolean forwardDelete) {
        if (hasModifiers(event)) {
            return false;
        }

        final int start = Selection.getSelectionStart(content);
        final int end = Selection.getSelectionEnd(content);
        if (hasInvalidSelection(start, end)) {
            return false;
        }

        final EmojiSpan[] spans = content.getSpans(start, end, EmojiSpan.class);
        if (spans != null && spans.length > 0) {
            final int length = spans.length;
            for (int index = 0; index < length; index++) {
                final EmojiSpan span = spans[index];
                final int spanStart = content.getSpanStart(span);
                final int spanEnd = content.getSpanEnd(span);
                if ((forwardDelete && spanStart == start)
                        || (!forwardDelete && spanEnd == start)
                        || (start > spanStart && start < spanEnd)) {
                    content.delete(spanStart, spanEnd);
                    return true;
                }
            }
        }

        return false;
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
    static boolean handleDeleteSurroundingText(final @NonNull InputConnection inputConnection,
            final @NonNull Editable editable, @IntRange(from = 0) final int beforeLength,
            @IntRange(from = 0) final int afterLength, final boolean inCodePoints) {
        //noinspection ConstantConditions
        if (editable == null || inputConnection == null) {
            return false;
        }

        if (beforeLength < 0 || afterLength < 0) {
            return false;
        }

        final int selectionStart = Selection.getSelectionStart(editable);
        final int selectionEnd = Selection.getSelectionEnd(editable);

        if (hasInvalidSelection(selectionStart, selectionEnd)) {
            return false;
        }

        int start;
        int end;
        if (inCodePoints) {
            // go backwards in terms of codepoints
            start = CodepointIndexFinder.findIndexBackward(editable, selectionStart,
                    Math.max(beforeLength, 0));
            end = CodepointIndexFinder.findIndexForward(editable, selectionEnd,
                    Math.max(afterLength, 0));

            if (start == CodepointIndexFinder.INVALID_INDEX
                    || end == CodepointIndexFinder.INVALID_INDEX) {
                return false;
            }
        } else {
            start = Math.max(selectionStart - beforeLength, 0);
            end = Math.min(selectionEnd + afterLength, editable.length());
        }

        final EmojiSpan[] spans = editable.getSpans(start, end, EmojiSpan.class);
        if (spans != null && spans.length > 0) {
            final int length = spans.length;
            for (int index = 0; index < length; index++) {
                final EmojiSpan span = spans[index];
                int spanStart = editable.getSpanStart(span);
                int spanEnd = editable.getSpanEnd(span);
                start = Math.min(spanStart, start);
                end = Math.max(spanEnd, end);
            }

            start = Math.max(start, 0);
            end = Math.min(end, editable.length());

            inputConnection.beginBatchEdit();
            editable.delete(start, end);
            inputConnection.endBatchEdit();
            return true;
        }

        return false;
    }

    private static boolean hasInvalidSelection(final int start, final int end) {
        return start == -1 || end == -1 || start != end;
    }

    private static boolean hasModifiers(@NonNull KeyEvent event) {
        return !KeyEvent.metaStateHasNoModifiers(event.getMetaState());
    }

    /**
     * Checks whether the current OS can render a given emoji. Used by the system to decide if an
     * emoji span should be added. If the system cannot render it, an emoji span will be added.
     * Used only for the case where replaceAll is set to {@code false}.
     *
     * @param charSequence the CharSequence that the emoji is in
     * @param start start index of the emoji in the CharSequence
     * @param end end index of the emoji in the CharSequence
     * @param rasterizer TypefaceEmojiRasterizer instance for the emoji
     *
     * @return {@code true} if the OS can render emoji, {@code false} otherwise
     */
    private boolean hasGlyph(final CharSequence charSequence, int start, final int end,
            final TypefaceEmojiRasterizer rasterizer) {
        // if the existence is not calculated yet
        if (rasterizer.getHasGlyph() == TypefaceEmojiRasterizer.HAS_GLYPH_UNKNOWN) {
            final boolean hasGlyph = mGlyphChecker.hasGlyph(charSequence, start, end,
                    rasterizer.getSdkAdded());
            rasterizer.setHasGlyph(hasGlyph);
        }

        return rasterizer.getHasGlyph() == TypefaceEmojiRasterizer.HAS_GLYPH_EXISTS;
    }

    /**
     * State machine for walking over the metadata trie.
     */
    static final class ProcessorSm {

        private static final int STATE_DEFAULT = 1;
        private static final int STATE_WALKING = 2;

        private int mState = STATE_DEFAULT;

        /**
         * Root of the trie
         */
        private final MetadataRepo.Node mRootNode;

        /**
         * Pointer to the node after last codepoint.
         */
        private MetadataRepo.Node mCurrentNode;

        /**
         * The node where ACTION_FLUSH is called. Required since after flush action is
         * returned mCurrentNode is reset to be the root.
         */
        private MetadataRepo.Node mFlushNode;

        /**
         * The code point that was checked.
         */
        private int mLastCodepoint;

        /**
         * Level for mCurrentNode. Root is 0.
         */
        private int mCurrentDepth;

        /**
         * @see EmojiCompat.Config#setUseEmojiAsDefaultStyle(boolean)
         */
        private final boolean mUseEmojiAsDefaultStyle;

        /**
         * @see EmojiCompat.Config#setUseEmojiAsDefaultStyle(boolean, List)
         */
        private final int[] mEmojiAsDefaultStyleExceptions;

        ProcessorSm(MetadataRepo.Node rootNode, boolean useEmojiAsDefaultStyle,
                int[] emojiAsDefaultStyleExceptions) {
            mRootNode = rootNode;
            mCurrentNode = rootNode;
            mUseEmojiAsDefaultStyle = useEmojiAsDefaultStyle;
            mEmojiAsDefaultStyleExceptions = emojiAsDefaultStyleExceptions;
        }

        @Action
        int check(final int codePoint) {
            final int action;
            MetadataRepo.Node node = mCurrentNode.get(codePoint);
            switch (mState) {
                case STATE_WALKING:
                    if (node != null) {
                        mCurrentNode = node;
                        mCurrentDepth += 1;
                        action = ACTION_ADVANCE_END;
                    } else {
                        if (isTextStyle(codePoint)) {
                            action = reset();
                        } else if (isEmojiStyle(codePoint)) {
                            action = ACTION_ADVANCE_END;
                        } else if (mCurrentNode.getData() != null) {
                            if (mCurrentDepth == 1) {
                                if (shouldUseEmojiPresentationStyleForSingleCodepoint()) {
                                    mFlushNode = mCurrentNode;
                                    action = ACTION_FLUSH;
                                    reset();
                                } else {
                                    action = reset();
                                }
                            } else {
                                mFlushNode = mCurrentNode;
                                action = ACTION_FLUSH;
                                reset();
                            }
                        } else {
                            action = reset();
                        }
                    }
                    break;
                case STATE_DEFAULT:
                default:
                    if (node == null) {
                        action = reset();
                    } else {
                        mState = STATE_WALKING;
                        mCurrentNode = node;
                        mCurrentDepth = 1;
                        action = ACTION_ADVANCE_END;
                    }
                    break;
            }

            mLastCodepoint = codePoint;
            return action;
        }

        @Action
        private int reset() {
            mState = STATE_DEFAULT;
            mCurrentNode = mRootNode;
            mCurrentDepth = 0;
            return ACTION_ADVANCE_BOTH;
        }

        /**
         * @return the metadata node when ACTION_FLUSH is returned
         */
        TypefaceEmojiRasterizer getFlushMetadata() {
            return mFlushNode.getData();
        }

        /**
         * @return current pointer to the metadata node in the trie
         */
        TypefaceEmojiRasterizer getCurrentMetadata() {
            return mCurrentNode.getData();
        }

        /**
         * Need for the case where input is consumed, but action_flush was not called. For example
         * when the char sequence has single codepoint character which is a default emoji. State
         * machine will wait for the next.
         *
         * @return whether the current state requires an emoji to be added
         */
        boolean isInFlushableState() {
            return mState == STATE_WALKING && mCurrentNode.getData() != null
                    && (mCurrentDepth > 1 || shouldUseEmojiPresentationStyleForSingleCodepoint());
        }

        private boolean shouldUseEmojiPresentationStyleForSingleCodepoint() {
            if (mCurrentNode.getData().isDefaultEmoji()) {
                // The codepoint is emoji style by default.
                return true;
            }
            if (isEmojiStyle(mLastCodepoint)) {
                // The codepoint was followed by the emoji style variation selector.
                return true;
            }
            if (mUseEmojiAsDefaultStyle) {
                // Emoji presentation style for text style default emojis is enabled. We have
                // to check that the current codepoint is not an exception.
                if (mEmojiAsDefaultStyleExceptions == null) {
                    return true;
                }
                final int codepoint = mCurrentNode.getData().getCodepointAt(0);
                final int index = Arrays.binarySearch(mEmojiAsDefaultStyleExceptions, codepoint);
                if (index < 0) {
                    // Index is negative, so the codepoint was not found in the array of exceptions.
                    return true;
                }
            }
            return false;
        }

        /**
         * @param codePoint CodePoint to check
         *
         * @return {@code true} if the codepoint is a emoji style standardized variation selector
         */
        private static boolean isEmojiStyle(int codePoint) {
            return codePoint == 0xFE0F;
        }

        /**
         * @param codePoint CodePoint to check
         *
         * @return {@code true} if the codepoint is a text style standardized variation selector
         */
        private static boolean isTextStyle(int codePoint) {
            return codePoint == 0xFE0E;
        }
    }

    /**
     * Copy of BaseInputConnection findIndexBackward and findIndexForward functions.
     */
    private static final class CodepointIndexFinder {
        private static final int INVALID_INDEX = -1;

        private CodepointIndexFinder() {}

        /**
         * Find start index of the character in {@code cs} that is {@code numCodePoints} behind
         * starting from {@code from}.
         *
         * @param cs CharSequence to work on
         * @param from the index to start going backwards
         * @param numCodePoints the number of codepoints
         *
         * @return start index of the character
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        static int findIndexBackward(final CharSequence cs, final int from,
                final int numCodePoints) {
            int currentIndex = from;
            boolean waitingHighSurrogate = false;
            final int length = cs.length();
            if (currentIndex < 0 || length < currentIndex) {
                return INVALID_INDEX;  // The starting point is out of range.
            }
            if (numCodePoints < 0) {
                return INVALID_INDEX;  // Basically this should not happen.
            }
            int remainingCodePoints = numCodePoints;
            while (true) {
                if (remainingCodePoints == 0) {
                    return currentIndex;  // Reached to the requested length in code points.
                }

                --currentIndex;
                if (currentIndex < 0) {
                    if (waitingHighSurrogate) {
                        return INVALID_INDEX;  // An invalid surrogate pair is found.
                    }
                    return 0;  // Reached to the R of the text w/o any invalid surrogate
                    // pair.
                }
                final char c = cs.charAt(currentIndex);
                if (waitingHighSurrogate) {
                    if (!Character.isHighSurrogate(c)) {
                        return INVALID_INDEX;  // An invalid surrogate pair is found.
                    }
                    waitingHighSurrogate = false;
                    --remainingCodePoints;
                    continue;
                }
                if (!Character.isSurrogate(c)) {
                    --remainingCodePoints;
                    continue;
                }
                if (Character.isHighSurrogate(c)) {
                    return INVALID_INDEX;  // A invalid surrogate pair is found.
                }
                waitingHighSurrogate = true;
            }
        }

        /**
         * Find start index of the character in {@code cs} that is {@code numCodePoints} ahead
         * starting from {@code from}.
         *
         * @param cs CharSequence to work on
         * @param from the index to start going forward
         * @param numCodePoints the number of codepoints
         *
         * @return start index of the character
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        static int findIndexForward(final CharSequence cs, final int from,
                final int numCodePoints) {
            int currentIndex = from;
            boolean waitingLowSurrogate = false;
            final int length = cs.length();
            if (currentIndex < 0 || length < currentIndex) {
                return INVALID_INDEX;  // The starting point is out of range.
            }
            if (numCodePoints < 0) {
                return INVALID_INDEX;  // Basically this should not happen.
            }
            int remainingCodePoints = numCodePoints;

            while (true) {
                if (remainingCodePoints == 0) {
                    return currentIndex;  // Reached to the requested length in code points.
                }

                if (currentIndex >= length) {
                    if (waitingLowSurrogate) {
                        return INVALID_INDEX;  // An invalid surrogate pair is found.
                    }
                    return length;  // Reached to the end of the text w/o any invalid surrogate
                    // pair.
                }
                final char c = cs.charAt(currentIndex);
                if (waitingLowSurrogate) {
                    if (!Character.isLowSurrogate(c)) {
                        return INVALID_INDEX;  // An invalid surrogate pair is found.
                    }
                    --remainingCodePoints;
                    waitingLowSurrogate = false;
                    ++currentIndex;
                    continue;
                }
                if (!Character.isSurrogate(c)) {
                    --remainingCodePoints;
                    ++currentIndex;
                    continue;
                }
                if (Character.isLowSurrogate(c)) {
                    return INVALID_INDEX;  // A invalid surrogate pair is found.
                }
                waitingLowSurrogate = true;
                ++currentIndex;
            }
        }
    }

    private static class EmojiProcessAddSpanCallback
            implements EmojiProcessCallback<UnprecomputeTextOnModificationSpannable> {
        public @Nullable UnprecomputeTextOnModificationSpannable spannable;
        private final EmojiCompat.SpanFactory mSpanFactory;

        EmojiProcessAddSpanCallback(@Nullable UnprecomputeTextOnModificationSpannable spannable,
                EmojiCompat.SpanFactory spanFactory) {
            this.spannable = spannable;
            this.mSpanFactory = spanFactory;
        }

        @Override
        public boolean handleEmoji(@NonNull CharSequence charSequence, int start, int end,
                TypefaceEmojiRasterizer metadata) {
            if (metadata.isPreferredSystemRender()) {
                return true;
            }
            if (spannable == null) {
                spannable = new UnprecomputeTextOnModificationSpannable(
                        charSequence instanceof Spannable
                                ? (Spannable) charSequence
                                : new SpannableString(charSequence));
            }
            final EmojiSpan span = mSpanFactory.createSpan(metadata);
            spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return true;
        }

        @Override
        public UnprecomputeTextOnModificationSpannable getResult() {
            return spannable;
        }
    }

    private static class EmojiProcessLookupCallback
            implements EmojiProcessCallback<EmojiProcessLookupCallback> {
        private final int mOffset;

        public int start = -1;

        public int end = -1;

        EmojiProcessLookupCallback(int offset) {
            this.mOffset = offset;
        }

        @Override
        public boolean handleEmoji(@NonNull CharSequence charSequence, int start, int end,
                TypefaceEmojiRasterizer metadata) {
            if (start <= mOffset && mOffset < end) {
                this.start = start;
                this.end = end;
                return false;
            }

            return end <= mOffset;
        }

        @Override
        public EmojiProcessLookupCallback getResult() {
            return this;
        }
    }

    /**
     * Mark exclusinos for any emoji matched by this callback
     */
    private static class MarkExclusionCallback
            implements EmojiProcessCallback<MarkExclusionCallback> {

        private final String mExclusion;

        MarkExclusionCallback(String emoji) {
            mExclusion = emoji;
        }

        @Override
        public boolean handleEmoji(@NonNull CharSequence charSequence, int start, int end,
                TypefaceEmojiRasterizer metadata) {
            if (TextUtils.equals(charSequence.subSequence(start, end), mExclusion)) {
                metadata.setExclusion(true);
                return false;
            } else {
                return true;
            }
        }

        @Override
        public MarkExclusionCallback getResult() {
            return this;
        }
    }
}
