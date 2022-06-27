/*
 **********************************************************************
 * Copyright (c) 2004-2014, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Alan Liu
 * Created: April 6, 2004
 * Since: ICU 3.0
 **********************************************************************
 */
package androidx.core.i18n.messageformat_icu.simple;

import android.content.Context;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.RestrictTo;
import androidx.core.i18n.DateTimeFormatter;
import androidx.core.i18n.DateTimeFormatterJdkStyleOptions;
import androidx.core.i18n.DateTimeFormatterSkeletonOptions;
import androidx.core.i18n.messageformat_icu.impl.PatternProps;
import androidx.core.i18n.messageformat_icu.simple.PluralRules.PluralType;
import androidx.core.i18n.messageformat_icu.text.MessagePattern;
import androidx.core.i18n.messageformat_icu.text.MessagePattern.ArgType;
import androidx.core.i18n.messageformat_icu.text.MessagePattern.Part;
import androidx.core.i18n.messageformat_icu.text.SelectFormat;
import androidx.core.i18n.messageformat_icu.util.ICUUncheckedIOException;

/**
 * {icu_annot::icuenhanced java.text.MessageFormat}.{icu_annot::icu _usage_}
 *
 * <p>MessageFormat prepares strings for display to users,
 * with optional arguments (variables/placeholders).
 * The arguments can occur in any order, which is necessary for translation
 * into languages with different grammars.
 *
 * <p>A MessageFormat is constructed from a <em>pattern</em> string
 * with arguments in {curly braces} which will be replaced by formatted values.
 *
 * <p><code>MessageFormat</code> differs from the other <code>Format</code>
 * classes in that you create a <code>MessageFormat</code> object with one
 * of its constructors (not with a <code>getInstance</code> style factory
 * method). Factory methods aren't necessary because <code>MessageFormat</code>
 * itself doesn't implement locale-specific behavior. Any locale-specific
 * behavior is defined by the pattern that you provide and the
 * subformats used for inserted arguments.
 *
 * <p>Arguments can be named (using identifiers) or numbered (using small ASCII-digit integers).
 * Some of the API methods work only with argument numbers and throw an exception
 * if the pattern has named arguments (see {@link #usesNamedArguments()}).
 *
 * <p>An argument might not specify any format type. In this case,
 * a Number value is formatted with a default (for the locale) NumberFormat,
 * a Date value is formatted with a default (for the locale) DateFormat,
 * and for any other value its toString() value is used.
 *
 * <p>An argument might specify a "simple" type for which the specified
 * Format object is created, cached and used.
 *
 * <p>An argument might have a "complex" type with nested MessageFormat sub-patterns.
 * During formatting, one of these sub-messages is selected according to the argument value
 * and recursively formatted.
 *
 * <p>After construction, a custom Format object can be set for
 * a top-level argument, overriding the default formatting and parsing behavior
 * for that argument.
 * However, custom formatting can be achieved more simply by writing
 * a typeless argument in the pattern string
 * and supplying it with a preformatted string value.
 *
 * <p>When formatting, MessageFormat takes a collection of argument values
 * and writes an output string.
 * The argument values may be passed as an array
 * (when the pattern contains only numbered arguments)
 * or as a Map (which works for both named and numbered arguments).
 *
 * <p>Each argument is matched with one of the input values by array index or map key
 * and formatted according to its pattern specification
 * (or using a custom Format object if one was set).
 * A numbered pattern argument is matched with a map key that contains that number
 * as an ASCII-decimal-digit string (without leading zero).
 *
 * <h3><a name="patterns">Patterns and Their Interpretation</a></h3>
 *
 * <code>MessageFormat</code> uses patterns of the following form:
 * <blockquote><pre>
 * message = messageText (argument messageText)*
 * argument = noneArg | simpleArg | complexArg
 * complexArg = choiceArg | pluralArg | selectArg | selectordinalArg
 *
 * noneArg = '{' argNameOrNumber '}'
 * simpleArg = '{' argNameOrNumber ',' argType [',' argStyle] '}'
 * choiceArg = '{' argNameOrNumber ',' "choice" ',' choiceStyle '}'
 * pluralArg = '{' argNameOrNumber ',' "plural" ',' pluralStyle '}'
 * selectArg = '{' argNameOrNumber ',' "select" ',' selectStyle '}'
 * selectordinalArg = '{' argNameOrNumber ',' "selectordinal" ',' pluralStyle '}'
 *
 * choiceStyle: see {@link ChoiceFormat}
 * pluralStyle: see {@link PluralFormat}
 * selectStyle: see {@link SelectFormat}
 *
 * argNameOrNumber = argName | argNumber
 * argName = [^[[:Pattern_Syntax:][:Pattern_White_Space:]]]+
 * argNumber = '0' | ('1'..'9' ('0'..'9')*)
 *
 * argType = "number" | "date" | "time" | "spellout" | "ordinal" | "duration"
 * argStyle = "short" | "medium" | "long" | "full" | "integer" | "currency" | "percent" | argStyleText | "::" argSkeletonText
 * </pre></blockquote>
 *
 * <ul>
 *   <li>{@code ::argSkeletonText} is only supported for {@code date} and {@code time},
 *       not {@code number}</li>
 *   <li>messageText can contain quoted literal strings including syntax characters.
 *       A quoted literal string begins with an ASCII apostrophe and a syntax character
 *       (usually a {curly brace}) and continues until the next single apostrophe.
 *       A double ASCII apostrophe inside or outside of a quoted string represents
 *       one literal apostrophe.
 *   <li>Quotable syntax characters are the {curly braces} in all messageText parts,
 *       plus the '#' sign in a messageText immediately inside a pluralStyle,
 *       and the '|' symbol in a messageText immediately inside a choiceStyle.
 *   <li>See also {@link MessagePattern.ApostropheMode}
 *   <li>In argStyleText, every single ASCII apostrophe begins and ends quoted literal text,
 *       and unquoted {curly braces} must occur in matched pairs.
 * </ul>
 *
 * <p>Recommendation: Use the real apostrophe (single quote) character \\u2019 for
 * human-readable text, and use the ASCII apostrophe (\\u0027 ' )
 * only in program syntax, like quoting in MessageFormat.
 * See the annotations for U+0027 Apostrophe in The Unicode Standard.
 *
 * <p>The <code>choice</code> argument type is deprecated.
 * Use <code>plural</code> arguments for proper plural selection,
 * and <code>select</code> arguments for simple selection among a fixed set of choices.
 *
 * <p>The <code>argType</code> and <code>argStyle</code> values are used to create
 * a <code>Format</code> instance for the format element. The following
 * table shows how the values map to Format instances. Combinations not
 * shown in the table are illegal. Any <code>argStyleText</code> must
 * be a valid pattern string for the Format subclass used.
 *
 * <table border=1>
 *    <tr>
 *       <th>argType
 *       <th>argStyle
 *       <th>resulting Format object
 *    <tr>
 *       <td colspan=2><i>(none)</i>
 *       <td><code>null</code>
 *    <tr>
 *       <td rowspan=5><code>number</code>
 *       <td><i>(none)</i>
 *       <td><code>NumberFormat.getInstance(getLocale())</code>
 *    <tr>
 *       <td><code>integer</code>
 *       <td><code>NumberFormat.getIntegerInstance(getLocale())</code>
 *    <tr>
 *       <td><code>currency</code>
 *       <td><code>NumberFormat.getCurrencyInstance(getLocale())</code>
 *    <tr>
 *       <td><code>percent</code>
 *       <td><code>NumberFormat.getPercentInstance(getLocale())</code>
 *    <tr>
 *       <td><i>argStyleText</i>
 *       <td><code>new DecimalFormat(argStyleText, new DecimalFormatSymbols(getLocale()))</code>
 *    <tr>
 *       <td rowspan=7><code>date</code>
 *       <td><i>(none)</i>
 *       <td><code>DateFormat.getDateInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>short</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.SHORT, getLocale())</code>
 *    <tr>
 *       <td><code>medium</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>long</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.LONG, getLocale())</code>
 *    <tr>
 *       <td><code>full</code>
 *       <td><code>DateFormat.getDateInstance(DateFormat.FULL, getLocale())</code>
 *    <tr>
 *       <td><i>argStyleText</i>
 *       <td><code>new SimpleDateFormat(argStyleText, getLocale())</code>
 *    <tr>
 *       <td><i>argSkeletonText</i>
 *       <td><code>DateFormat.getInstanceForSkeleton(argSkeletonText, getLocale())</code>
 *    <tr>
 *       <td rowspan=6><code>time</code>
 *       <td><i>(none)</i>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>short</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.SHORT, getLocale())</code>
 *    <tr>
 *       <td><code>medium</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.DEFAULT, getLocale())</code>
 *    <tr>
 *       <td><code>long</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.LONG, getLocale())</code>
 *    <tr>
 *       <td><code>full</code>
 *       <td><code>DateFormat.getTimeInstance(DateFormat.FULL, getLocale())</code>
 *    <tr>
 *       <td><i>argStyleText</i>
 *       <td><code>new SimpleDateFormat(argStyleText, getLocale())</code>
 *    <tr>
 *       <td><code>spellout</code>
 *       <td><i>argStyleText (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.SPELLOUT)
 *           <br>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(argStyleText);</code>
 *    <tr>
 *       <td><code>ordinal</code>
 *       <td><i>argStyleText (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.ORDINAL)
 *           <br>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(argStyleText);</code>
 *    <tr>
 *       <td><code>duration</code>
 *       <td><i>argStyleText (optional)</i>
 *       <td><code>new RuleBasedNumberFormat(getLocale(), RuleBasedNumberFormat.DURATION)
 *           <br>&nbsp;&nbsp;&nbsp;&nbsp;.setDefaultRuleset(argStyleText);</code>
 * </table>
 *
 * <h4><a name="diffsjdk">Differences from java.text.MessageFormat</a></h4>
 *
 * <p>The ICU MessageFormat supports both named and numbered arguments,
 * while the JDK MessageFormat only supports numbered arguments.
 * Named arguments make patterns more readable.
 *
 * <p>ICU implements a more user-friendly apostrophe quoting syntax.
 * In message text, an apostrophe only begins quoting literal text
 * if it immediately precedes a syntax character (mostly {curly braces}).<br>
 * In the JDK MessageFormat, an apostrophe always begins quoting,
 * which requires common text like "don't" and "aujourd'hui"
 * to be written with doubled apostrophes like "don''t" and "aujourd''hui".
 * For more details see {@link MessagePattern.ApostropheMode}.
 *
 * <p>ICU does not create a ChoiceFormat object for a choiceArg, pluralArg or selectArg
 * but rather handles such arguments itself.
 * The JDK MessageFormat does create and use a ChoiceFormat object
 * (<code>new ChoiceFormat(argStyleText)</code>).
 * The JDK does not support plural and select arguments at all.

 * <p>Both the ICU and the JDK <code>MessageFormat</code> can control the argument
 * formats by using <code>argStyle</code>. But the JDK <code>MessageFormat</code> only
 * supports predefined formats and number / date / time pattern strings (which would need
 * to be localized).<br>
 * ICU supports everything the JDK does, and also number / date / time <b>skeletons</b> using the
 * <code>::</code> prefix (which automatically yield output appropriate for the
 * <code>MessageFormat</code> locale).</p>
 *
 * <h4>Argument formatting</h4>
 *
 * <p>Arguments are formatted according to their type, using the default
 * ICU formatters for those types, unless otherwise specified.
 * For unknown types, <code>MessageFormat</code> will call <code>toString()</code>.</p>
 *
 * <p>There are also several ways to control the formatting.</p>
 *
 * <p>We recommend you use default styles, predefined style values, skeletons,
 * or preformatted values, but not pattern strings or custom format objects.</p>
 *
 * <p>For more details, see the
 * <a href="https://unicode-org.github.io/icu/userguide/format_parse/messages">ICU User Guide</a>.</p>
 *
 * <h4>Usage Information</h4>
 *
 * <p>Here are some examples of usage:
 * <blockquote>
 * <pre>
 * Object[] arguments = {
 *     7,
 *     new Date(System.currentTimeMillis()),
 *     "a disturbance in the Force"
 * };
 *
 * String result = MessageFormat.format(
 *     "At {1,time,::jmm} on {1,date,::dMMMM}, there was {2} on planet {0,number,integer}.",
 *     arguments);
 *
 * <em>output</em>: At 4:34 PM on March 23, there was a disturbance
 *           in the Force on planet 7.
 *
 * </pre>
 * </blockquote>
 * Typically, the message format will come from resources, and the
 * arguments will be dynamically set at runtime.
 *
 * <p>Example 2:
 * <blockquote>
 * <pre>
 * Object[] testArgs = { 3, "MyDisk" };
 *
 * MessageFormat form = new MessageFormat(
 *     "The disk \"{1}\" contains {0} file(s).");
 *
 * System.out.println(form.format(testArgs));
 *
 * // output, with different testArgs
 * <em>output</em>: The disk "MyDisk" contains 0 file(s).
 * <em>output</em>: The disk "MyDisk" contains 1 file(s).
 * <em>output</em>: The disk "MyDisk" contains 1,273 file(s).
 * </pre>
 * </blockquote>
 *
 * <p>For messages that include plural forms, you can use a plural argument:
 * <pre>
 * MessageFormat msgFmt = new MessageFormat(
 *     "{num_files, plural, " +
 *     "=0{There are no files on disk \"{disk_name}\".}" +
 *     "=1{There is one file on disk \"{disk_name}\".}" +
 *     "other{There are # files on disk \"{disk_name}\".}}",
 *     ULocale.ENGLISH);
 * Map args = new HashMap();
 * args.put("num_files", 0);
 * args.put("disk_name", "MyDisk");
 * System.out.println(msgFmt.format(args));
 * args.put("num_files", 3);
 * System.out.println(msgFmt.format(args));
 *
 * <em>output</em>:
 * There are no files on disk "MyDisk".
 * There are 3 files on "MyDisk".
 * </pre>
 * See {@link PluralFormat} and {@link PluralRules} for details.
 *
 * <h4><a name="synchronization">Synchronization</a></h4>
 *
 * <p>MessageFormats are not synchronized.
 * It is recommended to create separate format instances for each thread.
 * If multiple threads access a format concurrently, it must be synchronized
 * externally.
 *
 * @see          java.util.Locale
 * @see          Format
 * @see          NumberFormat
 * @see          DecimalFormat
 * @see          ChoiceFormat
 * @see          PluralFormat
 * @see          SelectFormat
 * @author       Mark Davis
 * @author       Markus Scherer
 * icu_annot::stable ICU 3.0
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MessageFormat extends Format {

    // Incremented by 1 for ICU 4.8's new format.
    static final long serialVersionUID = 7136212545847378652L;

    /**
     * Formats a message pattern string with a variable number of name/value pair arguments.
     * Creates an ICU MessageFormat for the locale and pattern,
     * and formats with the arguments.
     *
     * @param locale Locale for number formatting and plural selection etc.
     * @param msg an ICU-MessageFormat-syntax string
     * @param nameValuePairs (argument name, argument value) pairs
     */
    public static final String formatNamedArgs(Context context, Locale locale,
                                               String msg, Object... nameValuePairs) {
        StringBuilder result = new StringBuilder(msg.length());
        new MessageFormat(context, msg, locale).format(0, null, null, null, nameValuePairs,
                new AppendableWrapper(result), null);
        return result.toString();
    }

    /**
     * Constructs a MessageFormat for the default <code>FORMAT</code> locale and the
     * specified pattern.
     * Sets the locale and calls applyPattern(pattern).
     *
     * @param pattern the pattern for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * @see Category#FORMAT
     * icu_annot::stable ICU 3.0
     */
    public MessageFormat(Context context, String pattern) {
        locale_ = Locale.getDefault();  // Category.FORMAT
        context_ = context;
        applyPattern(pattern);
    }

    /**
     * Constructs a MessageFormat for the specified locale and
     * pattern.
     * Sets the locale and calls applyPattern(pattern).
     *
     * @param pattern the pattern for this message format
     * @param locale the locale for this message format
     * @exception IllegalArgumentException if the pattern is invalid
     * icu_annot::stable ICU 3.0
     */
    public MessageFormat(Context context, String pattern, Locale locale) {
        locale_ = locale;
        context_ = context;
        applyPattern(pattern);
    }

    /**
     * Returns the locale that's used when creating or comparing subformats.
     *
     * @return the locale used when creating or comparing subformats
     * icu_annot::stable ICU 3.0
     */
    public Locale getLocale() {
        return locale_;
    }

    /**
     * Sets the pattern used by this message format.
     * Parses the pattern and caches Format objects for simple argument types.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     *
     * @param pttrn the pattern for this message format
     * @throws IllegalArgumentException if the pattern is invalid
     * icu_annot::stable ICU 3.0
     */
    public void applyPattern(String pttrn) {
        try {
            if (msgPattern == null) {
                msgPattern = new MessagePattern(pttrn);
            } else {
                msgPattern.parse(pttrn);
            }
            // Cache the formats that are explicitly mentioned in the message pattern.
            cacheExplicitFormats();
        } catch(RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    /**
     * {icu_annot::icu} Sets the ApostropheMode and the pattern used by this message format.
     * Parses the pattern and caches Format objects for simple argument types.
     * Patterns and their interpretation are specified in the
     * <a href="#patterns">class description</a>.
     * <p>
     * This method is best used only once on a given object to avoid confusion about the mode,
     * and after constructing the object with an empty pattern string to minimize overhead.
     *
     * @param pattern the pattern for this message format
     * @param aposMode the new ApostropheMode
     * @throws IllegalArgumentException if the pattern is invalid
     * @see MessagePattern.ApostropheMode
     * icu_annot::stable ICU 4.8
     */
    public void applyPattern(String pattern, MessagePattern.ApostropheMode aposMode) {
        if (msgPattern == null) {
            msgPattern = new MessagePattern(aposMode);
        } else if (aposMode != msgPattern.getApostropheMode()) {
            msgPattern.clearPatternAndSetApostropheMode(aposMode);
        }
        applyPattern(pattern);
    }

    /**
     * {icu_annot::icu}
     * @return this instance's ApostropheMode.
     * icu_annot::stable ICU 4.8
     */
    public MessagePattern.ApostropheMode getApostropheMode() {
        if (msgPattern == null) {
            msgPattern = new MessagePattern();  // Sets the default mode.
        }
        return msgPattern.getApostropheMode();
    }

    /**
     * Returns the applied pattern string.
     * @return the pattern string
     * @throws IllegalStateException after custom Format objects have been set
     *         via setFormat() or similar APIs
     * icu_annot::stable ICU 3.0
     */
    public String toPattern() {
        // Return the original, applied pattern string, or else "".
        // Note: This does not take into account
        // - changes from setFormat() and similar methods, or
        // - normalization of apostrophes and arguments, for example,
        //   whether some date/time/number formatter was created via a pattern
        //   but is equivalent to the "medium" default format.
        if (customFormatArgStarts != null) {
            throw new IllegalStateException(
                    "toPattern() is not supported after custom Format objects "+
                    "have been set via setFormat() or similar APIs");
        }
        if (msgPattern == null) {
            return "";
        }
        String originalPattern = msgPattern.getPatternString();
        return originalPattern == null ? "" : originalPattern;
    }

    /**
     * Returns the part index of the next ARG_START after partIndex, or -1 if there is none more.
     * @param partIndex Part index of the previous ARG_START (initially 0).
     */
    private int nextTopLevelArgStart(int partIndex) {
        if (partIndex != 0) {
            partIndex = msgPattern.getLimitPartIndex(partIndex);
        }
        for (;;) {
            MessagePattern.Part.Type type = msgPattern.getPartType(++partIndex);
            if (type == MessagePattern.Part.Type.ARG_START) {
                return partIndex;
            }
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                return -1;
            }
        }
    }

    private boolean argNameMatches(int partIndex, String argName, int argNumber) {
        Part part = msgPattern.getPart(partIndex);
        return part.getType() == MessagePattern.Part.Type.ARG_NAME ?
            msgPattern.partSubstringMatches(part, argName) :
            part.getValue() == argNumber;  // ARG_NUMBER
    }

    private String getArgName(int partIndex) {
        Part part = msgPattern.getPart(partIndex);
        if (part.getType() == MessagePattern.Part.Type.ARG_NAME) {
            return msgPattern.getSubstring(part);
        } else {
            return Integer.toString(part.getValue());
        }
    }

    /**
     * Sets the Format objects to use for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The indices of elements in <code>newFormats</code>
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in <code>newFormats</code> thus corresponds to
     * the order of elements in the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the corresponding new format is used
     * for all such format elements. If an argument index is not used
     * for any format element in the pattern string, then the
     * corresponding new format is ignored. If fewer formats are provided
     * than needed, then only the formats for argument indices less
     * than <code>newFormats.length</code> are replaced.
     *
     * This method is only supported if the format does not use
     * named arguments, otherwise an IllegalArgumentException is thrown.
     *
     * @param newFormats the new formats to use
     * @throws NullPointerException if <code>newFormats</code> is null
     * @throws IllegalArgumentException if this formatter uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public void setFormatsByArgumentIndex(Format[] newFormats) {
        if (msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            int argNumber = msgPattern.getPart(partIndex + 1).getValue();
            if (argNumber < newFormats.length) {
                setCustomArgStartFormat(partIndex, newFormats[argNumber]);
            }
        }
    }

    /**
     * {icu_annot::icu} Sets the Format objects to use for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The keys in <code>newFormats</code> are the argument
     * names in the previously set pattern string, and the values
     * are the formats.
     * <p>
     * Only argument names from the pattern string are considered.
     * Extra keys in <code>newFormats</code> that do not correspond
     * to an argument name are ignored.  Similarly, if there is no
     * format in newFormats for an argument name, the formatter
     * for that argument remains unchanged.
     * <p>
     * This may be called on formats that do not use named arguments.
     * In this case the map will be queried for key Strings that
     * represent argument indices, e.g. "0", "1", "2" etc.
     *
     * @param newFormats a map from String to Format providing new
     *        formats for named arguments.
     * icu_annot::stable ICU 3.8
     */
    public void setFormatsByArgumentName(Map<String, Format> newFormats) {
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            String key = getArgName(partIndex + 1);
            if (newFormats.containsKey(key)) {
                setCustomArgStartFormat(partIndex, newFormats.get(key));
            }
        }
    }

    /**
     * Sets the Format objects to use for the format elements in the
     * previously set pattern string.
     * The order of formats in <code>newFormats</code> corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * If more formats are provided than needed by the pattern string,
     * the remaining ones are ignored. If fewer formats are provided
     * than needed, then only the first <code>newFormats.length</code>
     * formats are replaced.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatsByArgumentIndex setFormatsByArgumentIndex}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the <code>arguments</code> array passed to
     * the <code>format</code> methods or the result array returned by
     * the <code>parse</code> methods.
     *
     * @param newFormats the new formats to use
     * @exception NullPointerException if <code>newFormats</code> is null
     * icu_annot::stable ICU 3.0
     */
    public void setFormats(Format[] newFormats) {
        int formatNumber = 0;
        for (int partIndex = 0;
                formatNumber < newFormats.length &&
                (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            setCustomArgStartFormat(partIndex, newFormats[formatNumber]);
            ++formatNumber;
        }
    }

    /**
     * Sets the Format object to use for the format elements within the
     * previously set pattern string that use the given argument
     * index.
     * The argument index is part of the format element definition and
     * represents an index into the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If the argument index is used for more than one format element
     * in the pattern string, then the new format is used for all such
     * format elements. If the argument index is not used for any format
     * element in the pattern string, then the new format is ignored.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @param argumentIndex the argument index for which to use the new format
     * @param newFormat the new format to use
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        if (msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            if (msgPattern.getPart(partIndex + 1).getValue() == argumentIndex) {
                setCustomArgStartFormat(partIndex, newFormat);
            }
        }
    }

    /**
     * {icu_annot::icu} Sets the Format object to use for the format elements within the
     * previously set pattern string that use the given argument
     * name.
     * <p>
     * If the argument name is used for more than one format element
     * in the pattern string, then the new format is used for all such
     * format elements. If the argument name is not used for any format
     * element in the pattern string, then the new format is ignored.
     * <p>
     * This API may be used on formats that do not use named arguments.
     * In this case <code>argumentName</code> should be a String that names
     * an argument index, e.g. "0", "1", "2"... etc.  If it does not name
     * a valid index, the format will be ignored.  No error is thrown.
     *
     * @param argumentName the name of the argument to change
     * @param newFormat the new format to use
     * icu_annot::stable ICU 3.8
     */
    public void setFormatByArgumentName(String argumentName, Format newFormat) {
        int argNumber = MessagePattern.validateArgumentName(argumentName);
        if (argNumber < MessagePattern.ARG_NAME_NOT_NUMBER) {
            return;
        }
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            if (argNameMatches(partIndex + 1, argumentName, argNumber)) {
                setCustomArgStartFormat(partIndex, newFormat);
            }
        }
    }

    /**
     * Sets the Format object to use for the format element with the given
     * format element index within the previously set pattern string.
     * The format element index is the zero-based number of the format
     * element counting from the start of the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it is generally better to use the
     * {@link #setFormatByArgumentIndex setFormatByArgumentIndex}
     * method, which accesses format elements based on the argument
     * index they specify.
     *
     * @param formatElementIndex the index of a format element within the pattern
     * @param newFormat the format to use for the specified format element
     * @exception ArrayIndexOutOfBoundsException if formatElementIndex is equal to or
     *            larger than the number of format elements in the pattern string
     * icu_annot::stable ICU 3.0
     */
    public void setFormat(int formatElementIndex, Format newFormat) {
        int formatNumber = 0;
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            if (formatNumber == formatElementIndex) {
                setCustomArgStartFormat(partIndex, newFormat);
                return;
            }
            ++formatNumber;
        }
        throw new ArrayIndexOutOfBoundsException(formatElementIndex);
    }

    /**
     * Returns the Format objects used for the values passed into
     * <code>format</code> methods or returned from <code>parse</code>
     * methods. The indices of elements in the returned array
     * correspond to the argument indices used in the previously set
     * pattern string.
     * The order of formats in the returned array thus corresponds to
     * the order of elements in the <code>arguments</code> array passed
     * to the <code>format</code> methods or the result array returned
     * by the <code>parse</code> methods.
     * <p>
     * If an argument index is used for more than one format element
     * in the pattern string, then the format used for the last such
     * format element is returned in the array. If an argument index
     * is not used for any format element in the pattern string, then
     * null is returned in the array.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @return the formats used for the arguments within the pattern
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public Format[] getFormatsByArgumentIndex() {
        if (msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }
        ArrayList<Format> list = new ArrayList<>();
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            int argNumber = msgPattern.getPart(partIndex + 1).getValue();
            while (argNumber >= list.size()) {
                list.add(null);
            }
            list.set(argNumber, cachedFormatters == null ? null : cachedFormatters.get(partIndex));
        }
        return list.toArray(new Format[list.size()]);
    }

    /**
     * Returns the Format objects used for the format elements in the
     * previously set pattern string.
     * The order of formats in the returned array corresponds to
     * the order of format elements in the pattern string.
     * <p>
     * Since the order of format elements in a pattern string often
     * changes during localization, it's generally better to use the
     * {@link #getFormatsByArgumentIndex()}
     * method, which assumes an order of formats corresponding to the
     * order of elements in the <code>arguments</code> array passed to
     * the <code>format</code> methods or the result array returned by
     * the <code>parse</code> methods.
     *
     * This method is only supported when exclusively numbers are used for
     * argument names. Otherwise an IllegalArgumentException is thrown.
     *
     * @return the formats used for the format elements in the pattern
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public Format[] getFormats() {
        ArrayList<Format> list = new ArrayList<>();
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            list.add(cachedFormatters == null ? null : cachedFormatters.get(partIndex));
        }
        return list.toArray(new Format[list.size()]);
    }

    /**
     * {icu_annot::icu} Returns the top-level argument names. For more details, see
     * {@link #setFormatByArgumentName(String, Format)}.
     * @return a Set of argument names
     * icu_annot::stable ICU 4.8
     */
    public Set<String> getArgumentNames() {
        Set<String> result = new HashSet<>();
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            result.add(getArgName(partIndex + 1));
        }
        return result;
    }

    /**
     * {icu_annot::icu} Returns the first top-level format associated with the given argument name.
     * For more details, see {@link #setFormatByArgumentName(String, Format)}.
     * @param argumentName The name of the desired argument.
     * @return the Format associated with the name, or null if there isn't one.
     * icu_annot::stable ICU 4.8
     */
    public Format getFormatByArgumentName(String argumentName) {
        if (cachedFormatters == null) {
            return null;
        }
        int argNumber = MessagePattern.validateArgumentName(argumentName);
        if (argNumber < MessagePattern.ARG_NAME_NOT_NUMBER) {
            return null;
        }
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            if (argNameMatches(partIndex + 1, argumentName, argNumber)) {
                return cachedFormatters.get(partIndex);
            }
        }
        return null;
    }

    /**
     * Formats an array of objects and appends the <code>MessageFormat</code>'s
     * pattern, with arguments replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * <p>
     * The text substituted for the individual format elements is derived from
     * the current subformat of the format element and the
     * <code>arguments</code> element at the format element's argument index
     * as indicated by the first matching line of the following table. An
     * argument is <i>unavailable</i> if <code>arguments</code> is
     * <code>null</code> or has fewer than argumentIndex+1 elements.  When
     * an argument is unavailable no substitution is performed.
     *
     * <table border=1>
     *    <tr>
     *       <th>argType or Format
     *       <th>value object
     *       <th>Formatted Text
     *    <tr>
     *       <td><i>any</i>
     *       <td><i>unavailable</i>
     *       <td><code>"{" + argNameOrNumber + "}"</code>
     *    <tr>
     *       <td><i>any</i>
     *       <td><code>null</code>
     *       <td><code>"null"</code>
     *    <tr>
     *       <td>custom Format <code>!= null</code>
     *       <td><i>any</i>
     *       <td><code>customFormat.format(argument)</code>
     *    <tr>
     *       <td>noneArg, or custom Format <code>== null</code>
     *       <td><code>instanceof Number</code>
     *       <td><code>NumberFormat.getInstance(getLocale()).format(argument)</code>
     *    <tr>
     *       <td>noneArg, or custom Format <code>== null</code>
     *       <td><code>instanceof Date</code>
     *       <td><code>DateFormat.getDateTimeInstance(DateFormat.SHORT,
     *           DateFormat.SHORT, getLocale()).format(argument)</code>
     *    <tr>
     *       <td>noneArg, or custom Format <code>== null</code>
     *       <td><code>instanceof String</code>
     *       <td><code>argument</code>
     *    <tr>
     *       <td>noneArg, or custom Format <code>== null</code>
     *       <td><i>any</i>
     *       <td><code>argument.toString()</code>
     *    <tr>
     *       <td>complexArg
     *       <td><i>any</i>
     *       <td>result of recursive formatting of a selected sub-message
     * </table>
     * <p>
     * If <code>pos</code> is non-null, and refers to
     * <code>Field.ARGUMENT</code>, the location of the first formatted
     * string will be returned.
     *
     * This method is only supported when the format does not use named
     * arguments, otherwise an IllegalArgumentException is thrown.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @throws IllegalArgumentException if a value in the
     *         <code>arguments</code> array is not of the type
     *         expected by the corresponding argument or custom Format object.
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public final StringBuffer format(Object[] arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        format(arguments, null, new AppendableWrapper(result), pos);
        return result;
    }

    /**
     * Formats a map of objects and appends the <code>MessageFormat</code>'s
     * pattern, with arguments replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * <p>
     * The text substituted for the individual format elements is derived from
     * the current subformat of the format element and the
     * <code>arguments</code> value corresponding to the format element's
     * argument name.
     * <p>
     * A numbered pattern argument is matched with a map key that contains that number
     * as an ASCII-decimal-digit string (without leading zero).
     * <p>
     * An argument is <i>unavailable</i> if <code>arguments</code> is
     * <code>null</code> or does not have a value corresponding to an argument
     * name in the pattern.  When an argument is unavailable no substitution
     * is performed.
     *
     * @param arguments a map of objects to be formatted and substituted.
     * @param result where text is appended.
     * @param pos On input: an alignment field, if desired.
     *            On output: the offsets of the alignment field.
     * @throws IllegalArgumentException if a value in the
     *         <code>arguments</code> array is not of the type
     *         expected by the corresponding argument or custom Format object.
     * @return the passed-in StringBuffer
     * icu_annot::stable ICU 3.8
     */
    public final StringBuffer format(Map<String, Object> arguments, StringBuffer result,
                                     FieldPosition pos) {
        format(null, arguments, new AppendableWrapper(result), pos);
        return result;
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it
     * to format the given arguments. This is equivalent to
     * <blockquote>
     *     <code>(new {@link #MessageFormat(String) MessageFormat}(pattern)).{@link
     *     #format(java.lang.Object[], java.lang.StringBuffer, java.text.FieldPosition)
     *     format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     *
     * @throws IllegalArgumentException if the pattern is invalid
     * @throws IllegalArgumentException if a value in the
     *         <code>arguments</code> array is not of the type
     *         expected by the corresponding argument or custom Format object.
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public static String format(Context context, String pattern, Object... arguments) {
        MessageFormat temp = new MessageFormat(context, pattern);
        return temp.format(arguments);
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it to
     * format the given arguments.  The pattern must identifyarguments
     * by name instead of by number.
     * <p>
     * @throws IllegalArgumentException if the pattern is invalid
     * @throws IllegalArgumentException if a value in the
     *         <code>arguments</code> array is not of the type
     *         expected by the corresponding argument or custom Format object.
     * @see #format(Map, StringBuffer, FieldPosition)
     * @see #format(String, Object[])
     * icu_annot::stable ICU 3.8
     */
    public static String format(Context context, String pattern, Map<String, Object> arguments) {
        MessageFormat temp = new MessageFormat(context, pattern);
        return temp.format(arguments);
    }

    /**
     * {icu_annot::icu} Returns true if this MessageFormat uses named arguments,
     * and false otherwise.  See class description.
     *
     * @return true if named arguments are used.
     * icu_annot::stable ICU 3.8
     */
    public boolean usesNamedArguments() {
        return msgPattern.hasNamedArguments();
    }

    // Overrides
    /**
     * Formats a map or array of objects and appends the <code>MessageFormat</code>'s
     * pattern, with format elements replaced by the formatted objects, to the
     * provided <code>StringBuffer</code>.
     * This is equivalent to either of
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer,
     *     java.text.FieldPosition) format}((Object[]) arguments, result, pos)</code>
     *     <code>{@link #format(java.util.Map, java.lang.StringBuffer,
     *     java.text.FieldPosition) format}((Map) arguments, result, pos)</code>
     * </blockquote>
     * A map must be provided if this format uses named arguments, otherwise
     * an IllegalArgumentException will be thrown.
     * @param arguments a map or array of objects to be formatted
     * @param result where text is appended
     * @param pos On input: an alignment field, if desired
     *            On output: the offsets of the alignment field
     * @throws IllegalArgumentException if an argument in
     *         <code>arguments</code> is not of the type
     *         expected by the format element(s) that use it
     * @throws IllegalArgumentException if <code>arguments</code> is
     *         an array of Object and this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    @Override
    public final StringBuffer format(Object arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        format(arguments, new AppendableWrapper(result), pos);
        return result;
    }

    /**
     * Formats an array of objects and inserts them into the
     * <code>MessageFormat</code>'s pattern, producing an
     * <code>AttributedCharacterIterator</code>.
     * You can use the returned <code>AttributedCharacterIterator</code>
     * to build the resulting String, as well as to determine information
     * about the resulting String.
     * <p>
     * The text of the returned <code>AttributedCharacterIterator</code> is
     * the same that would be returned by
     * <blockquote>
     *     <code>{@link #format(java.lang.Object[], java.lang.StringBuffer,
     *     java.text.FieldPosition) format}(arguments, new StringBuffer(), null).toString()</code>
     * </blockquote>
     * <p>
     * In addition, the <code>AttributedCharacterIterator</code> contains at
     * least attributes indicating where text was generated from an
     * argument in the <code>arguments</code> array. The keys of these attributes are of
     * type <code>MessageFormat.Field</code>, their values are
     * <code>Integer</code> objects indicating the index in the <code>arguments</code>
     * array of the argument from which the text was generated.
     * <p>
     * The attributes/value from the underlying <code>Format</code>
     * instances that <code>MessageFormat</code> uses will also be
     * placed in the resulting <code>AttributedCharacterIterator</code>.
     * This allows you to not only find where an argument is placed in the
     * resulting String, but also which fields it contains in turn.
     *
     * @param arguments an array of objects to be formatted and substituted.
     * @return AttributedCharacterIterator describing the formatted value.
     * @exception NullPointerException if <code>arguments</code> is null.
     * @throws IllegalArgumentException if a value in the
     *         <code>arguments</code> array is not of the type
     *         expected by the corresponding argument or custom Format object.
     * icu_annot::stable ICU 3.8
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        if (arguments == null) {
            throw new NullPointerException(
                   "formatToCharacterIterator must be passed non-null object");
        }
        StringBuilder result = new StringBuilder();
        AppendableWrapper wrapper = new AppendableWrapper(result);
        wrapper.useAttributes();
        format(arguments, wrapper, null);
        AttributedString as = new AttributedString(result.toString());
        for (AttributeAndPosition a : wrapper.attributes) {
            as.addAttribute(a.key, a.value, a.start, a.limit);
        }
        return as.getIterator();
    }

    /**
     * Parses the string.
     *
     * <p>Caveats: The parse may fail in a number of circumstances.
     * For example:
     * <ul>
     * <li>If one of the arguments does not occur in the pattern.
     * <li>If the format of an argument loses information, such as
     *     with a choice format where a large number formats to "many".
     * <li>Does not yet handle recursion (where
     *     the substituted strings contain {n} references.)
     * <li>Will not always find a match (or the correct match)
     *     if some part of the parse is ambiguous.
     *     For example, if the pattern "{1},{2}" is used with the
     *     string arguments {"a,b", "c"}, it will format as "a,b,c".
     *     When the result is parsed, it will return {"a", "b,c"}.
     * <li>If a single argument is parsed more than once in the string,
     *     then the later parse wins.
     * </ul>
     * When the parse fails, use ParsePosition.getErrorIndex() to find out
     * where in the string did the parsing failed. The returned error
     * index is the starting offset of the sub-patterns that the string
     * is comparing with. For example, if the parsing string "AAA {0} BBB"
     * is comparing against the pattern "AAD {0} BBB", the error index is
     * 0. When an error occurs, the call to this method will return null.
     * If the source is null, return an empty array.
     *
     * @throws IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use named argument.");
        }

        // Count how many slots we need in the array.
        int maxArgId = -1;
        for (int partIndex = 0; (partIndex = nextTopLevelArgStart(partIndex)) >= 0;) {
            int argNumber=msgPattern.getPart(partIndex + 1).getValue();
            if (argNumber > maxArgId) {
                maxArgId = argNumber;
            }
        }
        Object[] resultArray = new Object[maxArgId + 1];

        int backupStartPos = pos.getIndex();
        parse(0, source, pos, resultArray, null);
        if (pos.getIndex() == backupStartPos) { // unchanged, returned object is null
            return null;
        }

        return resultArray;
    }

    /**
     * {icu_annot::icu} Parses the string, returning the results in a Map.
     * This is similar to the version that returns an array
     * of Object.  This supports both named and numbered
     * arguments-- if numbered, the keys in the map are the
     * corresponding ASCII-decimal-digit strings (e.g. "0", "1", "2"...).
     *
     * @param source the text to parse
     * @param pos the position at which to start parsing.  on return,
     *        contains the result of the parse.
     * @return a Map containing key/value pairs for each parsed argument.
     * icu_annot::stable ICU 3.8
     */
    public Map<String, Object> parseToMap(String source, ParsePosition pos)  {
        Map<String, Object> result = new HashMap<>();
        int backupStartPos = pos.getIndex();
        parse(0, source, pos, null, result);
        if (pos.getIndex() == backupStartPos) {
            return null;
        }
        return result;
    }

    /**
     * Parses text from the beginning of the given string to produce an object
     * array.
     * The method may not use the entire text of the given string.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return An <code>Object</code> array parsed from the string.
     * @exception ParseException if the beginning of the specified string cannot be parsed.
     * @exception IllegalArgumentException if this format uses named arguments
     * icu_annot::stable ICU 3.0
     */
    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() == 0) // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!",
                                     pos.getErrorIndex());

        return result;
    }

    /**
     * Parses the string, filling either the Map or the Array.
     * This is a private method that all the public parsing methods call.
     * This supports both named and numbered
     * arguments-- if numbered, the keys in the map are the
     * corresponding ASCII-decimal-digit strings (e.g. "0", "1", "2"...).
     *
     * @param msgStart index in the message pattern to start from.
     * @param source the text to parse
     * @param pos the position at which to start parsing.  on return,
     *        contains the result of the parse.
     * @param args if not null, the parse results will be filled here (The pattern
     *        has to have numbered arguments in order for this to not be null).
     * @param argsMap if not null, the parse results will be filled here.
     */
    @SuppressWarnings("IncrementInForLoopAndHeader")
    private void parse(int msgStart, String source, ParsePosition pos,
                       Object[] args, Map<String, Object> argsMap) {
        if (source == null) {
            return;
        }
        String msgString=msgPattern.getPatternString();
        int prevIndex=msgPattern.getPart(msgStart).getLimit();
        int sourceOffset = pos.getIndex();
        ParsePosition tempStatus = new ParsePosition(0);

        for(int i=msgStart+1; ; ++i) {
            Part part=msgPattern.getPart(i);
            Part.Type type=part.getType();
            int index=part.getIndex();
            // Make sure the literal string matches.
            int len = index - prevIndex;
            if (len == 0 || msgString.regionMatches(prevIndex, source, sourceOffset, len)) {
                sourceOffset += len;
                prevIndex += len;
            } else {
                pos.setErrorIndex(sourceOffset);
                return; // leave index as is to signal error
            }
            if(type==Part.Type.MSG_LIMIT) {
                // Things went well! Done.
                pos.setIndex(sourceOffset);
                return;
            }
            if(type==Part.Type.SKIP_SYNTAX || type==Part.Type.INSERT_CHAR) {
                prevIndex=part.getLimit();
                continue;
            }
            // We do not support parsing Plural formats. (No REPLACE_NUMBER here.)
            assert type==Part.Type.ARG_START : "Unexpected Part "+part+" in parsed message.";
            int argLimit=msgPattern.getLimitPartIndex(i);

            ArgType argType=part.getArgType();
            part=msgPattern.getPart(++i);
            // Compute the argId, so we can use it as a key.
            Object argId=null;
            int argNumber = 0;
            String key = null;
            if(args!=null) {
                argNumber=part.getValue();  // ARG_NUMBER
                argId = Integer.valueOf(argNumber);
            } else {
                if(part.getType()==MessagePattern.Part.Type.ARG_NAME) {
                    key=msgPattern.getSubstring(part);
                } else /* ARG_NUMBER */ {
                    key=Integer.toString(part.getValue());
                }
                argId = key;
            }

            ++i;
            Format formatter = null;
            boolean haveArgResult = false;
            Object argResult = null;
            if(cachedFormatters!=null && (formatter=cachedFormatters.get(i - 2))!=null) {
                // Just parse using the formatter.
                tempStatus.setIndex(sourceOffset);
                argResult = formatter.parseObject(source, tempStatus);
                if (tempStatus.getIndex() == sourceOffset) {
                    pos.setErrorIndex(sourceOffset);
                    return; // leave index as is to signal error
                }
                haveArgResult = true;
                sourceOffset = tempStatus.getIndex();
            } else if(
                    argType==ArgType.NONE ||
                    (cachedFormatters!=null && cachedFormatters.containsKey(i - 2))) {
                // Match as a string.
                // if at end, use longest possible match
                // otherwise uses first match to intervening string
                // does NOT recursively try all possibilities
                String stringAfterArgument = getLiteralStringUntilNextArgument(argLimit);
                int next;
                if (stringAfterArgument.length() != 0) {
                    next = source.indexOf(stringAfterArgument, sourceOffset);
                } else {
                    next = source.length();
                }
                if (next < 0) {
                    pos.setErrorIndex(sourceOffset);
                    return; // leave index as is to signal error
                } else {
                    String strValue = source.substring(sourceOffset, next);
                    if (!strValue.equals("{" + argId.toString() + "}")) {
                        haveArgResult = true;
                        argResult = strValue;
                    }
                    sourceOffset = next;
                }
            } else if(argType==ArgType.CHOICE) {
                tempStatus.setIndex(sourceOffset);
                double choiceResult = parseChoiceArgument(msgPattern, i, source, tempStatus);
                if (tempStatus.getIndex() == sourceOffset) {
                    pos.setErrorIndex(sourceOffset);
                    return; // leave index as is to signal error
                }
                argResult = choiceResult;
                haveArgResult = true;
                sourceOffset = tempStatus.getIndex();
            } else if(argType.hasPluralStyle() || argType==ArgType.SELECT) {
                // No can do!
                throw new UnsupportedOperationException(
                        "Parsing of plural/select/selectordinal argument is not supported.");
            } else {
                // This should never happen.
                throw new IllegalStateException("unexpected argType "+argType);
            }
            if (haveArgResult) {
                if (args != null) {
                    args[argNumber] = argResult;
                } else if (argsMap != null) {
                    argsMap.put(key, argResult);
                }
            }
            prevIndex=msgPattern.getPart(argLimit).getLimit();
            i=argLimit;
        }
    }

    /**
     * {icu_annot::icu} Parses text from the beginning of the given string to produce a map from
     * argument to values. The method may not use the entire text of the given string.
     *
     * <p>See the {@link #parse(String, ParsePosition)} method for more information on
     * message parsing.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return A <code>Map</code> parsed from the string.
     * @throws ParseException if the beginning of the specified string cannot
     *         be parsed.
     * @see #parseToMap(String, ParsePosition)
     * icu_annot::stable ICU 3.8
     */
    public Map<String, Object> parseToMap(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Map<String, Object> result = new HashMap<>();
        parse(0, source, pos, null, result);
        if (pos.getIndex() == 0) // unchanged, returned object is null
            throw new ParseException("MessageFormat parse error!",
                                     pos.getErrorIndex());

        return result;
    }

    /**
     * Parses text from a string to produce an object array or Map.
     * <p>
     * The method attempts to parse text starting at the index given by
     * <code>pos</code>.
     * If parsing succeeds, then the index of <code>pos</code> is updated
     * to the index after the last character used (parsing does not necessarily
     * use all characters up to the end of the string), and the parsed
     * object array is returned. The updated <code>pos</code> can be used to
     * indicate the starting point for the next call to this method.
     * If an error occurs, then the index of <code>pos</code> is not
     * changed, the error index of <code>pos</code> is set to the index of
     * the character where the error occurred, and null is returned.
     * <p>
     * See the {@link #parse(String, ParsePosition)} method for more information
     * on message parsing.
     *
     * @param source A <code>String</code>, part of which should be parsed.
     * @param pos A <code>ParsePosition</code> object with index and error
     *            index information as described above.
     * @return An <code>Object</code> parsed from the string, either an
     *         array of Object, or a Map, depending on whether named
     *         arguments are used.  This can be queried using <code>usesNamedArguments</code>.
     *         In case of error, returns null.
     * @throws NullPointerException if <code>pos</code> is null.
     * icu_annot::stable ICU 3.0
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        if (!msgPattern.hasNamedArguments()) {
            return parse(source, pos);
        } else {
            return parseToMap(source, pos);
        }
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 3.0
    @Override
    public boolean equals(Object obj) {
        if (this == obj)                      // quick check
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MessageFormat other = (MessageFormat) obj;
        return Objects.equals(ulocale, other.ulocale)
                && Objects.equals(msgPattern, other.msgPattern)
                && Objects.equals(cachedFormatters, other.cachedFormatters)
                && Objects.equals(customFormatArgStarts, other.customFormatArgStarts);
        // Note: It might suffice to only compare custom formatters
        // rather than all formatters.
    }
     */

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 3.0
     */
    @Override
    public int hashCode() {
        return msgPattern.getPatternString().hashCode(); // enough for reasonable distribution
    }

    /**
     * Defines constants that are used as attribute keys in the
     * <code>AttributedCharacterIterator</code> returned
     * from <code>MessageFormat.formatToCharacterIterator</code>.
     *
     * icu_annot::stable ICU 3.8
     */
    public static class Field extends Format.Field {

        private static final long serialVersionUID = 7510380454602616157L;

        /**
         * Create a <code>Field</code> with the specified name.
         *
         * @param name The name of the attribute
         *
         * icu_annot::stable ICU 3.8
         */
        protected Field(String name) {
            super(name);
        }

        /**
         * Resolves instances being deserialized to the predefined constants.
         *
         * @return resolved MessageFormat.Field constant
         * @throws InvalidObjectException if the constant could not be resolved.
         *
         * icu_annot::stable ICU 3.8
         */
        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != MessageFormat.Field.class) {
                throw new InvalidObjectException(
                    "A subclass of MessageFormat.Field must implement readResolve.");
            }
            if (this.getName().equals(ARGUMENT.getName())) {
                return ARGUMENT;
            } else {
                throw new InvalidObjectException("Unknown attribute name.");
            }
        }

        /**
         * Constant identifying a portion of a message that was generated
         * from an argument passed into <code>formatToCharacterIterator</code>.
         * The value associated with the key will be an <code>Integer</code>
         * indicating the index in the <code>arguments</code> array of the
         * argument from which the text was generated.
         *
         * icu_annot::stable ICU 3.8
         */
        public static final Field ARGUMENT = new Field("message argument field");
    }

    // ===========================privates============================

    // *Important*: All fields must be declared *transient* so that we can fully
    // control serialization!
    // See for example Joshua Bloch's "Effective Java", chapter 10 Serialization.

    /**
     * The locale to use for formatting numbers and dates.
     */
    private transient Locale locale_;

    /**
     * The Android Context to used to access user preferences and other Android functionality.
     */
    private transient Context context_;

    /**
     * The MessagePattern which contains the parsed structure of the pattern string.
     */
    private transient MessagePattern msgPattern;
    /**
     * Cached formatters so we can just use them whenever needed instead of creating
     * them from scratch every time.
     */
    private transient Map<Integer, Format> cachedFormatters;
    /**
     * Set of ARG_START part indexes where custom, user-provided Format objects
     * have been set via setFormat() or similar API.
     */
    private transient Set<Integer> customFormatArgStarts;

    /**
     * Stock formatters. Those are used when a format is not explicitly mentioned in
     * the message. The format is inferred from the argument.
     */
    private transient DateFormat stockDateFormatter;
    private transient NumberFormat stockNumberFormatter;

    private transient PluralSelectorProvider pluralProvider;
    private transient PluralSelectorProvider ordinalProvider;

    private DateFormat getStockDateFormatter() {
        if (stockDateFormatter == null) {
            stockDateFormatter = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, locale_);//fix
        }
        return stockDateFormatter;
    }
    private NumberFormat getStockNumberFormatter() {
        if (stockNumberFormatter == null) {
            stockNumberFormatter = NumberFormat.getInstance(locale_);
        }
        return stockNumberFormatter;
    }

    // *Important*: All fields must be declared *transient*.
    // See the longer comment above ulocale.

    /**
     * Formats the arguments and writes the result into the
     * AppendableWrapper, updates the field position.
     *
     * <p>Exactly one of args and argsMap must be null, the other non-null.
     *
     * @param msgStart      Index to msgPattern part to start formatting from.
     * @param pluralNumber  null except when formatting a plural argument sub-message
     *                      where a '#' is replaced by the format string for this number.
     * @param args          The formattable objects array. Non-null iff numbered values are used.
     * @param argsMap       The key-value map of formattable objects. Non-null iff named values are used.
     * @param dest          Output parameter to receive the result.
     *                      The result (string & attributes) is appended to existing contents.
     * @param fp            Field position status.
     */
    @SuppressWarnings("IncrementInForLoopAndHeader")
    private void format(int msgStart, PluralSelectorContext pluralNumber,
                        Object[] args, Map<String, Object> argsMap, Object[] nameValuePairs,
                        AppendableWrapper dest, FieldPosition fp) {
        String msgString=msgPattern.getPatternString();
        int prevIndex=msgPattern.getPart(msgStart).getLimit();
        for(int i=msgStart+1;; ++i) {
            Part part=msgPattern.getPart(i);
            Part.Type type=part.getType();
            int index=part.getIndex();
            dest.append(msgString, prevIndex, index);
            if(type==Part.Type.MSG_LIMIT) {
                return;
            }
            prevIndex=part.getLimit();
            if(type==Part.Type.REPLACE_NUMBER) {
                if(pluralNumber.forReplaceNumber) {
                    // number-offset was already formatted.
                    dest.formatAndAppend(pluralNumber.formatter,
                            pluralNumber.number, pluralNumber.numberString);
                } else {
                    dest.formatAndAppend(getStockNumberFormatter(), pluralNumber.number);
                }
                continue;
            }
            if(type!=Part.Type.ARG_START) {
                continue;
            }
            int argLimit=msgPattern.getLimitPartIndex(i);
            ArgType argType=part.getArgType();
            part=msgPattern.getPart(++i);
            Object arg;
            boolean noArg=false;
            Object argId=null;
            String argName=msgPattern.getSubstring(part);
            if(args!=null) {
                int argNumber=part.getValue();  // ARG_NUMBER
                if (dest.attributes != null) {
                    // We only need argId if we add it into the attributes.
                    argId = Integer.valueOf(argNumber);
                }
                if(0<=argNumber && argNumber<args.length) {
                    arg=args[argNumber];
                } else {
                    arg=null;
                    noArg=true;
                }
            } else if(nameValuePairs!=null) {
                argId = argName;
                for(int nvIndex=0;; nvIndex+=2) {
                    if(nvIndex<nameValuePairs.length) {
                        if(argName.equals(nameValuePairs[nvIndex].toString())) {
                            arg=nameValuePairs[nvIndex+1];
                            break;
                        }
                    } else {
                        arg=null;
                        noArg=true;
                        break;
                    }
                }
            } else {
                argId = argName;
                if(argsMap!=null && argsMap.containsKey(argName)) {
                    arg=argsMap.get(argName);
                } else {
                    arg=null;
                    noArg=true;
                }
            }
            ++i;
            int prevDestLength=dest.length;
            Format formatter = null;
            if (noArg) {
                dest.append("{"+argName+"}");
            } else if (arg == null) {
                dest.append("null");
            } else if(pluralNumber!=null && pluralNumber.numberArgIndex==(i-2)) {
                if(pluralNumber.offset == 0) {
                    // The number was already formatted with this formatter.
                    dest.formatAndAppend(pluralNumber.formatter, pluralNumber.number, pluralNumber.numberString);
                } else {
                    // Do not use the formatted (number-offset) string for a named argument
                    // that formats the number without subtracting the offset.
                    dest.formatAndAppend(pluralNumber.formatter, arg);
                }
            } else if(cachedFormatters!=null && (formatter=cachedFormatters.get(i - 2))!=null) {
                // Handles all ArgType.SIMPLE, and formatters from setFormat() and its siblings.
                {
                    dest.formatAndAppend(formatter, arg);
                }
            } else if(
                    argType==ArgType.NONE ||
                    (cachedFormatters!=null && cachedFormatters.containsKey(i - 2))) {
                // ArgType.NONE, or
                // any argument which got reset to null via setFormat() or its siblings.
                if (arg instanceof Number) {
                    // format number if can
                    dest.formatAndAppend(getStockNumberFormatter(), arg);
                 } else if (arg instanceof Date) {
                    // format a Date if can
                    dest.formatAndAppend(getStockDateFormatter(), arg);
                } else {
                    dest.append(arg.toString());
                }
            } else if(argType==ArgType.CHOICE) {
                if (!(arg instanceof Number)) {
                    throw new IllegalArgumentException("'" + arg + "' is not a Number");
                }
                double number = ((Number)arg).doubleValue();
                int subMsgStart=findChoiceSubMessage(msgPattern, i, number);
                formatComplexSubMessage(subMsgStart, null, args, argsMap, nameValuePairs, dest);
            } else if(argType.hasPluralStyle()) {
                if (!(arg instanceof Number)) {
                    throw new IllegalArgumentException("'" + arg + "' is not a Number");
                }
                PluralSelectorProvider selector;
                if(argType == ArgType.PLURAL) {
                    if (pluralProvider == null) {
                        pluralProvider = new PluralSelectorProvider(this, PluralType.CARDINAL);
                    }
                    selector = pluralProvider;
                } else {
                    if (ordinalProvider == null) {
                        ordinalProvider = new PluralSelectorProvider(this, PluralType.ORDINAL);
                    }
                    selector = ordinalProvider;
                }
                Number number = (Number)arg;
                double offset=msgPattern.getPluralOffset(i);
                PluralSelectorContext context =
                        new PluralSelectorContext(i, argName, number, offset);
                int subMsgStart=PluralFormat.findSubMessage(
                        msgPattern, i, selector, context, number.doubleValue());
                formatComplexSubMessage(subMsgStart, context, args, argsMap, nameValuePairs, dest);
            } else if(argType==ArgType.SELECT) {
                int subMsgStart=SelectFormat.findSubMessage(msgPattern, i, arg.toString());
                formatComplexSubMessage(subMsgStart, null, args, argsMap, nameValuePairs, dest);
            } else {
                // This should never happen.
                throw new IllegalStateException("unexpected argType "+argType);
            }
            fp = updateMetaData(dest, prevDestLength, fp, argId);
            prevIndex=msgPattern.getPart(argLimit).getLimit();
            i=argLimit;
        }
    }

    private void formatComplexSubMessage(
            int msgStart, PluralSelectorContext pluralNumber,
            Object[] args, Map<String, Object> argsMap, Object[] nameValuePairs,
            AppendableWrapper dest) {
        if (!msgPattern.jdkAposMode()) {
            format(msgStart, pluralNumber, args, argsMap, nameValuePairs, dest, null);
            return;
        }
        // JDK compatibility mode: (see JDK MessageFormat.format() API docs)
        throw new UnsupportedOperationException("JDK apostrophe mode not supported");
        /*
        // - remove SKIP_SYNTAX; that is, remove half of the apostrophes
        // - if the result string contains an open curly brace '{' then
        //   instantiate a temporary MessageFormat object and format again;
        //   otherwise just append the result string
        String msgString = msgPattern.getPatternString();
        String subMsgString;
        StringBuilder sb = null;
        int prevIndex = msgPattern.getPart(msgStart).getLimit();
        for (int i = msgStart;;) {
            Part part = msgPattern.getPart(++i);
            Part.Type type = part.getType();
            int index = part.getIndex();
            if (type == Part.Type.MSG_LIMIT) {
                if (sb == null) {
                    subMsgString = msgString.substring(prevIndex, index);
                } else {
                    subMsgString = sb.append(msgString, prevIndex, index).toString();
                }
                break;
            } else if (type == Part.Type.REPLACE_NUMBER || type == Part.Type.SKIP_SYNTAX) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(msgString, prevIndex, index);
                if (type == Part.Type.REPLACE_NUMBER) {
                    if(pluralNumber.forReplaceNumber) {
                        // number-offset was already formatted.
                        sb.append(pluralNumber.numberString);
                    } else {
                        sb.append(getStockNumberFormatter().format(pluralNumber.number));
                    }
                }
                prevIndex = part.getLimit();
            } else if (type == Part.Type.ARG_START) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(msgString, prevIndex, index);
                prevIndex = index;
                i = msgPattern.getLimitPartIndex(i);
                index = msgPattern.getPart(i).getLimit();
                MessagePattern.appendReducedApostrophes(msgString, prevIndex, index, sb);
                prevIndex = index;
            }
        }
        if (subMsgString.indexOf('{') >= 0) {
            MessageFormat subMsgFormat = new MessageFormat("", ulocale);
            subMsgFormat.applyPattern(subMsgString, MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
            subMsgFormat.format(0, null, args, argsMap, dest, null);
        } else {
            dest.append(subMsgString);
        }
        */
    }

    /**
     * Read as much literal string from the pattern string as possible. This stops
     * as soon as it finds an argument, or it reaches the end of the string.
     * @param from Index in the pattern string to start from.
     * @return A substring from the pattern string representing the longest possible
     *         substring with no arguments.
     */
    private String getLiteralStringUntilNextArgument(int from) {
        StringBuilder b = new StringBuilder();
        String msgString=msgPattern.getPatternString();
        int prevIndex=msgPattern.getPart(from).getLimit();
        for(int i=from+1;; ++i) {
            Part part=msgPattern.getPart(i);
            Part.Type type=part.getType();
            int index=part.getIndex();
            b.append(msgString, prevIndex, index);
            if(type==Part.Type.ARG_START || type==Part.Type.MSG_LIMIT) {
                return b.toString();
            }
            assert type==Part.Type.SKIP_SYNTAX || type==Part.Type.INSERT_CHAR :
                    "Unexpected Part "+part+" in parsed message.";
            prevIndex=part.getLimit();
        }
    }

    private FieldPosition updateMetaData(AppendableWrapper dest, int prevLength,
                                         FieldPosition fp, Object argId) {
        if (dest.attributes != null && prevLength < dest.length) {
            dest.attributes.add(new AttributeAndPosition(argId, prevLength, dest.length));
        }
        if (fp != null && Field.ARGUMENT.equals(fp.getFieldAttribute())) {
            fp.setBeginIndex(prevLength);
            fp.setEndIndex(dest.length);
            return null;
        }
        return fp;
    }

    // This lives here because ICU4J does not have its own ChoiceFormat class.
    /**
     * Finds the ChoiceFormat sub-message for the given number.
     * @param pattern A MessagePattern.
     * @param partIndex the index of the first ChoiceFormat argument style part.
     * @param number a number to be mapped to one of the ChoiceFormat argument's intervals
     * @return the sub-message start part index.
     */
    private static int findChoiceSubMessage(MessagePattern pattern, int partIndex, double number) {
        int count=pattern.countParts();
        int msgStart;
        // Iterate over (ARG_INT|DOUBLE, ARG_SELECTOR, message) tuples
        // until ARG_LIMIT or end of choice-only pattern.
        // Ignore the first number and selector and start the loop on the first message.
        partIndex+=2;
        for(;;) {
            // Skip but remember the current sub-message.
            msgStart=partIndex;
            partIndex=pattern.getLimitPartIndex(partIndex);
            if(++partIndex>=count) {
                // Reached the end of the choice-only pattern.
                // Return with the last sub-message.
                break;
            }
            Part part=pattern.getPart(partIndex++);
            Part.Type type=part.getType();
            if(type==Part.Type.ARG_LIMIT) {
                // Reached the end of the ChoiceFormat style.
                // Return with the last sub-message.
                break;
            }
            // part is an ARG_INT or ARG_DOUBLE
            assert type.hasNumericValue();
            double boundary=pattern.getNumericValue(part);
            // Fetch the ARG_SELECTOR character.
            int selectorIndex=pattern.getPatternIndex(partIndex++);
            char boundaryChar=pattern.getPatternString().charAt(selectorIndex);
            if(boundaryChar=='<' ? !(number>boundary) : !(number>=boundary)) {
                // The number is in the interval between the previous boundary and the current one.
                // Return with the sub-message between them.
                // The !(a>b) and !(a>=b) comparisons are equivalent to
                // (a<=b) and (a<b) except they "catch" NaN.
                break;
            }
        }
        return msgStart;
    }

    // Ported from C++ ChoiceFormat::parse().
    private static double parseChoiceArgument(
            MessagePattern pattern, int partIndex,
            String source, ParsePosition pos) {
        // find the best number (defined as the one with the longest parse)
        int start = pos.getIndex();
        int furthest = start;
        double bestNumber = Double.NaN;
        double tempNumber = 0.0;
        while (pattern.getPartType(partIndex) != Part.Type.ARG_LIMIT) {
            tempNumber = pattern.getNumericValue(pattern.getPart(partIndex));
            partIndex += 2;  // skip the numeric part and ignore the ARG_SELECTOR
            int msgLimit = pattern.getLimitPartIndex(partIndex);
            int len = matchStringUntilLimitPart(pattern, partIndex, msgLimit, source, start);
            if (len >= 0) {
                int newIndex = start + len;
                if (newIndex > furthest) {
                    furthest = newIndex;
                    bestNumber = tempNumber;
                    if (furthest == source.length()) {
                        break;
                    }
                }
            }
            partIndex = msgLimit + 1;
        }
        if (furthest == start) {
            pos.setErrorIndex(start);
        } else {
            pos.setIndex(furthest);
        }
        return bestNumber;
    }

    /**
     * Matches the pattern string from the end of the partIndex to
     * the beginning of the limitPartIndex,
     * including all syntax except SKIP_SYNTAX,
     * against the source string starting at sourceOffset.
     * If they match, returns the length of the source string match.
     * Otherwise returns -1.
     */
    private static int matchStringUntilLimitPart(
            MessagePattern pattern, int partIndex, int limitPartIndex,
            String source, int sourceOffset) {
        int matchingSourceLength = 0;
        String msgString = pattern.getPatternString();
        int prevIndex = pattern.getPart(partIndex).getLimit();
        for (;;) {
            Part part = pattern.getPart(++partIndex);
            if (partIndex == limitPartIndex || part.getType() == Part.Type.SKIP_SYNTAX) {
                int index = part.getIndex();
                int length = index - prevIndex;
                if (length != 0 && !source.regionMatches(sourceOffset, msgString, prevIndex, length)) {
                    return -1;  // mismatch
                }
                matchingSourceLength += length;
                if (partIndex == limitPartIndex) {
                    return matchingSourceLength;
                }
                prevIndex = part.getLimit();  // SKIP_SYNTAX
            }
        }
    }

    /**
     * Finds the "other" sub-message.
     * @param partIndex the index of the first PluralFormat argument style part.
     * @return the "other" sub-message start part index.
     */
    private int findOtherSubMessage(int partIndex) {
        int count=msgPattern.countParts();
        MessagePattern.Part part=msgPattern.getPart(partIndex);
        if(part.getType().hasNumericValue()) {
            ++partIndex;
        }
        // Iterate over (ARG_SELECTOR [ARG_INT|ARG_DOUBLE] message) tuples
        // until ARG_LIMIT or end of plural-only pattern.
        do {
            part=msgPattern.getPart(partIndex++);
            MessagePattern.Part.Type type=part.getType();
            if(type==MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            assert type==MessagePattern.Part.Type.ARG_SELECTOR;
            // part is an ARG_SELECTOR followed by an optional explicit value, and then a message
            if(msgPattern.partSubstringMatches(part, "other")) {
                return partIndex;
            }
            if(msgPattern.getPartType(partIndex).hasNumericValue()) {
                ++partIndex;  // skip the numeric-value part of "=1" etc.
            }
            partIndex=msgPattern.getLimitPartIndex(partIndex);
        } while(++partIndex<count);
        return 0;
    }

    /**
     * Returns the ARG_START index of the first occurrence of the plural number in a sub-message.
     * Returns -1 if it is a REPLACE_NUMBER.
     * Returns 0 if there is neither.
     */
    private int findFirstPluralNumberArg(int msgStart, String argName) {
        for(int i=msgStart+1;; ++i) {
            Part part=msgPattern.getPart(i);
            Part.Type type=part.getType();
            if(type==Part.Type.MSG_LIMIT) {
                return 0;
            }
            if(type==Part.Type.REPLACE_NUMBER) {
                return -1;
            }
            if(type==Part.Type.ARG_START) {
                ArgType argType=part.getArgType();
                if(argName.length()!=0 && (argType==ArgType.NONE || argType==ArgType.SIMPLE)) {
                    part=msgPattern.getPart(i+1);  // ARG_NUMBER or ARG_NAME
                    if(msgPattern.partSubstringMatches(part, argName)) {
                        return i;
                    }
                }
                i=msgPattern.getLimitPartIndex(i);
            }
        }
    }

    /**
     * Mutable input/output values for the PluralSelectorProvider.
     * Separate so that it is possible to make MessageFormat Freezable.
     */
    private static final class PluralSelectorContext {
        private PluralSelectorContext(int start, String name, Number num, double off) {
            startIndex = start;
            argName = name;
            // number needs to be set even when select() is not called.
            // Keep it as a Number/Formattable:
            // For format() methods, and to preserve information (e.g., BigDecimal).
            if(off == 0) {
                number = num;
            } else {
                number = num.doubleValue() - off;
            }
            offset = off;
        }
        @Override
        public String toString() {
            throw new AssertionError("PluralSelectorContext being formatted, rather than its number");
        }

        // Input values for plural selection with decimals.
        int startIndex;
        String argName;
        /** argument number - plural offset */
        Number number;
        double offset;
        // Output values for plural selection with decimals.
        /** -1 if REPLACE_NUMBER, 0 arg not found, >0 ARG_START index */
        int numberArgIndex;
        Format formatter;
        /** formatted argument number - plural offset */
        String numberString;
        /** true if number-offset was formatted with the stock number formatter */
        boolean forReplaceNumber;
    }

    /**
     * This provider helps defer instantiation of a PluralRules object
     * until we actually need to select a keyword.
     * For example, if the number matches an explicit-value selector like "=1"
     * we do not need any PluralRules.
     */
    private static final class PluralSelectorProvider implements PluralFormat.PluralSelector {
        public PluralSelectorProvider(MessageFormat mf, PluralType type) {
            msgFormat = mf;
            this.type = type;
        }
        @Override
        public String select(Object ctx, double number) {
            if(rules == null) {
                rules = PluralRules.forLocale(msgFormat.locale_, type);
            }
            // Select a sub-message according to how the number is formatted,
            // which is specified in the selected sub-message.
            // We avoid this circle by looking at how
            // the number is formatted in the "other" sub-message
            // which must always be present and usually contains the number.
            // Message authors should be consistent across sub-messages.
            PluralSelectorContext context = (PluralSelectorContext)ctx;
            int otherIndex = msgFormat.findOtherSubMessage(context.startIndex);
            context.numberArgIndex = msgFormat.findFirstPluralNumberArg(otherIndex, context.argName);
            if(context.numberArgIndex > 0 && msgFormat.cachedFormatters != null) {
                context.formatter = msgFormat.cachedFormatters.get(context.numberArgIndex);
            }
            if(context.formatter == null) {
                context.formatter = msgFormat.getStockNumberFormatter();
                context.forReplaceNumber = true;
            }
            assert context.number.doubleValue() == number;  // argument number minus the offset
            context.numberString = context.formatter.format(context.number);
            /* TODO: Try to get FixedDecimal from formatted string.
            if(context.formatter instanceof DecimalFormat) {
                FixedDecimal dec = ((DecimalFormat)context.formatter).getFixedDecimal(number);
                return rules.select(dec);
            } else */ {
                return rules.select(number);
            }
        }
        private MessageFormat msgFormat;
        private PluralRules rules;
        private PluralType type;
    }

    @SuppressWarnings("unchecked")
    private void format(Object arguments, AppendableWrapper result, FieldPosition fp) {
        if ((arguments == null || arguments instanceof Map)) {
            format(null, (Map<String, Object>)arguments, result, fp);
        } else {
            format((Object[])arguments, null, result, fp);
        }
    }

    /**
     * Internal routine used by format.
     *
     * @throws IllegalArgumentException if an argument in the
     *         <code>arguments</code> map is not of the type
     *         expected by the format element(s) that use it.
     */
    private void format(Object[] arguments, Map<String, Object> argsMap,
                        AppendableWrapper dest, FieldPosition fp) {
        if (arguments != null && msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException(
                "This method is not available in MessageFormat objects " +
                "that use alphanumeric argument names.");
        }
        format(0, null, arguments, argsMap, null, dest, fp);
    }

    private void resetPattern() {
        if (msgPattern != null) {
            msgPattern.clear();
        }
        if (cachedFormatters != null) {
            cachedFormatters.clear();
        }
        customFormatArgStarts = null;
    }

    private static final String[] typeList =
        { "number", "date", "time", "spellout", "ordinal", "duration" };
    private static final int
        TYPE_NUMBER = 0,
        TYPE_DATE = 1,
        TYPE_TIME = 2;
//        TYPE_SPELLOUT = 3,
//        TYPE_ORDINAL = 4,
//        TYPE_DURATION = 5;

    private static final String[] modifierList =
        {"", "currency", "percent", "integer"};

    private static final int
        MODIFIER_EMPTY = 0,
        MODIFIER_CURRENCY = 1,
        MODIFIER_PERCENT = 2,
        MODIFIER_INTEGER = 3;

    private static final String[] dateModifierList =
        {"", "short", "medium", "long", "full"};

    private static final int
        DATE_MODIFIER_EMPTY = 0,
        DATE_MODIFIER_SHORT = 1,
        DATE_MODIFIER_MEDIUM = 2,
        DATE_MODIFIER_LONG = 3,
        DATE_MODIFIER_FULL = 4;

    Format dateTimeFormatForPatternOrSkeleton(String style) {
        // Ignore leading whitespace when looking for "::", the skeleton signal sequence
        int i = PatternProps.skipWhiteSpace(style, 0);
        if (style.regionMatches(i, "::", 0, 2)) { // Skeleton
            DateTimeFormatter df = new DateTimeFormatter(context_,
                DateTimeFormatterSkeletonOptions.fromString(style.substring(i + 2)),
                locale_);
            return new DateTimeFormatterAsFormat(df);
        } else { // Pattern
            return new SimpleDateFormat(style, locale_);
        }
    }

    // Creates an appropriate Format object for the type and style passed.
    // Both arguments cannot be null.
    private Format createAppropriateFormat(String type, String style) {
        Format newFormat = null;
        int subformatType  = findKeyword(type, typeList);
        switch (subformatType){
        case TYPE_NUMBER:
            switch (findKeyword(style, modifierList)) {
            case MODIFIER_EMPTY:
                newFormat = NumberFormat.getInstance(locale_);
                break;
            case MODIFIER_CURRENCY:
                newFormat = NumberFormat.getCurrencyInstance(locale_);
                break;
            case MODIFIER_PERCENT:
                newFormat = NumberFormat.getPercentInstance(locale_);
                break;
            case MODIFIER_INTEGER:
                newFormat = NumberFormat.getIntegerInstance(locale_);
                break;
            default: // pattern
                newFormat = new DecimalFormat(style,
                        new DecimalFormatSymbols(locale_));
                break;
            }
            break;
        case TYPE_DATE:
            switch (findKeyword(style, dateModifierList)) {
            case DATE_MODIFIER_EMPTY:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                            DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.DEFAULT),
                                locale_));
                break;
            case DATE_MODIFIER_SHORT:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.SHORT),
                        locale_));
                break;
            case DATE_MODIFIER_MEDIUM:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.DEFAULT),
                        locale_));
                break;
            case DATE_MODIFIER_LONG:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.LONG),
                        locale_));
                break;
            case DATE_MODIFIER_FULL:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createDateInstance(DateFormat.FULL),
                        locale_));
                break;
            default: // pattern or skeleton
                newFormat = dateTimeFormatForPatternOrSkeleton(style);
                break;
            }
            break;
        case TYPE_TIME:
            switch (findKeyword(style, dateModifierList)) {
            case DATE_MODIFIER_EMPTY:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createTimeInstance(DateFormat.DEFAULT),
                        locale_));
                break;
            case DATE_MODIFIER_SHORT:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createTimeInstance(DateFormat.SHORT),
                        locale_));
                break;
            case DATE_MODIFIER_MEDIUM:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createTimeInstance(DateFormat.DEFAULT),
                        locale_));
                break;
            case DATE_MODIFIER_LONG:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createTimeInstance(DateFormat.LONG),
                        locale_));
                break;
            case DATE_MODIFIER_FULL:
                newFormat = new DateTimeFormatterAsFormat(
                    new DateTimeFormatter(
                        DateTimeFormatterJdkStyleOptions.createTimeInstance(DateFormat.FULL),
                        locale_));
                break;
            default: // pattern or skeleton
                newFormat = dateTimeFormatForPatternOrSkeleton(style);
                break;
            }
            break;
        /* There is no java.text.RuleBasedNumberFormat --
        case TYPE_SPELLOUT:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale,
                        RuleBasedNumberFormat.SPELLOUT);
                String ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        case TYPE_ORDINAL:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale,
                        RuleBasedNumberFormat.ORDINAL);
                String ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        case TYPE_DURATION:
            {
                RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ulocale,
                        RuleBasedNumberFormat.DURATION);
                String ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    }
                    catch (Exception e) {
                        // warn invalid ruleset
                    }
                }
                newFormat = rbnf;
            }
            break;
        */
        default:
            throw new IllegalArgumentException("Unknown format type \"" + type + "\"");
        }
        return newFormat;
    }

    private static final Locale rootLocale = new Locale("");  // Locale.ROOT only @since 1.6

    private static final int findKeyword(String s, String[] list) {
        s = PatternProps.trimWhiteSpace(s).toLowerCase(rootLocale);
        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i]))
                return i;
        }
        return -1;
    }

    private void cacheExplicitFormats() {
        if (cachedFormatters != null) {
            cachedFormatters.clear();
        }
        customFormatArgStarts = null;
        // The last two "parts" can at most be ARG_LIMIT and MSG_LIMIT
        // which we need not examine.
        int limit = msgPattern.countParts() - 2;
        // This loop starts at part index 1 because we do need to examine
        // ARG_START parts. (But we can ignore the MSG_START.)
        for(int i=1; i < limit; ++i) {
            Part part = msgPattern.getPart(i);
            if(part.getType()!=Part.Type.ARG_START) {
                continue;
            }
            ArgType argType=part.getArgType();
            if(argType != ArgType.SIMPLE) {
                continue;
            }
            int index = i;
            i += 2;
            String explicitType = msgPattern.getSubstring(msgPattern.getPart(i++));
            String style = "";
            if ((part = msgPattern.getPart(i)).getType() == MessagePattern.Part.Type.ARG_STYLE) {
                style = msgPattern.getSubstring(part);
                ++i;
            }
            Format formatter = createAppropriateFormat(explicitType, style);
            setArgStartFormat(index, formatter);
        }
    }

    /**
     * Sets a formatter for a MessagePattern ARG_START part index.
     */
    private void setArgStartFormat(int argStart, Format formatter) {
        if (cachedFormatters == null) {
            cachedFormatters = new HashMap<>();
        }
        cachedFormatters.put(argStart, formatter);
    }

    /**
     * Sets a custom formatter for a MessagePattern ARG_START part index.
     * "Custom" formatters are provided by the user via setFormat() or similar APIs.
     */
    private void setCustomArgStartFormat(int argStart, Format formatter) {
        setArgStartFormat(argStart, formatter);
        if (customFormatArgStarts == null) {
            customFormatArgStarts = new HashSet<>();
        }
        customFormatArgStarts.add(argStart);
    }

    private static final char SINGLE_QUOTE = '\'';
    private static final char CURLY_BRACE_LEFT = '{';
    private static final char CURLY_BRACE_RIGHT = '}';

    private static final int STATE_INITIAL = 0;
    private static final int STATE_SINGLE_QUOTE = 1;
    private static final int STATE_IN_QUOTE = 2;
    private static final int STATE_MSG_ELEMENT = 3;

    /**
     * {icu_annot::icu} Converts an 'apostrophe-friendly' pattern into a standard
     * pattern.
     * <em>This is obsolete for ICU 4.8 and higher MessageFormat pattern strings.</em>
     * It can still be useful together with {@link java.text.MessageFormat}.
     *
     * <p>See the class description for more about apostrophes and quoting,
     * and differences between ICU and {@link java.text.MessageFormat}.
     *
     * <p>{@link java.text.MessageFormat} and ICU 4.6 and earlier MessageFormat
     * treat all ASCII apostrophes as
     * quotes, which is problematic in some languages, e.g.
     * French, where apostrophe is commonly used.  This utility
     * assumes that only an unpaired apostrophe immediately before
     * a brace is a true quote.  Other unpaired apostrophes are paired,
     * and the resulting standard pattern string is returned.
     *
     * <p><b>Note</b>: It is not guaranteed that the returned pattern
     * is indeed a valid pattern.  The only effect is to convert
     * between patterns having different quoting semantics.
     *
     * <p><b>Note</b>: This method only works on top-level messageText,
     * not messageText nested inside a complexArg.
     *
     * @param pattern the 'apostrophe-friendly' pattern to convert
     * @return the standard equivalent of the original pattern
     * icu_annot::stable ICU 3.4
     */
    public static String autoQuoteApostrophe(String pattern) {
        StringBuilder buf = new StringBuilder(pattern.length() * 2);
        int state = STATE_INITIAL;
        int braceCount = 0;
        for (int i = 0, j = pattern.length(); i < j; ++i) {
            char c = pattern.charAt(i);
            switch (state) {
            case STATE_INITIAL:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_SINGLE_QUOTE;
                    break;
                case CURLY_BRACE_LEFT:
                    state = STATE_MSG_ELEMENT;
                    ++braceCount;
                    break;
                }
                break;
            case STATE_SINGLE_QUOTE:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_INITIAL;
                    break;
                case CURLY_BRACE_LEFT:
                case CURLY_BRACE_RIGHT:
                    state = STATE_IN_QUOTE;
                    break;
                default:
                    buf.append(SINGLE_QUOTE);
                    state = STATE_INITIAL;
                    break;
                }
                break;
            case STATE_IN_QUOTE:
                switch (c) {
                case SINGLE_QUOTE:
                    state = STATE_INITIAL;
                    break;
                }
                break;
            case STATE_MSG_ELEMENT:
                switch (c) {
                case CURLY_BRACE_LEFT:
                    ++braceCount;
                    break;
                case CURLY_BRACE_RIGHT:
                    if (--braceCount == 0) {
                        state = STATE_INITIAL;
                    }
                    break;
                }
                break;
            ///CLOVER:OFF
            default: // Never happens.
                break;
            ///CLOVER:ON
            }
            buf.append(c);
        }
        // End of scan
        if (state == STATE_SINGLE_QUOTE || state == STATE_IN_QUOTE) {
            buf.append(SINGLE_QUOTE);
        }
        return new String(buf);
    }

    /**
     * Convenience wrapper for Appendable, tracks the result string length.
     * Also, Appendable throws IOException, and we turn that into a RuntimeException
     * so that we need no throws clauses.
     */
    private static final class AppendableWrapper {
        public AppendableWrapper(StringBuilder sb) {
            app = sb;
            length = sb.length();
            attributes = null;
        }

        public AppendableWrapper(StringBuffer sb) {
            app = sb;
            length = sb.length();
            attributes = null;
        }

        public void useAttributes() {
            attributes = new ArrayList<>();
        }

        public void append(CharSequence s) {
            try {
                app.append(s);
                length += s.length();
            } catch(IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharSequence s, int start, int limit) {
            try {
                app.append(s, start, limit);
                length += limit - start;
            } catch(IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharacterIterator iterator) {
            length += append(app, iterator);
        }

        public static int append(Appendable result, CharacterIterator iterator) {
            try {
                int start = iterator.getBeginIndex();
                int limit = iterator.getEndIndex();
                int length = limit - start;
                if (start < limit) {
                    result.append(iterator.first());
                    while (++start < limit) {
                        result.append(iterator.next());
                    }
                }
                return length;
            } catch(IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void formatAndAppend(Format formatter, Object arg) {
            if (attributes == null) {
                append(formatter.format(arg));
            } else {
                AttributedCharacterIterator formattedArg = formatter.formatToCharacterIterator(arg);
                int prevLength = length;
                append(formattedArg);
                // Copy all of the attributes from formattedArg to our attributes list.
                formattedArg.first();
                int start = formattedArg.getIndex();  // Should be 0 but might not be.
                int limit = formattedArg.getEndIndex();  // == start + length - prevLength
                int offset = prevLength - start;  // Adjust attribute indexes for the result string.
                while (start < limit) {
                    Map<Attribute, Object> map = formattedArg.getAttributes();
                    int runLimit = formattedArg.getRunLimit();
                    if (map.size() != 0) {
                        for (Map.Entry<Attribute, Object> entry : map.entrySet()) {
                           attributes.add(
                               new AttributeAndPosition(
                                   entry.getKey(), entry.getValue(),
                                   offset + start, offset + runLimit));
                        }
                    }
                    start = runLimit;
                    formattedArg.setIndex(start);
                }
            }
        }

        public void formatAndAppend(Format formatter, Object arg, String argString) {
            if (attributes == null && argString != null) {
                append(argString);
            } else {
                formatAndAppend(formatter, arg);
            }
        }

        private Appendable app;
        private int length;
        private List<AttributeAndPosition> attributes;
    }

    private static final class AttributeAndPosition {
        /**
         * Defaults the field to Field.ARGUMENT.
         */
        public AttributeAndPosition(Object fieldValue, int startIndex, int limitIndex) {
            init(Field.ARGUMENT, fieldValue, startIndex, limitIndex);
        }

        public AttributeAndPosition(Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            init(field, fieldValue, startIndex, limitIndex);
        }

        public void init(Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            key = field;
            value = fieldValue;
            start = startIndex;
            limit = limitIndex;
        }

        private Attribute key;
        private Object value;
        private int start;
        private int limit;
    }
}
