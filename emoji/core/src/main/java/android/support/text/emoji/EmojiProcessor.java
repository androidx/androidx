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

import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.text.emoji.widget.SpannableBuilder;
import android.support.v4.graphics.PaintCompat;
import android.support.v4.util.Preconditions;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.KeyListener;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Processes the CharSequence and adds the emojis.
 *
 * @hide
 */
@AnyThread
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
final class EmojiProcessor {

    /**
     * State transition commands.
     */
    @IntDef({ACTION_ADVANCE_BOTH, ACTION_ADVANCE_END, ACTION_FLUSH})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Action {
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
     * Factory used to create EmojiSpans.
     */
    private final EmojiCompat.SpanFactory mSpanFactory;

    /**
     * Emoji metadata repository.
     */
    private final MetadataRepo mMetadataRepo;

    /**
     * Utility class that checks if the system can render a given glyph.
     */
    private GlyphChecker mGlyphChecker = new GlyphChecker();

    EmojiProcessor(@NonNull final MetadataRepo metadataRepo,
            @NonNull final EmojiCompat.SpanFactory spanFactory) {
        mSpanFactory = spanFactory;
        mMetadataRepo = metadataRepo;
    }

    EmojiMetadata getEmojiMetadata(@NonNull final CharSequence charSequence) {
        final ProcessorSm sm = new ProcessorSm(mMetadataRepo.getRootNode());
        final int end = charSequence.length();
        int currentOffset = 0;

        while (currentOffset < end) {
            final int codePoint = Character.codePointAt(charSequence, currentOffset);
            final int action = sm.check(codePoint);
            if (action != ACTION_ADVANCE_END) {
                return null;
            }
            currentOffset += Character.charCount(codePoint);
        }

        if (sm.isInFlushableState()) {
            return sm.getCurrentMetadata();
        }

        return null;
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
    CharSequence process(@NonNull final CharSequence charSequence, @IntRange(from = 0) int start,
            @IntRange(from = 0) int end, @IntRange(from = 0) int maxEmojiCount,
            final boolean replaceAll) {
        final boolean isSpannableBuilder = charSequence instanceof SpannableBuilder;
        if (isSpannableBuilder) {
            ((SpannableBuilder) charSequence).beginBatchEdit();
        }

        try {
            Spannable spannable = null;
            // if it is a spannable already, use the same instance to add/remove EmojiSpans.
            // otherwise wait until the the first EmojiSpan found in order to change the result
            // into a Spannable.
            if (isSpannableBuilder || charSequence instanceof Spannable) {
                spannable = (Spannable) charSequence;
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
            // add new ones
            int addedCount = 0;
            final ProcessorSm sm = new ProcessorSm(mMetadataRepo.getRootNode());

            int currentOffset = start;
            int codePoint = Character.codePointAt(charSequence, currentOffset);

            while (currentOffset < end && addedCount < maxEmojiCount) {
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
                        if (replaceAll || !hasGlyph(charSequence, start, currentOffset,
                                sm.getFlushMetadata())) {
                            if (spannable == null) {
                                spannable = new SpannableString(charSequence);
                            }
                            addEmoji(spannable, sm.getFlushMetadata(), start, currentOffset);
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
            if (sm.isInFlushableState() && addedCount < maxEmojiCount) {
                if (replaceAll || !hasGlyph(charSequence, start, currentOffset,
                        sm.getCurrentMetadata())) {
                    if (spannable == null) {
                        spannable = new SpannableString(charSequence);
                    }
                    addEmoji(spannable, sm.getCurrentMetadata(), start, currentOffset);
                    addedCount++;
                }
            }
            return spannable == null ? charSequence : spannable;
        } finally {
            if (isSpannableBuilder) {
                ((SpannableBuilder) charSequence).endBatchEdit();
            }
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
    static boolean handleOnKeyDown(@NonNull final Editable editable, final int keyCode,
            final KeyEvent event) {
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

    private static boolean delete(final Editable content, final KeyEvent event,
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
    static boolean handleDeleteSurroundingText(@NonNull final InputConnection inputConnection,
            @NonNull final Editable editable, @IntRange(from = 0) final int beforeLength,
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

    private static boolean hasModifiers(KeyEvent event) {
        return !KeyEvent.metaStateHasNoModifiers(event.getMetaState());
    }

    private void addEmoji(@NonNull final Spannable spannable, final EmojiMetadata metadata,
            final int start, final int end) {
        final EmojiSpan span = mSpanFactory.createSpan(metadata);
        spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Checks whether the current OS can render a given emoji. Used by the system to decide if an
     * emoji span should be added. If the system cannot render it, an emoji span will be added.
     * Used only for the case where replaceAll is set to {@code false}.
     *
     * @param charSequence the CharSequence that the emoji is in
     * @param start start index of the emoji in the CharSequence
     * @param end end index of the emoji in the CharSequence
     * @param metadata EmojiMetadata instance for the emoji
     *
     * @return {@code true} if the OS can render emoji, {@code false} otherwise
     */
    private boolean hasGlyph(final CharSequence charSequence, int start, final int end,
            final EmojiMetadata metadata) {
        // For pre M devices, heuristic in PaintCompat can result in false positives. we are
        // adding another heuristic using the sdkAdded field. if the emoji was added to OS
        // at a later version we assume that the system probably cannot render it.
        if (Build.VERSION.SDK_INT < 23 && metadata.getSdkAdded() > Build.VERSION.SDK_INT) {
            return false;
        }

        // if the existence is not calculated yet
        if (metadata.getHasGlyph() == EmojiMetadata.HAS_GLYPH_UNKNOWN) {
            final boolean hasGlyph = mGlyphChecker.hasGlyph(charSequence, start, end);
            metadata.setHasGlyph(hasGlyph);
        }

        return metadata.getHasGlyph() == EmojiMetadata.HAS_GLYPH_EXISTS;
    }

    /**
     * Set the GlyphChecker instance used by EmojiProcessor. Used for testing.
     */
    void setGlyphChecker(@NonNull final GlyphChecker glyphChecker) {
        Preconditions.checkNotNull(glyphChecker);
        mGlyphChecker = glyphChecker;
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

        ProcessorSm(MetadataRepo.Node rootNode) {
            mRootNode = rootNode;
            mCurrentNode = rootNode;
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
                                if (mCurrentNode.getData().isDefaultEmoji()
                                        || isEmojiStyle(mLastCodepoint)) {
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
        EmojiMetadata getFlushMetadata() {
            return mFlushNode.getData();
        }

        /**
         * @return current pointer to the metadata node in the trie
         */
        EmojiMetadata getCurrentMetadata() {
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
                    && (mCurrentNode.getData().isDefaultEmoji()
                    || isEmojiStyle(mLastCodepoint)
                    || mCurrentDepth > 1);
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
        private static int findIndexBackward(final CharSequence cs, final int from,
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
                    return 0;  // Reached to the beginning of the text w/o any invalid surrogate
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
        private static int findIndexForward(final CharSequence cs, final int from,
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

    /**
     * Utility class that checks if the system can render a given glyph.
     *
     * @hide
     */
    @AnyThread
    @RestrictTo(LIBRARY_GROUP)
    public static class GlyphChecker {
        /**
         * Default text size for {@link #mTextPaint}.
         */
        private static final int PAINT_TEXT_SIZE = 10;

        /**
         * Used to create strings required by
         * {@link PaintCompat#hasGlyph(android.graphics.Paint, String)}.
         */
        private static final ThreadLocal<StringBuilder> sStringBuilder = new ThreadLocal<>();

        /**
         * TextPaint used during {@link PaintCompat#hasGlyph(android.graphics.Paint, String)} check.
         */
        private final TextPaint mTextPaint;

        GlyphChecker() {
            mTextPaint = new TextPaint();
            mTextPaint.setTextSize(PAINT_TEXT_SIZE);
        }

        /**
         * Returns whether the system can render an emoji.
         *
         * @param charSequence the CharSequence that the emoji is in
         * @param start start index of the emoji in the CharSequence
         * @param end end index of the emoji in the CharSequence
         *
         * @return {@code true} if the OS can render emoji, {@code false} otherwise
         */
        public boolean hasGlyph(final CharSequence charSequence, int start, final int end) {
            final StringBuilder builder = getStringBuilder();
            builder.setLength(0);

            while (start < end) {
                builder.append(charSequence.charAt(start));
                start++;
            }

            return PaintCompat.hasGlyph(mTextPaint, builder.toString());
        }

        private static StringBuilder getStringBuilder() {
            if (sStringBuilder.get() == null) {
                sStringBuilder.set(new StringBuilder());
            }
            return sStringBuilder.get();
        }

    }
}
