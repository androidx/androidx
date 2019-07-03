/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.text;

import androidx.annotation.NonNull;

import java.text.CharacterIterator;

/**
 * An implementation of {@link java.text.CharacterIterator} that iterates over a given CharSequence.
 *
 * Note: This file is copied from
 * <a href="https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/CharSequenceCharacterIterator.java">
 * CharSequenceCharacterIterator.java
 * </a>.
 *
 * {@hide}
 */
public class CharSequenceCharacterIterator implements CharacterIterator {
    /** The index of the beginning of the range. */
    private final int mBeginIndex;
    /** The index of the end of the range. */
    private final int mEndIndex;
    /** The index of the current position of iterator. */
    private int mIndex;
    /** The given char sequence. */
    private final CharSequence mCharSequence;

    /**
     * Constructs the iterator given a CharSequence and a range. The position of the iterator index
     * is set to the beginning of the range.
     */
    public CharSequenceCharacterIterator(@NonNull CharSequence text, int start, int end) {
        mCharSequence = text;
        mBeginIndex = mIndex = start;
        mEndIndex = end;
    }

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     *
     * @return the first character in the text, or {@link java.text.CharacterIterator#DONE} if
     * the text is empty
     * @see #getBeginIndex()
     */
    public char first() {
        mIndex = mBeginIndex;
        return current();
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     *
     * @return the last character in the text, or {@link java.text.CharacterIterator#DONE} if the
     * text is empty
     * @see #getEndIndex()
     */
    public char last() {
        if (mBeginIndex == mEndIndex) {
            mIndex = mEndIndex;
            return DONE;
        } else {
            mIndex = mEndIndex - 1;
            return mCharSequence.charAt(mIndex);
        }
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     *
     * @return the character at the current position or {@link java.text.CharacterIterator#DONE}
     * if the current
     * position is off the end of the text.
     * @see #getIndex()
     */
    public char current() {
        return (mIndex == mEndIndex) ? DONE : mCharSequence.charAt(mIndex);
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of {@link java.text.CharacterIterator#DONE} is returned.
     *
     * @return the character at the new position or {@link java.text.CharacterIterator#DONE} if
     * the new
     * position is off the end of the text range.
     */
    public char next() {
        mIndex++;
        if (mIndex >= mEndIndex) {
            mIndex = mEndIndex;
            return DONE;
        } else {
            return mCharSequence.charAt(mIndex);
        }
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of {@link java.text.CharacterIterator#DONE} is
     * returned.
     *
     * @return the character at the new position or {@link java.text.CharacterIterator#DONE} if
     * the current
     * position is equal to getBeginIndex().
     */
    public char previous() {
        if (mIndex <= mBeginIndex) {
            return DONE;
        } else {
            mIndex--;
            return mCharSequence.charAt(mIndex);
        }
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     *
     * @param position the position within the text.  Valid values range from
     *                 getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     *                 if an invalid value is supplied.
     * @return the character at the specified position or
     * {@link java.text.CharacterIterator#DONE} if the specified
     * position is equal to getEndIndex()
     */
    public char setIndex(int position) {
        if (mBeginIndex <= position && position <= mEndIndex) {
            mIndex = position;
            return current();
        } else {
            throw new IllegalArgumentException("invalid position");
        }
    }

    /**
     * Returns the start index of the text.
     *
     * @return the index at which the text begins.
     */
    public int getBeginIndex() {
        return mBeginIndex;
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     *
     * @return the index after the last character in the text
     */
    public int getEndIndex() {
        return mEndIndex;
    }

    /**
     * Returns the current index.
     *
     * @return the current index.
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Create a copy of this iterator
     *
     * @return A copy of this
     */
    @NonNull
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
