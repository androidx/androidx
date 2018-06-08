/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A helper class for building the list of buckets for alpha jump.
 */
public class AlphaJumpBucketer {
    private static final Character[] DEFAULT_INITIAL_CHARS = {
            '0', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    private static final String[] PREFIX_WORDS = {"a", "the"};

    private final List<Bucket> mBuckets;

    public AlphaJumpBucketer() {
        mBuckets = new ArrayList<>();
        for (Character ch : DEFAULT_INITIAL_CHARS) {
            if (ch == '0') {
                mBuckets.add(new Bucket("123", (String s) -> s.matches("^[0-9]")));
            } else {
                String prefix = new String(new char[] {ch});
                mBuckets.add(new Bucket(prefix, (String s) -> s.startsWith(prefix.toLowerCase())));
            }
        }
    }

    public AlphaJumpBucketer(Bucket[] buckets) {
        mBuckets = Arrays.asList(buckets);
    }

    /**
     * Creates a collection of {@link IAlphaJumpAdapter.Bucket}s from the given list of strings.
     */
    public Collection<IAlphaJumpAdapter.Bucket> createBuckets(String[] values) {
        return createBuckets(Arrays.asList(values));
    }

    /**
     * Creates a collection of {@link IAlphaJumpAdapter.Bucket}s from the given iterable collection
     * of strings.
    */
    public Collection<IAlphaJumpAdapter.Bucket> createBuckets(Iterable<String> values) {
        return createBuckets(values.iterator());
    }

    /**
     * Creates the collection of {@link IAlphaJumpAdapter.Bucket}s from the given enumeration of
     * values.
     */
    public Collection<IAlphaJumpAdapter.Bucket> createBuckets(Iterator<String> values) {
        int index = 0;
        while (values.hasNext()) {
            String value = values.next();
            for (Bucket bucket : mBuckets) {
                if (bucket.matchString(value, index)) {
                    break;
                }
            }
            index++;
        }
        ArrayList<IAlphaJumpAdapter.Bucket> buckets = new ArrayList<>();
        buckets.addAll(mBuckets);
        return buckets;
    }

    /**
     * "Preprocess" a string so that we remove some common prefixes and so on when performing the
     * bucketing.
     *
     * @param s The string to pre-process.
     * @return The input string with whitespace trimmed, and also words like "the", "a" and so on
     *    removed.
     */
    private static String preprocess(String s) {
        s = s.trim().toLowerCase();

        for (String word : PREFIX_WORDS) {
            if (s.startsWith(word + " ")) {
                s = s.substring(0, word.length() + 1).trim();
                break;
            }
        }

        return s;
    }

    /**
     * A basic implementation of {@link IAlphaJumpAdapter.Bucket}.
     */
    public static class Bucket implements IAlphaJumpAdapter.Bucket {
        private CharSequence mLabel;
        private int mIndex;
        private Predicate<String> mStringMatcher;
        private boolean mIsEmpty;

        Bucket(CharSequence label, Predicate<String> stringMatcher) {
            mLabel = label;
            mIndex = -1;
            mIsEmpty = true;
            mStringMatcher = stringMatcher;
        }

        boolean matchString(String s, int index) {
            boolean match = mStringMatcher.test(preprocess(s));
            if (match) {
                if (mIndex < 0) {
                    mIndex = index;
                }
                mIsEmpty = false;
            }
            return match;
        }

        @Override
        public boolean isEmpty() {
            return mIsEmpty;
        }

        @Override
        public CharSequence getLabel() {
            return mLabel;
        }

        @Override
        public int getIndex() {
            return mIndex;
        }
    }
}
