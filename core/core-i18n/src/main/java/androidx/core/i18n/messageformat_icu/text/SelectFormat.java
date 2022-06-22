/*
 *******************************************************************************
 * Copyright (C) 2004-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 * Copyright (C) 2009 , Yahoo! Inc.                                            *
 *******************************************************************************
 */
package androidx.core.i18n.messageformat_icu.text;

import androidx.annotation.RestrictTo;
import androidx.core.i18n.messageformat_icu.impl.PatternProps;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * <p><code>SelectFormat</code> supports the creation of  internationalized
 * messages by selecting phrases based on keywords. The pattern  specifies
 * how to map keywords to phrases and provides a default phrase. The
 * object provided to the format method is a string that's matched
 * against the keywords. If there is a match, the corresponding phrase
 * is selected; otherwise, the default phrase is used.</p>
 *
 * <h4>Using <code>SelectFormat</code> for Gender Agreement</h4>
 *
 * <p>Note: Typically, select formatting is done via <code>MessageFormat</code>
 * with a <code>select</code> argument type,
 * rather than using a stand-alone <code>SelectFormat</code>.</p>
 *
 * <p>The main use case for the select format is gender based  inflection.
 * When names or nouns are inserted into sentences, their gender can  affect pronouns,
 * verb forms, articles, and adjectives. Special care needs to be
 * taken for the case where the gender cannot be determined.
 * The impact varies between languages:</p>
 *
 * <ul>
 * <li>English has three genders, and unknown gender is handled as a  special
 * case. Names use the gender of the named person (if known), nouns  referring
 * to people use natural gender, and inanimate objects are usually  neutral.
 * The gender only affects pronouns: "he", "she", "it", "they".
 *
 * <li>German differs from English in that the gender of nouns is  rather
 * arbitrary, even for nouns referring to people ("M&#u00E4;dchen", girl, is  neutral).
 * The gender affects pronouns ("er", "sie", "es"), articles ("der",  "die",
 * "das"), and adjective forms ("guter Mann", "gute Frau", "gutes  M&#u00E4;dchen").
 *
 * <li>French has only two genders; as in German the gender of nouns
 * is rather arbitrary - for sun and moon, the genders
 * are the opposite of those in German. The gender affects
 * pronouns ("il", "elle"), articles ("le", "la"),
 * adjective forms ("bon", "bonne"), and sometimes
 * verb forms ("all&#u00E9;", "all&#u00E9e;").
 *
 * <li>Polish distinguishes five genders (or noun classes),
 * human masculine, animate non-human masculine, inanimate masculine,
 * feminine, and neuter.
 * </ul>
 *
 * <p>Some other languages have noun classes that are not related to  gender,
 * but similar in grammatical use.
 * Some African languages have around 20 noun classes.</p>
 *
 * <p><b>Note:</b>For the gender of a <i>person</i> in a given sentence,
 * we usually need to distinguish only between female, male and other/unknown.</p>
 *
 * <p>To enable localizers to create sentence patterns that take their
 * language's gender dependencies into consideration, software has to  provide
 * information about the gender associated with a noun or name to
 * <code>MessageFormat</code>.
 * Two main cases can be distinguished:</p>
 *
 * <ul>
 * <li>For people, natural gender information should be maintained  for each person.
 * Keywords like "male", "female", "mixed" (for groups of people)
 * and "unknown" could be used.
 *
 * <li>For nouns, grammatical gender information should be maintained  for
 * each noun and per language, e.g., in resource bundles.
 * The keywords "masculine", "feminine", and "neuter" are commonly  used,
 * but some languages may require other keywords.
 * </ul>
 *
 * <p>The resulting keyword is provided to <code>MessageFormat</code>  as a
 * parameter separate from the name or noun it's associated with. For  example,
 * to generate a message such as "Jean went to Paris", three separate  arguments
 * would be provided: The name of the person as argument 0, the  gender of
 * the person as argument 1, and the name of the city as argument 2.
 * The sentence pattern for English, where the gender of the person has
 * no impact on this simple sentence, would not refer to argument 1  at all:</p>
 *
 * <pre>{0} went to {2}.</pre>
 *
 * <p><b>Note:</b> The entire sentence should be included (and partially repeated)
 * inside each phrase. Otherwise translators would have to be trained on how to
 * move bits of the sentence in and out of the select argument of a message.
 * (The examples below do not follow this recommendation!)</p>
 * 
 * <p>The sentence pattern for French, where the gender of the person affects
 * the form of the participle, uses a select format based on argument 1:</p>
 *
 * <pre>{0} est {1, select, female {all&#u00E9;e} other {all&#u00E9;}} &#u00E0; {2}.</pre>
 *
 * <p>Patterns can be nested, so that it's possible to handle  interactions of
 * number and gender where necessary. For example, if the above  sentence should
 * allow for the names of several people to be inserted, the  following sentence
 * pattern can be used (with argument 0 the list of people's names,  
 * argument 1 the number of people, argument 2 their combined gender, and  
 * argument 3 the city name):</p>
 *
 * <pre>{0} {1, plural, 
 * one {est {2, select, female {all&#u00E9;e} other  {all&#u00E9;}}}
 * other {sont {2, select, female {all&#u00E9;es} other {all&#u00E9;s}}}
 * }&#u00E0; {3}.</pre>
 *
 * <h4>Patterns and Their Interpretation</h4>
 *
 * <p>The <code>SelectFormat</code> pattern string defines the phrase  output
 * for each user-defined keyword.
 * The pattern is a sequence of (keyword, message) pairs.
 * A keyword is a "pattern identifier": [^[[:Pattern_Syntax:][:Pattern_White_Space:]]]+</p>
 *
 * <p>Each message is a MessageFormat pattern string enclosed in {curly braces}.</p>
 *
 * <p>You always have to define a phrase for the default keyword
 * <code>other</code>; this phrase is returned when the keyword  
 * provided to
 * the <code>format</code> method matches no other keyword.
 * If a pattern does not provide a phrase for <code>other</code>, the  method
 * it's provided to returns the error  <code>U_DEFAULT_KEYWORD_MISSING</code>.
 * <br/>
 * Pattern_White_Space between keywords and messages is ignored.
 * Pattern_White_Space within a message is preserved and output.</p>
 *
 * <p><pre>Example:
 * MessageFormat msgFmt = new MessageFormat("{0} est " +
 *     "{1, select, female {all&#u00E9;e} other {all&#u00E9;}} &#u00E0; Paris.",
 *     new ULocale("fr"));
 * Object args[] = {"Kirti","female"};
 * System.out.println(msgFmt.format(args));
 * </pre>
 * <p>
 * Produces the output:<br/>
 * <code>Kirti est all&#u00E9;e &#u00E0; Paris.</code>
 * </p>
 *
 * icu_annot::stable ICU 4.4
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectFormat extends Format{
    // Generated by serialver from JDK 1.5
    private static final long serialVersionUID = 2993154333257524984L;

    /*
     * The applied pattern string.
     */
    private String pattern = null;

    /**
     * The MessagePattern which contains the parsed structure of the pattern string.
     */
    transient private MessagePattern msgPattern;
    
    /**
     * Creates a new <code>SelectFormat</code> for a given pattern string.
     * @param  pattern the pattern for this <code>SelectFormat</code>.
     * icu_annot::stable ICU 4.4
     */
    public SelectFormat(String pattern) {
        applyPattern(pattern);
    }

    /*
     * Resets the <code>SelectFormat</code> object.
     */
    private void reset() {
        pattern = null;
        if(msgPattern != null) {
            msgPattern.clear();
        }
    }

    /**
     * Sets the pattern used by this select format.
     * Patterns and their interpretation are specified in the class description.
     *
     * @param pattern the pattern for this select format.
     * @throws IllegalArgumentException when the pattern is not a valid select format pattern.
     * icu_annot::stable ICU 4.4
     */
    public void applyPattern(String pattern) {
        this.pattern = pattern;
        if (msgPattern == null) {
            msgPattern = new MessagePattern();
        }
        try {
            msgPattern.parseSelectStyle(pattern);
        } catch(RuntimeException e) {
            reset();
            throw e;
        }
    }

    /**
     * Returns the pattern for this <code>SelectFormat</code>
     *
     * @return the pattern string
     * icu_annot::stable ICU 4.4
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Finds the SelectFormat sub-message for the given keyword, or the "other" sub-message.
     * @param pattern A MessagePattern.
     * @param partIndex the index of the first SelectFormat argument style part.
     * @param keyword a keyword to be matched to one of the SelectFormat argument's keywords.
     * @return the sub-message start part index.
     */
    public static int findSubMessage(MessagePattern pattern, int partIndex, String keyword) {
        int count=pattern.countParts();
        int msgStart=0;
        // Iterate over (ARG_SELECTOR, message) pairs until ARG_LIMIT or end of select-only pattern.
        do {
            MessagePattern.Part part=pattern.getPart(partIndex++);
            MessagePattern.Part.Type type=part.getType();
            if(type==MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            assert type==MessagePattern.Part.Type.ARG_SELECTOR;
            // part is an ARG_SELECTOR followed by a message
            if(pattern.partSubstringMatches(part, keyword)) {
                // keyword matches
                return partIndex;
            } else if(msgStart==0 && pattern.partSubstringMatches(part, "other")) {
                msgStart=partIndex;
            }
            partIndex=pattern.getLimitPartIndex(partIndex);
        } while(++partIndex<count);
        return msgStart;
    }

    /**
     * Selects the phrase for the given keyword.
     *
     * @param keyword a phrase selection keyword.
     * @return the string containing the formatted select message.
     * @throws IllegalArgumentException when the given keyword is not a "pattern identifier"
     * icu_annot::stable ICU 4.4
     */
    public final String format(String keyword) {
        //Check for the validity of the keyword
        if (!PatternProps.isIdentifier(keyword)) {
            throw new IllegalArgumentException("Invalid formatting argument.");
        }
        // If no pattern was applied, throw an exception
        if (msgPattern == null || msgPattern.countParts() == 0) {
            throw new IllegalStateException("Invalid format error.");
        }

        // Get the appropriate sub-message.
        int msgStart = findSubMessage(msgPattern, 0, keyword);
        if (!msgPattern.jdkAposMode()) {
            int msgLimit = msgPattern.getLimitPartIndex(msgStart);
            return msgPattern.getPatternString().substring(msgPattern.getPart(msgStart).getLimit(),
                                                           msgPattern.getPatternIndex(msgLimit));
        }
        // JDK compatibility mode: Remove SKIP_SYNTAX.
        StringBuilder result = null;
        int prevIndex = msgPattern.getPart(msgStart).getLimit();
        for (int i = msgStart;;) {
            MessagePattern.Part part = msgPattern.getPart(++i);
            MessagePattern.Part.Type type = part.getType();
            int index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                if (result == null) {
                    return pattern.substring(prevIndex, index);
                } else {
                    return result.append(pattern, prevIndex, index).toString();
                }
            } else if (type == MessagePattern.Part.Type.SKIP_SYNTAX) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(pattern, prevIndex, index);
                prevIndex = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(pattern, prevIndex, index);
                prevIndex = index;
                i = msgPattern.getLimitPartIndex(i);
                index = msgPattern.getPart(i).getLimit();
                MessagePattern.appendReducedApostrophes(pattern, prevIndex, index, result);
                prevIndex = index;
            }
        }
    }

    /**
     * Selects the phrase for the given keyword.
     * and appends the formatted message to the given <code>StringBuffer</code>.
     * @param keyword a phrase selection keyword.
     * @param toAppendTo the selected phrase will be appended to this
     *        <code>StringBuffer</code>.
     * @param pos will be ignored by this method.
     * @throws IllegalArgumentException when the given keyword is not a String
     *         or not a "pattern identifier"
     * @return the string buffer passed in as toAppendTo, with formatted text
     *         appended.
     * icu_annot::stable ICU 4.4
     */
    @Override
    public StringBuffer format(Object keyword, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (keyword instanceof String) {
            toAppendTo.append(format( (String)keyword));
        }else{
            throw new IllegalArgumentException("'" + keyword + "' is not a String");
        }
        return toAppendTo;
    }

    /**
     * This method is not supported by <code>SelectFormat</code>.
     * @param source the string to be parsed.
     * @param pos defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not supported.
     * @throws UnsupportedOperationException thrown always.
     * icu_annot::stable ICU 4.4
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 4.4
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
//        if(obj == null || getClass() != obj.getClass()) {
        if(obj == null || obj instanceof SelectFormat) {
            return false;
        }
        SelectFormat sf = (SelectFormat) obj;
        return msgPattern == null ? sf.msgPattern == null : msgPattern.equals(sf.msgPattern);
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 4.4
     */
    @Override
    public int hashCode() {
        if (pattern != null) {
            return pattern.hashCode();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * icu_annot::stable ICU 4.4
     */
    @Override
    public String toString() {
        return "pattern='" + pattern + "'";
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (pattern != null) {
            applyPattern(pattern);
        }
    }
}
