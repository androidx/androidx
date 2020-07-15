/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import java.text.BreakIterator
import java.util.Locale

import androidx.ui.semantics.SemanticsProperties

/**
 * This class contains the implementation of text segment iterators
 * for accessibility support.
 *
 * Note: We want to be able to iterator over [SemanticsProperties.AccessibilityLabel] of any
 * component.
 */
internal class AccessibilityIterators {

    interface TextSegmentIterator {
        /**
         * Given the current position, returning the start and end of next element in an array.
         */
        fun following(current: Int): IntArray?
        /**
         * Given the current position, returning the start and end of previous element in an array.
         */
        fun preceding(current: Int): IntArray?
    }

    abstract class AbstractTextSegmentIterator : TextSegmentIterator {

        protected lateinit var text: String

        private val segment = IntArray(2)

        open fun initialize(text: String) {
            this.text = text
        }

        protected fun getRange(start: Int, end: Int): IntArray? {
            if (start < 0 || end < 0 || start == end) {
                return null
            }
            segment[0] = start
            segment[1] = end
            return segment
        }
    }

    open class CharacterTextSegmentIterator private constructor(locale: Locale) :
        AbstractTextSegmentIterator() {
        companion object {
            private var instance: CharacterTextSegmentIterator? = null
            fun getInstance(locale: Locale): CharacterTextSegmentIterator {
                if (instance == null) {
                    instance = CharacterTextSegmentIterator(locale)
                }
                return instance as CharacterTextSegmentIterator
            }
        }

        private lateinit var impl: BreakIterator

        init {
            onLocaleChanged(locale)
            // TODO(yingleiw): register callback for locale change
            // ViewRootImpl.addConfigCallback(this);
        }

        override fun initialize(text: String) {
            super.initialize(text)
            impl.setText(text)
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= textLength) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (!impl.isBoundary(start)) {
                start = impl.following(start)
                if (start == BreakIterator.DONE) {
                    return null
                }
            }
            val end = impl.following(start)
            if (end == BreakIterator.DONE) {
                return null
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (!impl.isBoundary(end)) {
                end = impl.preceding(end)
                if (end == BreakIterator.DONE) {
                    return null
                }
            }
            val start = impl.preceding(end)
            if (start == BreakIterator.DONE) {
                return null
            }
            return getRange(start, end)
        }

        // TODO(yingleiw): callback for locale change
        /*
        @Override
        public void onConfigurationChanged(Configuration globalConfig) {
            final Locale locale = globalConfig.getLocales().get(0);
            if (locale == null) {
                return;
            }
            if (!mLocale.equals(locale)) {
                mLocale = locale;
                onLocaleChanged(locale);
            }
        }
        */

        private fun onLocaleChanged(locale: Locale) {
            impl = BreakIterator.getCharacterInstance(locale)
        }
    }

    class WordTextSegmentIterator private constructor(locale: Locale) :
        AbstractTextSegmentIterator() {
        companion object {
            private var instance: WordTextSegmentIterator? = null

            fun getInstance(locale: Locale): WordTextSegmentIterator {
                if (instance == null) {
                    instance = WordTextSegmentIterator(locale)
                }
                return instance as WordTextSegmentIterator
            }
        }

        private lateinit var impl: BreakIterator

        init {
            onLocaleChanged(locale)
            // TODO: register callback for locale change
            // ViewRootImpl.addConfigCallback(this);
        }

        override fun initialize(text: String) {
            super.initialize(text)
            impl.setText(text)
        }

        private fun onLocaleChanged(locale: Locale) {
            impl = BreakIterator.getWordInstance(locale)
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= text.length) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (!isLetterOrDigit(start) && !isStartBoundary(start)) {
                start = impl.following(start)
                if (start == BreakIterator.DONE) {
                    return null
                }
            }
            val end = impl.following(start)
            if (end == BreakIterator.DONE || !isEndBoundary(end)) {
                return null
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (end > 0 && !isLetterOrDigit(end - 1) && !isEndBoundary(end)) {
                end = impl.preceding(end)
                if (end == BreakIterator.DONE) {
                    return null
                }
            }
            val start = impl.preceding(end)
            if (start == BreakIterator.DONE || !isStartBoundary(start)) {
                return null
            }
            return getRange(start, end)
        }

        private fun isStartBoundary(index: Int): Boolean {
            return isLetterOrDigit(index) &&
                    (index == 0 || !isLetterOrDigit(index - 1))
        }

        private fun isEndBoundary(index: Int): Boolean {
            return (index > 0 && isLetterOrDigit(index - 1)) &&
                    (index == text.length || !isLetterOrDigit(index))
        }

        private fun isLetterOrDigit(index: Int): Boolean {
            if (index >= 0 && index < text.length) {
                val codePoint = text.codePointAt(index)
                return Character.isLetterOrDigit(codePoint)
            }
            return false
        }
    }

    class ParagraphTextSegmentIterator : AbstractTextSegmentIterator() {
        companion object {
            private var instance: ParagraphTextSegmentIterator? = null

            fun getInstance(): ParagraphTextSegmentIterator {
                if (instance == null) {
                    instance = ParagraphTextSegmentIterator()
                }
                return instance as ParagraphTextSegmentIterator
            }
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= textLength) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (start < textLength && text[start] == '\n' &&
                !isStartBoundary(start)) {
                start++
            }
            if (start >= textLength) {
                return null
            }
            var end = start + 1
            while (end < textLength && !isEndBoundary(end)) {
                end++
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (end > 0 && text[end - 1] == '\n' && !isEndBoundary(end)) {
                end--
            }
            if (end <= 0) {
                return null
            }
            var start = end - 1
            while (start > 0 && !isStartBoundary(start)) {
                start--
            }
            return getRange(start, end)
        }

        private fun isStartBoundary(index: Int): Boolean {
            return (text[index] != '\n' &&
                    (index == 0 || text[index - 1] == '\n'))
        }

        private fun isEndBoundary(index: Int): Boolean {
            return (index > 0 && text[index - 1] != '\n' &&
                    (index == text.length || text[index] == '\n'))
        }
    }

    // TODO: This is tightly coupled with Text.kt. Need to discuss with the text team on how to
    //  expose the necessary properties.
    /*
    static class LineTextSegmentIterator extends AbstractTextSegmentIterator {
        private static LineTextSegmentIterator sLineInstance;

        protected static final int DIRECTION_START = -1;
        protected static final int DIRECTION_END = 1;

        protected Layout mLayout;

        public static LineTextSegmentIterator getInstance() {
            if (sLineInstance == null) {
                sLineInstance = new LineTextSegmentIterator();
            }
            return sLineInstance;
        }

        public void initialize(Spannable text, Layout layout) {
            mText = text.toString();
            mLayout = layout;
        }

        @Override
        public int[] following(int offset) {
            final int textLegth = mText.length();
            if (textLegth <= 0) {
                return null;
            }
            if (offset >= mText.length()) {
                return null;
            }
            int nextLine;
            if (offset < 0) {
                nextLine = mLayout.getLineForOffset(0);
            } else {
                final int currentLine = mLayout.getLineForOffset(offset);
                if (getLineEdgeIndex(currentLine, DIRECTION_START) == offset) {
                    nextLine = currentLine;
                } else {
                    nextLine = currentLine + 1;
                }
            }
            if (nextLine >= mLayout.getLineCount()) {
                return null;
            }
            final int start = getLineEdgeIndex(nextLine, DIRECTION_START);
            final int end = getLineEdgeIndex(nextLine, DIRECTION_END) + 1;
            return getRange(start, end);
        }

        @Override
        public int[] preceding(int offset) {
            final int textLegth = mText.length();
            if (textLegth <= 0) {
                return null;
            }
            if (offset <= 0) {
                return null;
            }
            int previousLine;
            if (offset > mText.length()) {
                previousLine = mLayout.getLineForOffset(mText.length());
            } else {
                final int currentLine = mLayout.getLineForOffset(offset);
                if (getLineEdgeIndex(currentLine, DIRECTION_END) + 1 == offset) {
                    previousLine = currentLine;
                } else {
                    previousLine = currentLine - 1;
                }
            }
            if (previousLine < 0) {
                return null;
            }
            final int start = getLineEdgeIndex(previousLine, DIRECTION_START);
            final int end = getLineEdgeIndex(previousLine, DIRECTION_END) + 1;
            return getRange(start, end);
        }

        protected int getLineEdgeIndex(int lineNumber, int direction) {
            final int paragraphDirection = mLayout.getParagraphDirection(lineNumber);
            if (direction * paragraphDirection < 0) {
                return mLayout.getLineStart(lineNumber);
            } else {
                return mLayout.getLineEnd(lineNumber) - 1;
            }
        }
    }

    static class PageTextSegmentIterator extends LineTextSegmentIterator {
        private static PageTextSegmentIterator sPageInstance;

        private TextView mView;

        private final Rect mTempRect = new Rect();

        public static PageTextSegmentIterator getInstance() {
            if (sPageInstance == null) {
                sPageInstance = new PageTextSegmentIterator();
            }
            return sPageInstance;
        }

        public void initialize(TextView view) {
            super.initialize((Spannable) view.getIterableTextForAccessibility(), view.getLayout());
            mView = view;
        }

        @Override
        public int[] following(int offset) {
            final int textLength = mText.length();
            if (textLength <= 0) {
                return null;
            }
            if (offset >= mText.length()) {
                return null;
            }
            if (!mView.getGlobalVisibleRect(mTempRect)) {
                return null;
            }

            final int start = Math.max(0, offset);

            final int currentLine = mLayout.getLineForOffset(start);
            final int currentLineTop = mLayout.getLineTop(currentLine);
            final int pageHeight = mTempRect.height() - mView.getTotalPaddingTop()
            - mView.getTotalPaddingBottom();
            final int nextPageStartY = currentLineTop + pageHeight;
            final int lastLineTop = mLayout.getLineTop(mLayout.getLineCount() - 1);
            final int currentPageEndLine = (nextPageStartY < lastLineTop)
            ? mLayout.getLineForVertical(nextPageStartY) - 1 : mLayout.getLineCount() - 1;

            final int end = getLineEdgeIndex(currentPageEndLine, DIRECTION_END) + 1;

            return getRange(start, end);
        }

        @Override
        public int[] preceding(int offset) {
            final int textLength = mText.length();
            if (textLength <= 0) {
                return null;
            }
            if (offset <= 0) {
                return null;
            }
            if (!mView.getGlobalVisibleRect(mTempRect)) {
                return null;
            }

            final int end = Math.min(mText.length(), offset);

            final int currentLine = mLayout.getLineForOffset(end);
            final int currentLineTop = mLayout.getLineTop(currentLine);
            final int pageHeight = mTempRect.height() - mView.getTotalPaddingTop()
            - mView.getTotalPaddingBottom();
            final int previousPageEndY = currentLineTop - pageHeight;
            int currentPageStartLine = (previousPageEndY > 0) ?
            mLayout.getLineForVertical(previousPageEndY) : 0;
            // If we're at the end of text, we're at the end of the current line rather than the
            // start of the next line, so we should move up one fewer lines than we would otherwise.
            if (end == mText.length() && (currentPageStartLine < currentLine)) {
                currentPageStartLine += 1;
            }

            final int start = getLineEdgeIndex(currentPageStartLine, DIRECTION_START);

            return getRange(start, end);
        }
    }
    */
}
