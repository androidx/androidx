/*
 *******************************************************************************
 * Copyright (C) 2007-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package androidx.core.i18n.messageformat_icu.simple;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import androidx.annotation.RestrictTo;
import androidx.core.i18n.messageformat_icu.util.Output;

/**
 * <p>
 * Defines rules for mapping non-negative numeric values onto a small set of keywords.
 * </p>
 * <p>
 * Rules are constructed from a text description, consisting of a series of keywords and conditions. The {@link #select}
 * method examines each condition in order and returns the keyword for the first condition that matches the number. If
 * none match, {@link #KEYWORD_OTHER} is returned.
 * </p>
 * <p>
 * A PluralRules object is immutable. It contains caches for sample values, but those are synchronized.
 * <p>
 * PluralRules is Serializable so that it can be used in formatters, which are serializable.
 * </p>
 * <p>
 * For more information, details, and tips for writing rules, see the <a
 * href="https://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules">LDML spec,
 * Part 3.5 Language Plural Rules</a>
 * </p>
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * &quot;one: n is 1; few: n in 2..4&quot;
 * </pre>
 * <p>
 * This defines two rules, for 'one' and 'few'. The condition for 'one' is "n is 1" which means that the number must be
 * equal to 1 for this condition to pass. The condition for 'few' is "n in 2..4" which means that the number must be
 * between 2 and 4 inclusive - and be an integer - for this condition to pass. All other numbers are assigned the
 * keyword "other" by the default rule.
 * </p>
 *
 * <pre>
 * &quot;zero: n is 0; one: n is 1; zero: n mod 100 in 1..19&quot;
 * </pre>
 * <p>
 * This illustrates that the same keyword can be defined multiple times. Each rule is examined in order, and the first
 * keyword whose condition passes is the one returned. Also notes that a modulus is applied to n in the last rule. Thus
 * its condition holds for 119, 219, 319...
 * </p>
 *
 * <pre>
 * &quot;one: n is 1; few: n mod 10 in 2..4 and n mod 100 not in 12..14&quot;
 * </pre>
 * <p>
 * This illustrates conjunction and negation. The condition for 'few' has two parts, both of which must be met:
 * "n mod 10 in 2..4" and "n mod 100 not in 12..14". The first part applies a modulus to n before the test as in the
 * previous example. The second part applies a different modulus and also uses negation, thus it matches all numbers
 * _not_ in 12, 13, 14, 112, 113, 114, 212, 213, 214...
 * </p>
 * <p>
 * Syntax:
 * </p>
 * <pre>
 * rules         = rule (';' rule)*
 * rule          = keyword ':' condition
 * keyword       = &lt;identifier&gt;
 * condition     = and_condition ('or' and_condition)*
 * and_condition = relation ('and' relation)*
 * relation      = not? expr not? rel not? range_list
 * expr          = ('n' | 'i' | 'f' | 'v' | 't') (mod value)?
 * not           = 'not' | '!'
 * rel           = 'in' | 'is' | '=' | '≠' | 'within'
 * mod           = 'mod' | '%'
 * range_list    = (range | value) (',' range_list)*
 * value         = digit+
 * digit         = 0|1|2|3|4|5|6|7|8|9
 * range         = value'..'value
 * </pre>
 * <p>Each <b>not</b> term inverts the meaning; however, there should not be more than one of them.</p>
 * <p>
 * The i, f, t, and v values are defined as follows:
 * </p>
 * <ul>
 * <li>i to be the integer digits.</li>
 * <li>f to be the visible decimal digits, as an integer.</li>
 * <li>t to be the visible decimal digits—without trailing zeros—as an integer.</li>
 * <li>v to be the number of visible fraction digits.</li>
 * <li>j is defined to only match integers. That is j is 3 fails if v != 0 (eg for 3.1 or 3.0).</li>
 * </ul>
 * <p>
 * Examples are in the following table:
 * </p>
 * <table border='1' style="border-collapse:collapse">
 * <tbody>
 * <tr>
 * <th>n</th>
 * <th>i</th>
 * <th>f</th>
 * <th>v</th>
 * </tr>
 * <tr>
 * <td>1.0</td>
 * <td>1</td>
 * <td align="right">0</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>1.00</td>
 * <td>1</td>
 * <td align="right">0</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>1.3</td>
 * <td>1</td>
 * <td align="right">3</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>1.03</td>
 * <td>1</td>
 * <td align="right">3</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>1.23</td>
 * <td>1</td>
 * <td align="right">23</td>
 * <td>2</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * An "identifier" is a sequence of characters that do not have the Unicode Pattern_Syntax or Pattern_White_Space
 * properties.
 * <p>
 * The difference between 'in' and 'within' is that 'in' only includes integers in the specified range, while 'within'
 * includes all values. Using 'within' with a range_list consisting entirely of values is the same as using 'in' (it's
 * not an error).
 * </p>
 *
 * icu_annot::stable ICU 3.8
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PluralRules implements Serializable {

    // static final UnicodeSet ALLOWED_ID = new UnicodeSet("[a-z]").freeze();

    // TODO Remove RulesList by moving its API and fields into PluralRules.

    private static final String CATEGORY_SEPARATOR = ";  ";

    private static final long serialVersionUID = 1;

    private final RuleList rules;
    private final transient Set<String> keywords;

    /**
     * Provides a factory for returning plural rules
     *
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public static abstract class Factory {
        /**
         * Provides access to the predefined <code>PluralRules</code> for a given locale and the plural type.
         *
         * <p>
         * ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>. For these predefined
         * rules, see CLDR page at
         * https://unicode-org.github.io/cldr-staging/charts/latest/supplemental/language_plural_rules.html
         *
         * @param locale
         *            The locale for which a <code>PluralRules</code> object is returned.
         * @param type
         *            The plural type (e.g., cardinal or ordinal).
         * @return The predefined <code>PluralRules</code> object for this locale. If there's no predefined rules for
         *         this locale, the rules for the closest parent in the locale hierarchy that has one will be returned.
         *         The final fallback always returns the default rules.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public abstract PluralRules forLocale(Locale locale, PluralType type);

        /**
         * Utility for getting CARDINAL rules.
         * @param locale the locale
         * @return plural rules.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final PluralRules forLocale(Locale locale) {
            return forLocale(locale, PluralType.CARDINAL);
        }

        /**
         * Returns the locales for which there is plurals data.
         *
         * icu_annot::internal CLDR
         * This API is ICU internal only.
        @Deprecated
        public abstract ULocale[] getAvailableULocales();
         */

        /**
         * Returns the 'functionally equivalent' locale with respect to plural rules. Calling PluralRules.forLocale with
         * the functionally equivalent locale, and with the provided locale, returns rules that behave the same. <br/>
         * All locales with the same functionally equivalent locale have plural rules that behave the same. This is not
         * exhaustive; there may be other locales whose plural rules behave the same that do not have the same equivalent
         * locale.
         *
         * @param locale
         *            the locale to check
         * @param isAvailable
         *            if not null and of length > 0, this will hold 'true' at index 0 if locale is directly defined
         *            (without fallback) as having plural rules
         * @return the functionally-equivalent locale
         * icu_annot::internal CLDR
         * This API is ICU internal only.
        @Deprecated
        public abstract ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable);
         */

        /**
         * Returns the default factory.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public static PluralRulesLoader getDefaultFactory() {
            return PluralRulesLoader.loader;
        }

        /**
         * Returns whether or not there are overrides.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
        @Deprecated
        public abstract boolean hasOverride(ULocale locale);
         */
    }
    // Standard keywords.

    /**
     * Common name for the 'zero' plural form.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_ZERO = "zero";

    /**
     * Common name for the 'singular' plural form.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_ONE = "one";

    /**
     * Common name for the 'dual' plural form.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_TWO = "two";

    /**
     * Common name for the 'paucal' or other special plural form.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_FEW = "few";

    /**
     * Common name for the arabic (11 to 99) plural form.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_MANY = "many";

    /**
     * Common name for the default plural form.  This name is returned
     * for values to which no other form in the rule applies.  It
     * can additionally be assigned rules of its own.
     * icu_annot::stable ICU 3.8
     */
    public static final String KEYWORD_OTHER = "other";

    /**
     * Value returned by {@link #getUniqueKeywordValue} when there is no
     * unique value to return.
     * icu_annot::stable ICU 4.8
     */
    public static final double NO_UNIQUE_VALUE = -0.00123456777;

    /**
     * Type of plurals and PluralRules.
     * icu_annot::stable ICU 50
     */
    public enum PluralType {
        /**
         * Plural rules for cardinal numbers: 1 file vs. 2 files.
         * icu_annot::stable ICU 50
         */
        CARDINAL,
        /**
         * Plural rules for ordinal numbers: 1st file, 2nd file, 3rd file, 4th file, etc.
         * icu_annot::stable ICU 50
         */
        ORDINAL
    };

    /*
     * The default constraint that is always satisfied.
     */
    private static final Constraint NO_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 9163464945387899416L;

        @Override
        public boolean isFulfilled(FixedDecimal n) {
            return true;
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            return false;
        }

        @Override
        public String toString() {
            return "";
        }
    };

    /**
     *
     */
    private static final Rule DEFAULT_RULE = new Rule("other", NO_CONSTRAINT, null, null);

    /**
     * Parses a plural rules description and returns a PluralRules.
     * @param description the rule description.
     * @throws ParseException if the description cannot be parsed.
     *    The exception index is typically not set, it will be -1.
     * icu_annot::stable ICU 3.8
     */
    public static PluralRules parseDescription(String description)
            throws ParseException {

        description = description.trim();
        return description.length() == 0 ? DEFAULT : new PluralRules(parseRuleChain(description));
    }

    /**
     * Creates a PluralRules from a description if it is parsable,
     * otherwise returns null.
     * @param description the rule description.
     * @return the PluralRules
     * icu_annot::stable ICU 3.8
     */
    public static PluralRules createRules(String description) {
        try {
            return parseDescription(description);
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * The default rules that accept any number and return
     * {@link #KEYWORD_OTHER}.
     * icu_annot::stable ICU 3.8
     */
    public static final PluralRules DEFAULT = new PluralRules(new RuleList().addRule(DEFAULT_RULE));

    private enum Operand {
        n,
        i,
        f,
        t,
        v,
        w,
        /* deprecated */
        j;
    }

    /**
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public static class FixedDecimal extends Number implements Comparable<FixedDecimal> {
        private static final long serialVersionUID = -4756200506571685661L;
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final double source;

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final int visibleDecimalDigitCount;

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final int visibleDecimalDigitCountWithoutTrailingZeros;

        /**
         * Whether the number has no nonzero fraction digits.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final long decimalDigits;

        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final long decimalDigitsWithoutTrailingZeros;

        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final long integerValue;

        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final boolean hasIntegerValue;

        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final boolean isNegative;

        private final int baseFactor;

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public double getSource() {
            return source;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public int getVisibleDecimalDigitCount() {
            return visibleDecimalDigitCount;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public int getVisibleDecimalDigitCountWithoutTrailingZeros() {
            return visibleDecimalDigitCountWithoutTrailingZeros;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public long getDecimalDigits() {
            return decimalDigits;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public long getDecimalDigitsWithoutTrailingZeros() {
            return decimalDigitsWithoutTrailingZeros;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public long getIntegerValue() {
            return integerValue;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public boolean isHasIntegerValue() {
            return hasIntegerValue;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public boolean isNegative() {
            return isNegative;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public int getBaseFactor() {
            return baseFactor;
        }

        static final long MAX = (long)1E18;

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         * @param n is the original number
         * @param v number of digits to the right of the decimal place. e.g. 1.00 = 2 25. = 0
         * @param f Corresponds to f in the plural rules grammar.
         *   The digits to the right of the decimal place as an integer. e.g. 1.10 = 10
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimal(double n, int v, long f) {
            isNegative = n < 0;
            source = isNegative ? -n : n;
            visibleDecimalDigitCount = v;
            decimalDigits = f;
            integerValue = n > MAX ? MAX : (long) n;
            hasIntegerValue = source == integerValue;
            // check values. TODO make into unit test.
            //
            //            long visiblePower = (int) Math.pow(10, v);
            //            if (fractionalDigits > visiblePower) {
            //                throw new IllegalArgumentException();
            //            }
            //            double fraction = intValue + (fractionalDigits / (double) visiblePower);
            //            if (fraction != source) {
            //                double diff = Math.abs(fraction - source)/(Math.abs(fraction) + Math.abs(source));
            //                if (diff > 0.00000001d) {
            //                    throw new IllegalArgumentException();
            //                }
            //            }
            if (f == 0) {
                decimalDigitsWithoutTrailingZeros = 0;
                visibleDecimalDigitCountWithoutTrailingZeros = 0;
            } else {
                long fdwtz = f;
                int trimmedCount = v;
                while ((fdwtz%10) == 0) {
                    fdwtz /= 10;
                    --trimmedCount;
                }
                decimalDigitsWithoutTrailingZeros = fdwtz;
                visibleDecimalDigitCountWithoutTrailingZeros = trimmedCount;
            }
            baseFactor = (int) Math.pow(10, v);
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimal(double n, int v) {
            this(n,v,getFractionalDigits(n, v));
        }

        private static int getFractionalDigits(double n, int v) {
            if (v == 0) {
                return 0;
            } else {
                if (n < 0) {
                    n = -n;
                }
                int baseFactor = (int) Math.pow(10, v);
                long scaled = Math.round(n * baseFactor);
                return (int) (scaled % baseFactor);
            }
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimal(double n) {
            this(n, decimals(n));
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimal(long n) {
            this(n,0);
        }

        private static final long MAX_INTEGER_PART = 1000000000;
        /**
         * Return a guess as to the number of decimals that would be displayed. This is only a guess; callers should
         * always supply the decimals explicitly if possible. Currently, it is up to 6 decimals (without trailing zeros).
         * Returns 0 for infinities and nans.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         *
         */
        // @Deprecated: in fact internal ICU
        public static int decimals(double n) {
            // Ugly...
            if (Double.isInfinite(n) || Double.isNaN(n)) {
                return 0;
            }
            if (n < 0) {
                n = -n;
            }
            if (n < MAX_INTEGER_PART) {
                long temp = (long)(n * 1000000) % 1000000; // get 6 decimals
                for (int mask = 10, digits = 6; digits > 0; mask *= 10, --digits) {
                    if ((temp % mask) != 0) {
                        return digits;
                    }
                }
                return 0;
            } else {
                String buf = String.format(Locale.ENGLISH, "%1.15e", n);
                int ePos = buf.lastIndexOf('e');
                int expNumPos = ePos + 1;
                if (buf.charAt(expNumPos) == '+') {
                    expNumPos++;
                }
                String exponentStr = buf.substring(expNumPos);
                int exponent = Integer.parseInt(exponentStr);
                int numFractionDigits = ePos - 2 - exponent;
                if (numFractionDigits < 0) {
                    return 0;
                }
                for (int i=ePos-1; numFractionDigits > 0; --i) {
                    if (buf.charAt(i) != '0') {
                        break;
                    }
                    --numFractionDigits;
                }
                return numFractionDigits;
            }
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimal (String n) {
            // Ugly, but for samples we don't care.
            this(Double.parseDouble(n), getVisibleFractionCount(n));
        }

        private static int getVisibleFractionCount(String value) {
            value = value.trim();
            int decimalPos = value.indexOf('.') + 1;
            if (decimalPos == 0) {
                return 0;
            } else {
                return value.length() - decimalPos;
            }
        }

        /**
         * {@inheritDoc}
         *
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public double get(@SuppressLint("HiddenTypeParameter") Operand operand) {
            switch(operand) {
            default: return source;
            case i: return integerValue;
            case f: return decimalDigits;
            case t: return decimalDigitsWithoutTrailingZeros;
            case v: return visibleDecimalDigitCount;
            case w: return visibleDecimalDigitCountWithoutTrailingZeros;
            }
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @SuppressLint("HiddenTypeParameter")
        public static Operand getOperand(String t) {
            return Operand.valueOf(t);
        }

        /**
         * We're not going to care about NaN.
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public int compareTo(FixedDecimal other) {
            if (integerValue != other.integerValue) {
                return integerValue < other.integerValue ? -1 : 1;
            }
            if (source != other.source) {
                return source < other.source ? -1 : 1;
            }
            if (visibleDecimalDigitCount != other.visibleDecimalDigitCount) {
                return visibleDecimalDigitCount < other.visibleDecimalDigitCount ? -1 : 1;
            }
            long diff = decimalDigits - other.decimalDigits;
            if (diff != 0) {
                return diff < 0 ? -1 : 1;
            }
            return 0;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public boolean equals(Object arg0) {
            if (arg0 == null) {
                return false;
            }
            if (arg0 == this) {
                return true;
            }
            if (!(arg0 instanceof FixedDecimal)) {
                return false;
            }
            FixedDecimal other = (FixedDecimal)arg0;
            return source == other.source && visibleDecimalDigitCount == other.visibleDecimalDigitCount && decimalDigits == other.decimalDigits;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return (int)(decimalDigits + 37 * (visibleDecimalDigitCount + (int)(37 * source)));
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public String toString() {
            return String.format("%." + visibleDecimalDigitCount + "f", source);
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public boolean hasIntegerValue() {
            return hasIntegerValue;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public int intValue() {
            // TODO Auto-generated method stub
            return (int) integerValue;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public long longValue() {
            return integerValue;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public float floatValue() {
            return (float) source;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public double doubleValue() {
            return isNegative ? -source : source;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public long getShiftedValue() {
            return integerValue * baseFactor + decimalDigits;
        }

        private void writeObject(
                ObjectOutputStream out)
                        throws IOException {
            throw new NotSerializableException();
        }

        private void readObject(ObjectInputStream in
                ) throws IOException, ClassNotFoundException {
            throw new NotSerializableException();
        }
    }

    /**
     * Selection parameter for either integer-only or decimal-only.
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public enum SampleType {
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        INTEGER,
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        DECIMAL
    }

    /**
     * A range of NumberInfo that includes all values with the same visibleFractionDigitCount.
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public static class FixedDecimalRange {
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final FixedDecimal start;
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final FixedDecimal end;
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public FixedDecimalRange(FixedDecimal start, FixedDecimal end) {
            if (start.visibleDecimalDigitCount != end.visibleDecimalDigitCount) {
                throw new IllegalArgumentException("Ranges must have the same number of visible decimals: " + start + "~" + end);
            }
            this.start = start;
            this.end = end;
        }
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public String toString() {
            return start + (end.equals(start) ? "" : "~" + end);
        }
    }

    /**
     * A list of NumberInfo that includes all values with the same visibleFractionDigitCount.
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public static class FixedDecimalSamples {
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final SampleType sampleType;
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final Set<FixedDecimalRange> samples;
        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public final boolean bounded;
        /**
         * The samples must be immutable.
         * @param sampleType
         * @param samples
         */
        private FixedDecimalSamples(SampleType sampleType, Set<FixedDecimalRange> samples, boolean bounded) {
            super();
            this.sampleType = sampleType;
            this.samples = samples;
            this.bounded = bounded;
        }
        /*
         * Parse a list of the form described in CLDR. The source must be trimmed.
         */
        static FixedDecimalSamples parse(String source) {
            SampleType sampleType2;
            boolean bounded2 = true;
            boolean haveBound = false;
            Set<FixedDecimalRange> samples2 = new LinkedHashSet<>();

            if (source.startsWith("integer")) {
                sampleType2 = SampleType.INTEGER;
            } else if (source.startsWith("decimal")) {
                sampleType2 = SampleType.DECIMAL;
            } else {
                throw new IllegalArgumentException("Samples must start with 'integer' or 'decimal'");
            }
            source = source.substring(7).trim(); // remove both

            for (String range : COMMA_SEPARATED.split(source, 0)) {
                if (range.equals("…") || range.equals("...")) {
                    bounded2 = false;
                    haveBound = true;
                    continue;
                }
                if (haveBound) {
                    throw new IllegalArgumentException("Can only have … at the end of samples: " + range);
                }
                String[] rangeParts = TILDE_SEPARATED.split(range, 0);
                switch (rangeParts.length) {
                case 1:
                    FixedDecimal sample = new FixedDecimal(rangeParts[0]);
                    checkDecimal(sampleType2, sample);
                    samples2.add(new FixedDecimalRange(sample, sample));
                    break;
                case 2:
                    FixedDecimal start = new FixedDecimal(rangeParts[0]);
                    FixedDecimal end = new FixedDecimal(rangeParts[1]);
                    checkDecimal(sampleType2, start);
                    checkDecimal(sampleType2, end);
                    samples2.add(new FixedDecimalRange(start, end));
                    break;
                default: throw new IllegalArgumentException("Ill-formed number range: " + range);
                }
            }
            return new FixedDecimalSamples(sampleType2, Collections.unmodifiableSet(samples2), bounded2);
        }

        private static void checkDecimal(SampleType sampleType2, FixedDecimal sample) {
            if ((sampleType2 == SampleType.INTEGER) != (sample.getVisibleDecimalDigitCount() == 0)) {
                throw new IllegalArgumentException("Ill-formed number range: " + sample);
            }
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public Set<Double> addSamples(Set<Double> result) {
            for (FixedDecimalRange item : samples) {
                // we have to convert to longs so we don't get strange double issues
                long startDouble = item.start.getShiftedValue();
                long endDouble = item.end.getShiftedValue();

                for (long d = startDouble; d <= endDouble; d += 1) {
                    result.add(d/(double)item.start.baseFactor);
                }
            }
            return result;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("@").append(sampleType.toString().toLowerCase(Locale.ENGLISH));
            boolean first = true;
            for (FixedDecimalRange item : samples) {
                if (first) {
                    first = false;
                } else {
                    b.append(",");
                }
                b.append(' ').append(item);
            }
            if (!bounded) {
                b.append(", …");
            }
            return b.toString();
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public Set<FixedDecimalRange> getSamples() {
            return samples;
        }

        /**
         * icu_annot::internal CLDR
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        public void getStartEndSamples(Set<FixedDecimal> target) {
            for (FixedDecimalRange item : samples) {
                target.add(item.start);
                target.add(item.end);
            }
        }
    }

    /*
     * A constraint on a number.
     */
    private interface Constraint extends Serializable {
        /*
         * Returns true if the number fulfills the constraint.
         * @param n the number to test, >= 0.
         */
        boolean isFulfilled(FixedDecimal n);

        /*
         * Returns false if an unlimited number of values fulfills the
         * constraint.
         */
        boolean isLimited(SampleType sampleType);
    }

    private static final boolean isBreakAndIgnore(char c) {
        return c <= 0x20 && (c == 0x20 || c == 9 || c == 0xa || c == 0xc || c == 0xd);
    }
    private static final boolean isBreakAndKeep(char c) {
        return c <= '=' && c >= '!' && (c == '!' || c == '%' || c == ',' || c == '.' || c == '=');
    }
    static class SimpleTokenizer {
        // static final UnicodeSet BREAK_AND_IGNORE = new UnicodeSet(0x09, 0x0a, 0x0c, 0x0d, 0x20, 0x20).freeze();
        // static final UnicodeSet BREAK_AND_KEEP = new UnicodeSet('!', '!', '%', '%', ',', ',', '.', '.', '=', '=').freeze();
        static String[] split(String source) {
            int last = -1;
            List<String> result = new ArrayList<>();
            for (int i = 0; i < source.length(); ++i) {
                char ch = source.charAt(i);
                if (isBreakAndIgnore(ch) /* BREAK_AND_IGNORE.contains(ch) */) {
                    if (last >= 0) {
                        result.add(source.substring(last,i));
                        last = -1;
                    }
                } else if (isBreakAndKeep(ch) /* BREAK_AND_KEEP.contains(ch) */) {
                    if (last >= 0) {
                        result.add(source.substring(last,i));
                    }
                    result.add(source.substring(i,i+1));
                    last = -1;
                } else if (last < 0) {
                    last = i;
                }
            }
            if (last >= 0) {
                result.add(source.substring(last));
            }
            return result.toArray(new String[result.size()]);
        }
    }

    /*
     * syntax:
     * condition :       or_condition
     *                   and_condition
     * or_condition :    and_condition 'or' condition
     * and_condition :   relation
     *                   relation 'and' relation
     * relation :        in_relation
     *                   within_relation
     * in_relation :     not? expr not? in not? range
     * within_relation : not? expr not? 'within' not? range
     * not :             'not'
     *                   '!'
     * expr :            'n'
     *                   'n' mod value
     * mod :             'mod'
     *                   '%'
     * in :              'in'
     *                   'is'
     *                   '='
     *                   '≠'
     * value :           digit+
     * digit :           0|1|2|3|4|5|6|7|8|9
     * range :           value'..'value
     */
    private static Constraint parseConstraint(String description)
            throws ParseException {

        Constraint result = null;
        String[] or_together = OR_SEPARATED.split(description, 0);
        for (int i = 0; i < or_together.length; ++i) {
            Constraint andConstraint = null;
            String[] and_together = AND_SEPARATED.split(or_together[i], 0);
            for (int j = 0; j < and_together.length; ++j) {
                Constraint newConstraint = NO_CONSTRAINT;

                String condition = and_together[j].trim();
                String[] tokens = SimpleTokenizer.split(condition);

                int mod = 0;
                boolean inRange = true;
                boolean integersOnly = true;
                double lowBound = Long.MAX_VALUE;
                double highBound = Long.MIN_VALUE;
                long[] vals = null;

                int x = 0;
                String t = tokens[x++];
                boolean hackForCompatibility = false;
                Operand operand;
                try {
                    operand = FixedDecimal.getOperand(t);
                } catch (Exception e) {
                    throw unexpected(t, condition);
                }
                if (x < tokens.length) {
                    t = tokens[x++];
                    if ("mod".equals(t) || "%".equals(t)) {
                        mod = Integer.parseInt(tokens[x++]);
                        t = nextToken(tokens, x++, condition);
                    }
                    if ("not".equals(t)) {
                        inRange = !inRange;
                        t = nextToken(tokens, x++, condition);
                        if ("=".equals(t)) {
                            throw unexpected(t, condition);
                        }
                    } else if ("!".equals(t)) {
                        inRange = !inRange;
                        t = nextToken(tokens, x++, condition);
                        if (!"=".equals(t)) {
                            throw unexpected(t, condition);
                        }
                    }
                    if ("is".equals(t) || "in".equals(t) || "=".equals(t)) {
                        hackForCompatibility = "is".equals(t);
                        if (hackForCompatibility && !inRange) {
                            throw unexpected(t, condition);
                        }
                        t = nextToken(tokens, x++, condition);
                    } else if ("within".equals(t)) {
                        integersOnly = false;
                        t = nextToken(tokens, x++, condition);
                    } else {
                        throw unexpected(t, condition);
                    }
                    if ("not".equals(t)) {
                        if (!hackForCompatibility && !inRange) {
                            throw unexpected(t, condition);
                        }
                        inRange = !inRange;
                        t = nextToken(tokens, x++, condition);
                    }

                    List<Long> valueList = new ArrayList<>();

                    // the token t is always one item ahead
                    while (true) {
                        long low = Long.parseLong(t);
                        long high = low;
                        if (x < tokens.length) {
                            t = nextToken(tokens, x++, condition);
                            if (t.equals(".")) {
                                t = nextToken(tokens, x++, condition);
                                if (!t.equals(".")) {
                                    throw unexpected(t, condition);
                                }
                                t = nextToken(tokens, x++, condition);
                                high = Long.parseLong(t);
                                if (x < tokens.length) {
                                    t = nextToken(tokens, x++, condition);
                                    if (!t.equals(",")) { // adjacent number: 1 2
                                        // no separator, fail
                                        throw unexpected(t, condition);
                                    }
                                }
                            } else if (!t.equals(",")) { // adjacent number: 1 2
                                // no separator, fail
                                throw unexpected(t, condition);
                            }
                        }
                        // at this point, either we are out of tokens, or t is ','
                        if (low > high) {
                            throw unexpected(low + "~" + high, condition);
                        } else if (mod != 0 && high >= mod) {
                            throw unexpected(high + ">mod=" + mod, condition);
                        }
                        valueList.add(low);
                        valueList.add(high);
                        lowBound = Math.min(lowBound, low);
                        highBound = Math.max(highBound, high);
                        if (x >= tokens.length) {
                            break;
                        }
                        t = nextToken(tokens, x++, condition);
                    }

                    if (t.equals(",")) {
                        throw unexpected(t, condition);
                    }

                    if (valueList.size() == 2) {
                        vals = null;
                    } else {
                        vals = new long[valueList.size()];
                        for (int k = 0; k < vals.length; ++k) {
                            vals[k] = valueList.get(k);
                        }
                    }

                    // Hack to exclude "is not 1,2"
                    if (lowBound != highBound && hackForCompatibility && !inRange) {
                        throw unexpected("is not <range>", condition);
                    }

                    newConstraint =
                            new RangeConstraint(mod, inRange, operand, integersOnly, lowBound, highBound, vals);
                }

                if (andConstraint == null) {
                    andConstraint = newConstraint;
                } else {
                    andConstraint = new AndConstraint(andConstraint,
                            newConstraint);
                }
            }

            if (result == null) {
                result = andConstraint;
            } else {
                result = new OrConstraint(result, andConstraint);
            }
        }
        return result;
    }

    static final Pattern AT_SEPARATED = Pattern.compile("\\s*\\Q\\E@\\s*");
    static final Pattern OR_SEPARATED = Pattern.compile("\\s*or\\s*");
    static final Pattern AND_SEPARATED = Pattern.compile("\\s*and\\s*");
    static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*");
    static final Pattern DOTDOT_SEPARATED = Pattern.compile("\\s*\\Q..\\E\\s*");
    static final Pattern TILDE_SEPARATED = Pattern.compile("\\s*~\\s*");
    static final Pattern SEMI_SEPARATED = Pattern.compile("\\s*;\\s*");


    /* Returns a parse exception wrapping the token and context strings. */
    private static ParseException unexpected(String token, String context) {
        return new ParseException("unexpected token '" + token +
                "' in '" + context + "'", -1);
    }

    /*
     * Returns the token at x if available, else throws a parse exception.
     */
    private static String nextToken(String[] tokens, int x, String context)
            throws ParseException {
        if (x < tokens.length) {
            return tokens[x];
        }
        throw new ParseException("missing token at end of '" + context + "'", -1);
    }

    /*
     * Syntax:
     * rule : keyword ':' condition
     * keyword: <identifier>
     */
    private static Rule parseRule(String description) throws ParseException {
        if (description.length() == 0) {
            return DEFAULT_RULE;
        }

        description = description.toLowerCase(Locale.ENGLISH);

        int x = description.indexOf(':');
        if (x == -1) {
            throw new ParseException("missing ':' in rule description '" +
                    description + "'", 0);
        }

        String keyword = description.substring(0, x).trim();
        if (!isValidKeyword(keyword)) {
            throw new ParseException("keyword '" + keyword +
                    " is not valid", 0);
        }

        description = description.substring(x+1).trim();
        String[] constraintOrSamples = AT_SEPARATED.split(description, 0);
        boolean sampleFailure = false;
        FixedDecimalSamples integerSamples = null, decimalSamples = null;
        switch (constraintOrSamples.length) {
        case 1: break;
        case 2:
            integerSamples = FixedDecimalSamples.parse(constraintOrSamples[1]);
            if (integerSamples.sampleType == SampleType.DECIMAL) {
                decimalSamples = integerSamples;
                integerSamples = null;
            }
            break;
        case 3:
            integerSamples = FixedDecimalSamples.parse(constraintOrSamples[1]);
            decimalSamples = FixedDecimalSamples.parse(constraintOrSamples[2]);
            if (integerSamples.sampleType != SampleType.INTEGER || decimalSamples.sampleType != SampleType.DECIMAL) {
                throw new IllegalArgumentException("Must have @integer then @decimal in " + description);
            }
            break;
        default:
            throw new IllegalArgumentException("Too many samples in " + description);
        }
        if (sampleFailure) {
            throw new IllegalArgumentException("Ill-formed samples—'@' characters.");
        }

        // 'other' is special, and must have no rules; all other keywords must have rules.
        boolean isOther = keyword.equals("other");
        if (isOther != (constraintOrSamples[0].length() == 0)) {
            throw new IllegalArgumentException("The keyword 'other' must have no constraints, just samples.");
        }

        Constraint constraint;
        if (isOther) {
            constraint = NO_CONSTRAINT;
        } else {
            constraint = parseConstraint(constraintOrSamples[0]);
        }
        return new Rule(keyword, constraint, integerSamples, decimalSamples);
    }


    /*
     * Syntax:
     * rules : rule
     *         rule ';' rules
     */
    private static RuleList parseRuleChain(String description)
            throws ParseException {
        RuleList result = new RuleList();
        // remove trailing ;
        if (description.endsWith(";")) {
            description = description.substring(0,description.length()-1);
        }
        String[] rules = SEMI_SEPARATED.split(description, 0);
        for (int i = 0; i < rules.length; ++i) {
            Rule rule = parseRule(rules[i].trim());
            result.hasExplicitBoundingInfo |= rule.integerSamples != null || rule.decimalSamples != null;
            result.addRule(rule);
        }
        return result.finish();
    }

    /*
     * An implementation of Constraint representing a modulus,
     * a range of values, and include/exclude. Provides lots of
     * convenience factory methods.
     */
    private static class RangeConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;

        private final int mod;
        private final boolean inRange;
        private final boolean integersOnly;
        private final double lowerBound;
        private final double upperBound;
        private final long[] range_list;
        private final Operand operand;

        RangeConstraint(int mod, boolean inRange, Operand operand, boolean integersOnly,
                double lowBound, double highBound, long[] vals) {
            this.mod = mod;
            this.inRange = inRange;
            this.integersOnly = integersOnly;
            this.lowerBound = lowBound;
            this.upperBound = highBound;
            this.range_list = vals;
            this.operand = operand;
        }

        @Override
        public boolean isFulfilled(FixedDecimal number) {
            double n = number.get(operand);
            if ((integersOnly && (n - (long)n) != 0.0)
                    || (operand == Operand.j && number.visibleDecimalDigitCount != 0)) {
                return !inRange;
            }
            if (mod != 0) {
                n = n % mod;    // java % handles double numerator the way we want
            }
            boolean test = n >= lowerBound && n <= upperBound;
            if (test && range_list != null) {
                test = false;
                for (int i = 0; !test && i < range_list.length; i += 2) {
                    test = n >= range_list[i] && n <= range_list[i+1];
                }
            }
            return inRange == test;
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            boolean valueIsZero = lowerBound == upperBound && lowerBound == 0d;
            boolean hasDecimals =
                    (operand == Operand.v || operand == Operand.w || operand == Operand.f || operand == Operand.t)
                    && inRange != valueIsZero; // either NOT f = zero or f = non-zero
            switch (sampleType) {
            case INTEGER:
                return hasDecimals // will be empty
                        || ((operand == Operand.n || operand == Operand.i || operand == Operand.j)
                        && mod == 0
                        && inRange);

            case DECIMAL:
                return  (!hasDecimals || operand == Operand.n || operand == Operand.j)
                        && (integersOnly || lowerBound == upperBound)
                        && mod == 0
                        && inRange;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(operand);
            if (mod != 0) {
                result.append(" % ").append(mod);
            }
            boolean isList = lowerBound != upperBound;
            result.append(
                    !isList ? (inRange ? " = " : " != ")
                            : integersOnly ? (inRange ? " = " : " != ")
                                    : (inRange ? " within " : " not within ")
                    );
            if (range_list != null) {
                for (int i = 0; i < range_list.length; i += 2) {
                    addRange(result, range_list[i], range_list[i+1], i != 0);
                }
            } else {
                addRange(result, lowerBound, upperBound, false);
            }
            return result.toString();
        }
    }

    private static void addRange(StringBuilder result, double lb, double ub, boolean addSeparator) {
        if (addSeparator) {
            result.append(",");
        }
        if (lb == ub) {
            result.append(format(lb));
        } else {
            result.append(format(lb) + ".." + format(ub));
        }
    }

    private static String format(double lb) {
        long lbi = (long) lb;
        return lb == lbi ? String.valueOf(lbi) : String.valueOf(lb);
    }

    /* Convenience base class for and/or constraints. */
    private static abstract class BinaryConstraint implements Constraint,
    Serializable {
        private static final long serialVersionUID = 1;
        protected final Constraint a;
        protected final Constraint b;

        protected BinaryConstraint(Constraint a, Constraint b) {
            this.a = a;
            this.b = b;
        }
    }

    /* A constraint representing the logical and of two constraints. */
    private static class AndConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 7766999779862263523L;

        AndConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        @Override
        public boolean isFulfilled(FixedDecimal n) {
            return a.isFulfilled(n)
                    && b.isFulfilled(n);
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            // we ignore the case where both a and b are unlimited but no values
            // satisfy both-- we still consider this 'unlimited'
            return a.isLimited(sampleType)
                    || b.isLimited(sampleType);
        }

        @Override
        public String toString() {
            return a.toString() + " and " + b.toString();
        }
    }

    /* A constraint representing the logical or of two constraints. */
    private static class OrConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 1405488568664762222L;

        OrConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        @Override
        public boolean isFulfilled(FixedDecimal n) {
            return a.isFulfilled(n)
                    || b.isFulfilled(n);
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            return a.isLimited(sampleType)
                    && b.isLimited(sampleType);
        }

        @Override
        public String toString() {
            return a.toString() + " or " + b.toString();
        }
    }

    /*
     * Implementation of Rule that uses a constraint.
     * Provides 'and' and 'or' to combine constraints.  Immutable.
     */
    private static class Rule implements Serializable {
        private static final long serialVersionUID = 1;
        private final String keyword;
        private final Constraint constraint;
        private final FixedDecimalSamples integerSamples;
        private final FixedDecimalSamples decimalSamples;

        public Rule(String keyword, Constraint constraint, FixedDecimalSamples integerSamples, FixedDecimalSamples decimalSamples) {
            this.keyword = keyword;
            this.constraint = constraint;
            this.integerSamples = integerSamples;
            this.decimalSamples = decimalSamples;
        }

        @SuppressWarnings("unused")
        public Rule and(Constraint c) {
            return new Rule(keyword, new AndConstraint(constraint, c), integerSamples, decimalSamples);
        }

        @SuppressWarnings("unused")
        public Rule or(Constraint c) {
            return new Rule(keyword, new OrConstraint(constraint, c), integerSamples, decimalSamples);
        }

        public String getKeyword() {
            return keyword;
        }

        public boolean appliesTo(FixedDecimal n) {
            return constraint.isFulfilled(n);
        }

        public boolean isLimited(SampleType sampleType) {
            return constraint.isLimited(sampleType);
        }

        @Override
        public String toString() {
            return keyword + ": " + constraint.toString()
                    + (integerSamples == null ? "" : " " + integerSamples.toString())
                    + (decimalSamples == null ? "" : " " + decimalSamples.toString());
        }

        /**
         * {@inheritDoc}
         * icu_annot::stable ICU 3.8
         */
        @Override
        public int hashCode() {
            return keyword.hashCode() ^ constraint.hashCode();
        }

        public String getConstraint() {
            return constraint.toString();
        }
    }

    private static class RuleList implements Serializable {
        private boolean hasExplicitBoundingInfo = false;
        private static final long serialVersionUID = 1;
        private final List<Rule> rules = new ArrayList<>();

        public RuleList addRule(Rule nextRule) {
            String keyword = nextRule.getKeyword();
            for (Rule rule : rules) {
                if (keyword.equals(rule.getKeyword())) {
                    throw new IllegalArgumentException("Duplicate keyword: " + keyword);
                }
            }
            rules.add(nextRule);
            return this;
        }

        public RuleList finish() throws ParseException {
            // make sure that 'other' is present, and at the end.
            Rule otherRule = null;
            for (Iterator<Rule> it = rules.iterator(); it.hasNext();) {
                Rule rule = it.next();
                if ("other".equals(rule.getKeyword())) {
                    otherRule = rule;
                    it.remove();
                }
            }
            if (otherRule == null) {
                otherRule = parseRule("other:"); // make sure we have always have an 'other' a rule
            }
            rules.add(otherRule);
            return this;
        }

        private Rule selectRule(FixedDecimal n) {
            for (Rule rule : rules) {
                if (rule.appliesTo(n)) {
                    return rule;
                }
            }
            return null;
        }

        public String select(FixedDecimal n) {
            if (Double.isInfinite(n.source) || Double.isNaN(n.source)) {
                return KEYWORD_OTHER;
            }
            Rule r = selectRule(n);
            return r.getKeyword();
        }

        public Set<String> getKeywords() {
            Set<String> result = new LinkedHashSet<>();
            for (Rule rule : rules) {
                result.add(rule.getKeyword());
            }
            // since we have explicit 'other', we don't need this.
            //result.add(KEYWORD_OTHER);
            return result;
        }

        public boolean isLimited(String keyword, SampleType sampleType) {
            if (hasExplicitBoundingInfo) {
                FixedDecimalSamples mySamples = getDecimalSamples(keyword, sampleType);
                return mySamples == null ? true : mySamples.bounded;
            }

            return computeLimited(keyword, sampleType);
        }

        public boolean computeLimited(String keyword, SampleType sampleType) {
            // if all rules with this keyword are limited, it's limited,
            // and if there's no rule with this keyword, it's unlimited
            boolean result = false;
            for (Rule rule : rules) {
                if (keyword.equals(rule.getKeyword())) {
                    if (!rule.isLimited(sampleType)) {
                        return false;
                    }
                    result = true;
                }
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Rule rule : rules) {
                if (builder.length() != 0) {
                    builder.append(CATEGORY_SEPARATOR);
                }
                builder.append(rule);
            }
            return builder.toString();
        }

        public String getRules(String keyword) {
            for (Rule rule : rules) {
                if (rule.getKeyword().equals(keyword)) {
                    return rule.getConstraint();
                }
            }
            return null;
        }

        public boolean select(FixedDecimal sample, String keyword) {
            for (Rule rule : rules) {
                if (rule.getKeyword().equals(keyword) && rule.appliesTo(sample)) {
                    return true;
                }
            }
            return false;
        }

        public FixedDecimalSamples getDecimalSamples(String keyword, SampleType sampleType) {
            for (Rule rule : rules) {
                if (rule.getKeyword().equals(keyword)) {
                    return sampleType == SampleType.INTEGER ? rule.integerSamples : rule.decimalSamples;
                }
            }
            return null;
        }
    }

    /**
     * This API is ICU internal only.
     * icu_annot::internal
     */
    // @Deprecated: in fact internal ICU
    public enum StandardPluralCategories {
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        zero,
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        one,
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        two,
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        few,
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        many,
        /**
         * icu_annot::internal
         * This API is ICU internal only.
         */
        // @Deprecated: in fact internal ICU
        other;
        static StandardPluralCategories forString(String s) {
            StandardPluralCategories a;
            try {
                a = valueOf(s);
            } catch (Exception e) {
                return null;
            }
            return a;
        }
    }

    @SuppressWarnings("unused")
    private boolean addConditional(Set<FixedDecimal> toAddTo, Set<FixedDecimal> others, double trial) {
        boolean added;
        FixedDecimal toAdd = new FixedDecimal(trial);
        if (!toAddTo.contains(toAdd) && !others.contains(toAdd)) {
            others.add(toAdd);
            added = true;
        } else {
            added = false;
        }
        return added;
    }



    // -------------------------------------------------------------------------
    // Static class methods.
    // -------------------------------------------------------------------------

    /**
     * Provides access to the predefined cardinal-number <code>PluralRules</code> for a given
     * {@link java.util.Locale}.
     * Same as forLocale(locale, PluralType.CARDINAL).
     *
     * <p>ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>.
     * For these predefined rules, see CLDR page at
     * https://unicode-org.github.io/cldr-staging/charts/latest/supplemental/language_plural_rules.html
     *
     * @param locale The locale for which a <code>PluralRules</code> object is
     *   returned.
     * @return The predefined <code>PluralRules</code> object for this locale.
     *   If there's no predefined rules for this locale, the rules
     *   for the closest parent in the locale hierarchy that has one will
     *   be returned.  The final fallback always returns the default
     *   rules.
     * icu_annot::stable ICU 54
     */
    public static PluralRules forLocale(Locale locale) {
        return forLocale(locale, PluralType.CARDINAL);
    }

    /**
     * Provides access to the predefined <code>PluralRules</code> for a given
     * locale and the plural type.
     *
     * <p>ICU defines plural rules for many locales based on CLDR <i>Language Plural Rules</i>.
     * For these predefined rules, see CLDR page at
     * https://unicode-org.github.io/cldr-staging/charts/latest/supplemental/language_plural_rules.html
     *
     * @param locale The locale for which a <code>PluralRules</code> object is
     *   returned.
     * @param type The plural type (e.g., cardinal or ordinal).
     * @return The predefined <code>PluralRules</code> object for this locale.
     *   If there's no predefined rules for this locale, the rules
     *   for the closest parent in the locale hierarchy that has one will
     *   be returned.  The final fallback always returns the default
     *   rules.
     * icu_annot::stable ICU 50
     */
    public static PluralRules forLocale(Locale locale, PluralType type) {
        return Factory.getDefaultFactory().forLocale(locale, type);
    }

    /*
     * Checks whether a token is a valid keyword.
     *
     * @param token the token to be checked
     * @return true if the token is a valid keyword.
     */
    private static boolean isValidKeyword(String token) {
        // return ALLOWED_ID.containsAll(token);
        for (int i = 0; i < token.length(); ++i) {
            char c = token.charAt(i);
            if (!('a' <= c && c <= 'z')) {
                return false;
            }
        }
        return true;
    }

    /*
     * Creates a new <code>PluralRules</code> object.  Immutable.
     */
    private PluralRules(RuleList rules) {
        this.rules = rules;
        this.keywords = Collections.unmodifiableSet(rules.getKeywords());
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 3.8
     */
    @Override
    public int hashCode() {
        return rules.hashCode();
    }

    /**
     * Given a floating-point number, returns the keyword of the first rule
     * that applies to the number.
     *
     * @param number The number for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * icu_annot::stable ICU 4.0
     */
    public String select(double number) {
        return rules.select(new FixedDecimal(number));
    }

    /**
     * Given a number, returns the keyword of the first rule that applies to
     * the number.
     *
     * @param number The number for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * icu_annot::internal Visible For Testing
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public String select(double number, int countVisibleFractionDigits, long fractionaldigits) {
        return rules.select(new FixedDecimal(number, countVisibleFractionDigits, fractionaldigits));
    }

    /**
     * Given a number information, returns the keyword of the first rule that applies to
     * the number.
     *
     * @param number The number information for which the rule has to be determined.
     * @return The keyword of the selected rule.
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public String select(FixedDecimal number) {
        return rules.select(number);
    }

    /**
     * Given a number information, and keyword, return whether the keyword would match the number.
     *
     * @param sample The number information for which the rule has to be determined.
     * @param keyword The keyword to filter on
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public boolean matches(FixedDecimal sample, String keyword) {
        return rules.select(sample, keyword);
    }

    /**
     * Returns a set of all rule keywords used in this <code>PluralRules</code>
     * object.  The rule "other" is always present by default.
     *
     * @return The set of keywords.
     * icu_annot::stable ICU 3.8
     */
    public Set<String> getKeywords() {
        return keywords;
    }

    /**
     * Returns the unique value that this keyword matches, or {@link #NO_UNIQUE_VALUE}
     * if the keyword matches multiple values or is not defined for this PluralRules.
     *
     * @param keyword the keyword to check for a unique value
     * @return The unique value for the keyword, or NO_UNIQUE_VALUE.
     * icu_annot::stable ICU 4.8
     */
    public double getUniqueKeywordValue(String keyword) {
        Collection<Double> values = getAllKeywordValues(keyword);
        if (values != null && values.size() == 1) {
            return values.iterator().next();
        }
        return NO_UNIQUE_VALUE;
    }

    /**
     * Returns all the values that trigger this keyword, or null if the number of such
     * values is unlimited.
     *
     * @param keyword the keyword
     * @return the values that trigger this keyword, or null.  The returned collection
     * is immutable. It will be empty if the keyword is not defined.
     * icu_annot::stable ICU 4.8
     */
    public Collection<Double> getAllKeywordValues(String keyword) {
        return getAllKeywordValues(keyword, SampleType.INTEGER);
    }

    /**
     * Returns all the values that trigger this keyword, or null if the number of such
     * values is unlimited.
     *
     * @param keyword the keyword
     * @param type the type of samples requested, INTEGER or DECIMAL
     * @return the values that trigger this keyword, or null.  The returned collection
     * is immutable. It will be empty if the keyword is not defined.
     *
     * icu_annot::internal Visible For Testing
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public Collection<Double> getAllKeywordValues(String keyword, SampleType type) {
        if (!isLimited(keyword, type)) {
            return null;
        }
        Collection<Double> samples = getSamples(keyword, type);
        return samples == null ? null : Collections.unmodifiableCollection(samples);
    }

    /**
     * Returns a list of integer values for which select() would return that keyword,
     * or null if the keyword is not defined. The returned collection is unmodifiable.
     * The returned list is not complete, and there might be additional values that
     * would return the keyword.
     *
     * @param keyword the keyword to test
     * @return a list of values matching the keyword.
     * icu_annot::stable ICU 4.8
     */
    public Collection<Double> getSamples(String keyword) {
        return getSamples(keyword, SampleType.INTEGER);
    }

    /**
     * Returns a list of values for which select() would return that keyword,
     * or null if the keyword is not defined.
     * The returned collection is unmodifiable.
     * The returned list is not complete, and there might be additional values that
     * would return the keyword. The keyword might be defined, and yet have an empty set of samples,
     * IF there are samples for the other sampleType.
     *
     * @param keyword the keyword to test
     * @param sampleType the type of samples requested, INTEGER or DECIMAL
     * @return a list of values matching the keyword.
     * icu_annot::internal CLDR
     * @deprecated ICU internal only
     */
    // @Deprecated: in fact internal ICU
    public Collection<Double> getSamples(String keyword, SampleType sampleType) {
        if (!keywords.contains(keyword)) {
            return null;
        }
        Set<Double> result = new TreeSet<>();

        if (rules.hasExplicitBoundingInfo) {
            FixedDecimalSamples samples = rules.getDecimalSamples(keyword, sampleType);
            return samples == null ? Collections.unmodifiableSet(result)
                    : Collections.unmodifiableSet(samples.addSamples(result));
        }

        // hack in case the rule is created without explicit samples
        int maxCount = isLimited(keyword, sampleType) ? Integer.MAX_VALUE : 20;

        switch (sampleType) {
        case INTEGER:
            for (int i = 0; i < 200; ++i) {
                if (!addSample(keyword, i, maxCount, result)) {
                    break;
                }
            }
            addSample(keyword, 1000000, maxCount, result); // hack for Welsh
            break;
        case DECIMAL:
            for (int i = 0; i < 2000; ++i) {
                if (!addSample(keyword, new FixedDecimal(i/10d, 1), maxCount, result)) {
                    break;
                }
            }
            addSample(keyword, new FixedDecimal(1000000d, 1), maxCount, result); // hack for Welsh
            break;
        }
        return result.size() == 0 ? null : Collections.unmodifiableSet(result);
    }

    private boolean addSample(String keyword, Number sample, int maxCount, Set<Double> result) {
        String selectedKeyword = sample instanceof FixedDecimal ? select((FixedDecimal)sample) : select(sample.doubleValue());
        if (selectedKeyword.equals(keyword)) {
            result.add(sample.doubleValue());
            if (--maxCount < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of values for which select() would return that keyword,
     * or null if the keyword is not defined or no samples are available.
     * The returned collection is unmodifiable.
     * The returned list is not complete, and there might be additional values that
     * would return the keyword.
     *
     * @param keyword the keyword to test
     * @param sampleType the type of samples requested, INTEGER or DECIMAL
     * @return a list of values matching the keyword.
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public FixedDecimalSamples getDecimalSamples(String keyword, SampleType sampleType) {
        return rules.getDecimalSamples(keyword, sampleType);
    }

    /**
     * Returns the set of locales for which PluralRules are known.
     * @return the set of locales for which PluralRules are known, as a list
     * icu_annot::draft ICU 4.2 (retain)
    public static ULocale[] getAvailableULocales() {
        return Factory.getDefaultFactory().getAvailableULocales();
    }
     */

    /**
     * Returns the 'functionally equivalent' locale with respect to
     * plural rules.  Calling PluralRules.forLocale with the functionally equivalent
     * locale, and with the provided locale, returns rules that behave the same.
     * <br/>
     * All locales with the same functionally equivalent locale have
     * plural rules that behave the same.  This is not exhaustive;
     * there may be other locales whose plural rules behave the same
     * that do not have the same equivalent locale.
     *
     * @param locale the locale to check
     * @param isAvailable if not null and of length > 0, this will hold 'true' at
     * index 0 if locale is directly defined (without fallback) as having plural rules
     * @return the functionally-equivalent locale
     * icu_annot::draft ICU 4.2 (retain)
    public static ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
        return Factory.getDefaultFactory().getFunctionalEquivalent(locale, isAvailable);
    }
     */

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 3.8
     */
    @Override
    public String toString() {
        return rules.toString();
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 3.8
     */
    @Override
    public boolean equals(Object rhs) {
//        return rhs instanceof PluralRules && equals((PluralRules)rhs);
        return rhs instanceof PluralRules && toString().equals(rhs.toString());
    }

    /**
     * Returns true if rhs is equal to this.
     * @param rhs the PluralRules to compare to.
     * @return true if this and rhs are equal.
     * icu_annot::stable ICU 3.8
     */
    // TODO Optimize this
//    public boolean equals(PluralRules rhs) {
//        return rhs != null && toString().equals(rhs.toString());
//    }

    /**
     * Status of the keyword for the rules, given a set of explicit values.
     *
     * icu_annot::draft ICU 50
     */
    public enum KeywordStatus {
        /**
         * The keyword is not valid for the rules.
         *
         * icu_annot::draft ICU 50
         */
        INVALID,
        /**
         * The keyword is valid, but unused (it is covered by the explicit values, OR has no values for the given {@link SampleType}).
         *
         * icu_annot::draft ICU 50
         */
        SUPPRESSED,
        /**
         * The keyword is valid, used, and has a single possible value (before considering explicit values).
         *
         * icu_annot::draft ICU 50
         */
        UNIQUE,
        /**
         * The keyword is valid, used, not unique, and has a finite set of values.
         *
         * icu_annot::draft ICU 50
         */
        BOUNDED,
        /**
         * The keyword is valid but not bounded; there indefinitely many matching values.
         *
         * icu_annot::draft ICU 50
         */
        UNBOUNDED
    }

    /**
     * Find the status for the keyword, given a certain set of explicit values.
     *
     * @param keyword
     *            the particular keyword (call rules.getKeywords() to get the valid ones)
     * @param offset
     *            the offset used, or 0.0d if not. Internally, the offset is subtracted from each explicit value before
     *            checking against the keyword values.
     * @param explicits
     *            a set of Doubles that are used explicitly (eg [=0], "[=1]"). May be empty or null.
     * @param uniqueValue
     *            If non null, set to the unique value.
     * @return the KeywordStatus
     * icu_annot::draft ICU 50
     */
    public KeywordStatus getKeywordStatus(String keyword, int offset, Set<Double> explicits,
            Output<Double> uniqueValue) {
        return getKeywordStatus(keyword, offset, explicits, uniqueValue, SampleType.INTEGER);
    }
    /**
     * Find the status for the keyword, given a certain set of explicit values.
     *
     * @param keyword
     *            the particular keyword (call rules.getKeywords() to get the valid ones)
     * @param offset
     *            the offset used, or 0.0d if not. Internally, the offset is subtracted from each explicit value before
     *            checking against the keyword values.
     * @param explicits
     *            a set of Doubles that are used explicitly (eg [=0], "[=1]"). May be empty or null.
     * @param sampleType
     *            request KeywordStatus relative to INTEGER or DECIMAL values
     * @param uniqueValue
     *            If non null, set to the unique value.
     * @return the KeywordStatus
     * icu_annot::internal Visible For Testing
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public KeywordStatus getKeywordStatus(String keyword, int offset, Set<Double> explicits,
            Output<Double> uniqueValue, SampleType sampleType) {
        if (uniqueValue != null) {
            uniqueValue.value = null;
        }

        if (!keywords.contains(keyword)) {
            return KeywordStatus.INVALID;
        }

        if (!isLimited(keyword, sampleType)) {
            return KeywordStatus.UNBOUNDED;
        }

        Collection<Double> values = getSamples(keyword, sampleType);

        int originalSize = values.size();

        if (explicits == null) {
            explicits = Collections.emptySet();
        }

        // Quick check on whether there are multiple elements

        if (originalSize > explicits.size()) {
            if (originalSize == 1) {
                if (uniqueValue != null) {
                    uniqueValue.value = values.iterator().next();
                }
                return KeywordStatus.UNIQUE;
            }
            return KeywordStatus.BOUNDED;
        }

        // Compute if the quick test is insufficient.

        HashSet<Double> subtractedSet = new HashSet<>(values);
        for (Double explicit : explicits) {
            subtractedSet.remove(explicit - offset);
        }
        if (subtractedSet.size() == 0) {
            return KeywordStatus.SUPPRESSED;
        }

        if (uniqueValue != null && subtractedSet.size() == 1) {
            uniqueValue.value = subtractedSet.iterator().next();
        }

        return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
    }

    /**
     * icu_annot::internal CLDR
     * This API is ICU internal only.
     */
    // @Deprecated: in fact internal ICU
    public String getRules(String keyword) {
        return rules.getRules(keyword);
    }
    /*
    private void writeObject(
            ObjectOutputStream out)
                    throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in
            ) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new PluralRulesSerialProxy(toString());
    }
    */
    /**
     * icu_annot::internal CLDR
     * @deprecated internal
     */
    // @Deprecated: in fact internal ICU
    public int compareTo(PluralRules other) {
        return toString().compareTo(other.toString());
    }

    Boolean isLimited(String keyword) {
        return rules.isLimited(keyword, SampleType.INTEGER);
    }

    /**
     * icu_annot::internal Visible For Testing
     * @deprecated internal
     */
    // @Deprecated: in fact internal ICU
    public boolean isLimited(String keyword, SampleType sampleType) {
        return rules.isLimited(keyword, sampleType);
    }

    /**
     * icu_annot::internal CLDR
     * @deprecated internal
     */
    // @Deprecated: in fact internal ICU
    public boolean computeLimited(String keyword, SampleType sampleType) {
        return rules.computeLimited(keyword, sampleType);
    }
}
