/*
 * checkstyle: Checks Java source code for adherence to a set of rules.
 * Copyright (C) 2001-2016 the original author or authors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.android.support.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtility;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to verify that both an annotation and javadoc tag are
 * present when either one is present.
 * <p>
 * Typically, both ways of flagging APIs serve their own purposes. Annotations
 * are used for compilers and development tools, while javadoc tags are used
 * for documentation.
 * <p>
 * In some cases, the presence of an annotations implies the presence of a
 * javadoc tag (or vice versa). For example, in the case of the
 * {@literal @}Deprecated annotation, the {@literal @}deprecated tag should
 * also be present. In the case of the {@literal @}RestrictTo tag, the
 * {@literal @}hide tag should also be present.
 * <p>
 * To configure this check, do the following:
 * <pre>
 *     &lt;module name="MismatchedAnnotationCheck"&gt;
 *       &lt;property name="tag" value="hide" /&gt;
 *       &lt;property name="annotation" value="android.support.annotation.RestrictTo" /&gt;
 *       &lt;property name="messageKey" value="annotation.missing.hide" /&gt;
 *       &lt;message key="annotation.missing.hide"
 *                   value="Must include both {@literal @}RestrictTo annotation
 *                          and {@literal @}hide Javadoc tag." /&gt;
 *     &lt;/module&gt;
 * </pre>
 * <p>
 * Additionally, you can configure this check with skipNoJavadoc option to
 * ignore cases when JavaDoc is missing and the annotation is present::
 * <pre>
 *     &lt;property name="skipNoJavadoc" value="true" /&gt;
 * </pre>
 * <p>
 * Examples of validating source code with skipNoJavadoc:
 * <pre>
 * <code>
 * {@literal @}RestrictTo
 * public static final int MY_CONST = 123456; // no violation
 *
 * &#47;** This javadoc is missing hide tag. *&#47;
 * {@literal @}RestrictTo
 * public static final int COUNTER = 10; // violation as javadoc exists
 * </code>
 * </pre>
 */
@SuppressWarnings("unused")
public final class MismatchedAnnotationCheck extends AbstractCheck {

    /** Key for the warning message text by check properties. */
    private static final String MSG_KEY_JAVADOC_DUPLICATE_TAG = "javadoc.duplicateTag";

    /** Key for the warning message text by check properties. */
    private static final String MSG_KEY_JAVADOC_MISSING = "javadoc.missing";

    /** Compiled regexp to look for a continuation of the comment. */
    private static final Pattern MATCH_HIDE_MULTILINE_CONT =
            CommonUtils.createPattern("(\\*/|@|[^\\s\\*])");

    /** Multiline finished at end of comment. */
    private static final String END_JAVADOC = "*/";

    /** Multiline finished at next Javadoc. */
    private static final String NEXT_TAG = "@";

    /** Javadoc tag. */
    private String mTag;

    /** Compiled regexp to match Javadoc tag with no argument. */
    private Pattern mMatchTag;

    /** Compiled regexp to match first part of multilineJavadoc tags. */
    private Pattern mMatchTagMultilineStart;

    /** Simple annotation name. */
    private String mAnnotationSimpleName;

    /** Fully-qualified annotation name. */
    private String mAnnotation;

    /** Key for the warning message text specified by check properties. */
    private String mMessageKey;

    /** Is tagged element valid without javadoc? */
    private boolean mSkipNoJavadoc;

    /**
     * Sets skipJavadoc value.
     *
     * @param skipNoJavadoc user's value of skipJavadoc
     */
    @SuppressWarnings("unused")
    public void setSkipNoJavadoc(boolean skipNoJavadoc) {
        mSkipNoJavadoc = skipNoJavadoc;
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    /**
     * Sets javadoc tag.
     *
     * @param tag javadoc tag to check
     */
    @SuppressWarnings("unused")
    public void setTag(String tag) {
        mTag = tag;

        mMatchTag = CommonUtils.createPattern("@(" + tag + ")\\s+\\S");
        mMatchTagMultilineStart = CommonUtils.createPattern("@(" + tag + ")\\s*$");
    }

    /**
     * Sets annotation tag.
     *
     * @param annotation annotation to check
     */
    @SuppressWarnings("unused")
    public void setAnnotation(String annotation) {
        mAnnotation = annotation;

        // Extract the simple class name.
        final int lastDollar = annotation.lastIndexOf('$');
        final int lastSep = lastDollar >= 0 ? lastDollar : annotation.lastIndexOf('.');
        mAnnotationSimpleName = annotation.substring(lastSep);
    }


    /**
     * Sets annotation tag.
     *
     * @param messageKey key to use for failed check message
     */
    @SuppressWarnings("unused")
    public void setMessageKey(String messageKey) {
        mMessageKey = messageKey;
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {
                TokenTypes.INTERFACE_DEF,
                TokenTypes.CLASS_DEF,
                TokenTypes.ANNOTATION_DEF,
                TokenTypes.ENUM_DEF,
                TokenTypes.METHOD_DEF,
                TokenTypes.CTOR_DEF,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.ENUM_CONSTANT_DEF,
                TokenTypes.ANNOTATION_FIELD_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return getAcceptableTokens();
    }

    @Override
    public void visitToken(final DetailAST ast) {
        final TextBlock javadoc = getFileContents().getJavadocBefore(ast.getLineNo());

        final boolean containsAnnotation =
                AnnotationUtility.containsAnnotation(ast, mAnnotationSimpleName)
                        || AnnotationUtility.containsAnnotation(ast, mAnnotation);

        final boolean containsJavadocTag = containsJavadocTag(javadoc);

        if (containsAnnotation ^ containsJavadocTag && !(mSkipNoJavadoc && javadoc == null)) {
            log(ast.getLineNo(), mMessageKey);
        }
    }

    /**
     * Checks to see if the text block contains the tag.
     *
     * @param javadoc the javadoc of the AST
     * @return true if contains the tag
     */
    private boolean containsJavadocTag(final TextBlock javadoc) {
        if (javadoc == null) {
            return false;
        }

        final String[] lines = javadoc.getText();
        int currentLine = javadoc.getStartLineNo();

        boolean found = false;

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final Matcher javadocNoArgMatcher = mMatchTag.matcher(line);
            if (javadocNoArgMatcher.find()) {
                if (found) {
                    log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG, mTag);
                }
                found = true;
            } else {
                final Matcher noArgMultilineStart = mMatchTagMultilineStart.matcher(line);
                if (noArgMultilineStart.find()) {
                    found = checkTagAtTheRestOfComment(lines, found, currentLine, i);
                }
            }
            currentLine++;
        }

        return found;
    }

    /**
     * Looks for the rest of the comment if all we saw was the tag and the
     * name. Stops when we see '*' (end of Javadoc), '{@literal @}' (start of
     * next tag), or anything that's not whitespace or '*' characters.
     *
     * @param lines all lines
     * @param foundBefore flag from parent method
     * @param currentLine current line
     * @param index som index
     * @return true if Tag is found
     */
    private boolean checkTagAtTheRestOfComment(String[] lines, boolean foundBefore, int currentLine,
            int index) {
        boolean found = false;

        for (int reindex = index + 1; reindex < lines.length;) {
            final Matcher multilineCont = MATCH_HIDE_MULTILINE_CONT.matcher(lines[reindex]);
            if (multilineCont.find()) {
                reindex = lines.length;

                final String lFin = multilineCont.group(1);
                if (lFin.equals(NEXT_TAG) || lFin.equals(END_JAVADOC)) {
                    log(currentLine, MSG_KEY_JAVADOC_MISSING);
                    if (foundBefore) {
                        log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG, mTag);
                    }
                    found = true;
                } else {
                    if (foundBefore) {
                        log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG, mTag);
                    }
                    found = true;
                }
            }
            reindex++;
        }

        return found;
    }
}
