/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.ast;

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

/**
 * {@link Node} that stores text.
 *
 * <p>Text may represent a string or number.
 * For example in the query `hello AND "world peace" -cat price:49.99`
 * <ul>
 *     <li> hello and cat are strings.
 *     <li> "world peace" is a verbatim string, i.e. a quoted string that can be represented by
 *     setting mVerbatim to true. Because it is a verbatim string, it will be treated as a
 *     single term "world peace" instead of terms "world" and "peace".
 *     <li> 49.99 is a number. {@link TextNode}s may represent integers or doubles and treat numbers
 *     as terms.
 *     <li> price is NOT a string but a property path as part of a {@link PropertyRestrictNode}.
 * </ul>
 *
 * <p>The node will be segmented and normalized based on the flags set in the Node.
 * For example, if the node containing the string "foo" has both mPrefix and mVerbatim set to true,
 * then the resulting tree will be treated as the query `"foo"*`
 * i.e. the prefix of the quoted string "foo".
 *
 * <p>{@link TextNode}s is guaranteed to not have child nodes.
 *
 * <p>This API may change in response to feedback and additional changes.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class TextNode implements Node{
    private String mValue;
    private boolean mPrefix = false;
    private boolean mVerbatim = false;

    /**
     * Public constructor for {@link TextNode} representing text passed into the constructor as a
     * string.
     *
     * <p>By default {@link #mPrefix}  and {@link #mVerbatim} are both false. In other words the
     * {@link TextNode} represents a term that is not the prefix of a potentially longer term that
     * could be matched against and not a quoted string to be treated as a single term.
     *
     * @param value The text value that {@link TextNode} holds.
     */
    public TextNode(@NonNull String value) {
        mValue = Preconditions.checkNotNull(value);
    }

    /**
     * Copy constructor that takes in {@link TextNode}.
     *
     * @param original The {@link TextNode} to copy and return another {@link TextNode}.
     */
    public TextNode(@NonNull TextNode original) {
        Preconditions.checkNotNull(original);
        mValue = original.mValue;
        mPrefix = original.mPrefix;
        mVerbatim = original.mVerbatim;
    }

    /**
     * Retrieve the string value that the TextNode holds.
     *
     * @return A string representing the text that the TextNode holds.
     */
    @NonNull
    public String getValue() {
        return mValue;
    }

    /**
     * Whether or not a TextNode represents a query term that will match indexed tokens when the
     * query term is a prefix of the token.
     *
     * <p>For example, if the value of the TextNode is "foo" and mPrefix is set to true, then the
     * TextNode represents the query `foo*`, and will match against tokens like "foo", "foot", and
     * "football".
     *
     * <p>If mPrefix and mVerbatim are both true, then the TextNode represents the prefix of the
     * quoted string. For example if the value of the TextNode is "foo bar" and both mPrefix and
     * mVerbatim are set to true, then the TextNode represents the query `"foo bar"*`.
     *
     * @return True, if the TextNode represents a query term that will match indexed tokens when the
     * query term is a prefix of the token.
     *
     * <p> False, if the TextNode represents a query term that will only match exact tokens in the
     * index.
     */
    public boolean isPrefix() {
        return mPrefix;
    }

    /**
     * Whether or not a TextNode represents a quoted string.
     *
     * <p>For example, if the value of the TextNode is "foo bar" and mVerbatim is set to true, then
     * the TextNode represents the query `"foo bar"`. "foo bar" will be treated as a single token
     * and match documents that have a property marked as verbatim and exactly contain
     * "foo bar".
     *
     * <p>If mVerbatim and mPrefix are both true, then the TextNode represents the prefix of the
     * quoted string. For example if the value of the TextNode is "foo bar" and both mPrefix and
     * mVerbatim are set to true, then the TextNode represents the query `"foo bar"*`.
     *
     * @return True, if the TextNode represents a quoted string. For example, if the value of
     * TextNode is "foo bar", then the query represented is `"foo bar"`. This means "foo bar" will
     * be treated as one term, matching documents that have a property marked as verbatim and
     * contains exactly "foo bar".
     *
     * <p> False, if the TextNode does not represent a quoted string. For example, if the value of
     * TextNode is "foo bar", then the query represented is `foo bar`. This means that "foo" and
     * "bar" will be treated as separate terms instead of one term and implicitly ANDed, matching
     * documents that contain both "foo" and "bar".
     */
    public boolean isVerbatim() {
        return mVerbatim;
    }

    /**
     * Set the text value that the {@link TextNode} holds.
     *
     * @param value The string that the {@link TextNode} will hold.
     */
    public void setValue(@NonNull String value) {
        mValue = Preconditions.checkNotNull(value);
    }

    /**
     * Set whether or not the {@link TextNode} represents a prefix. If true, the {@link TextNode}
     * represents a prefix match for {@code value}.
     *
     * @param isPrefix Whether or not the {@link TextNode} represents a prefix. If true, it
     *                 represents a query term that will match against indexed tokens when the query
     *                 term is a prefix of token.
     */
    public void setPrefix(boolean isPrefix) {
        mPrefix = isPrefix;
    }

    /**
     * Set whether or not the {@link TextNode} represents a quoted string, i.e. verbatim. If true,
     * the {@link TextNode} represents a quoted string.
     *
     * @param isVerbatim Whether or not the {@link TextNode} represents a quoted string. If true, it
     *     represents a quoted string.
     */
    public void setVerbatim(boolean isVerbatim) {
        mVerbatim = isVerbatim;
    }

    /**
     * Get the query string representation of {@link TextNode}.
     *
     * <p>If no flags are set, then the string representation is just the value
     * held by {@link TextNode}. Otherwise the value will be formatted depending on the combination
     * of flags set.
     *
     * <p>The string representation of {@link TextNode} maybe different from {@link #mValue} if it
     * contains operators that need to be escaped for the query string to be treated as a string
     * rather than a query.
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append('(');
        if (mVerbatim) {
            queryStringBuilder.append('\"');
        }
        escapeString(queryStringBuilder);
        if (mVerbatim) {
            queryStringBuilder.append('\"');
        }
        if (mPrefix) {
            queryStringBuilder.append('*');
        }
        queryStringBuilder.append(')');
        return queryStringBuilder.toString();
    }

    /**
     * Escapes {@link #mValue} by adding backslashes to special characters that could be
     * interpreted by Icing as operators and making everything lower case.
     */
    private void escapeString(StringBuilder queryStringBuilder) {
        for (int i = 0; i < mValue.length(); i++) {
            char currChar = mValue.charAt(i);
            if ((!mVerbatim && isSpecialCharacter(currChar)) || currChar == '"') {
                // If the Text is not verbatim, we need to escape all characters that either are
                // themselves operators or signify an operator, such as parentheses for functions.
                // If the Text is verbatim, we only need to escape quotes.
                queryStringBuilder.append('\\');
            } else if (!mVerbatim && isLatinLetter(currChar)
                    && Character.isUpperCase(currChar)) {
                // If not verbatim, escape operators such as NOT, AND, OR.
                currChar = Character.toLowerCase(currChar);
            }
            queryStringBuilder.append(currChar);
        }
    }

    /**
     * Returns whether or not a given character is a special symbol in the query language.
     *
     * <p>Special symbols include:
     * <ul>
     *     <li>Logical operators such as negation ("-").
     *     <li>Numeric search operators such as less than (">").
     *     <li>Other characters that are tricky to handle such as comma (",").
     * </ul>
     */
    private boolean isSpecialCharacter(char character) {
        switch (character) {
            case '-':
            case '*':
            case ':':
            case '"':
            case '>':
            case '<':
            case '=':
            case '(':
            case ')':
            case '\\':
            case ',':
            case '.':
            case '&':
            case '|':
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns whether or not a given character is a letter in the Latin alphabet.
     */
    private boolean isLatinLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
