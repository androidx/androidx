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
 */
@SuppressWarnings("unused")
public final class MismatchedAnnotationCheck extends AbstractCheck {

    /** Key for the warning message text by check properties. */
    private static final String MSG_KEY_JAVADOC_DUPLICATE_TAG = "javadoc.duplicateTag";

    /** Key for the warning message text by check properties. */
    private static final String MSG_KEY_JAVADOC_MISSING = "javadoc.missing";

    /** Javadoc tag. */
    private String mTag;

    /** Pattern for matching javadoc tag. */
    private Pattern mMatchTag;

    /** Simple annotation name. */
    private String mAnnotationSimpleName;

    /** Fully-qualified annotation name. */
    private String mAnnotation;

    /** Key for the warning message text specified by check properties. */
    private String mMessageKey;

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

        // Tag may either have a description or be on a line by itself.
        mMatchTag = CommonUtils.createPattern("@" + tag + "(?:\\s|$)");
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
        mAnnotationSimpleName = annotation.substring(lastSep + 1);
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
        final boolean containsAnnotation =
                AnnotationUtility.containsAnnotation(ast, mAnnotationSimpleName)
                        || AnnotationUtility.containsAnnotation(ast, mAnnotation);
        final boolean containsJavadocTag = containsJavadocTag(ast);
        if (containsAnnotation ^ containsJavadocTag) {
            log(ast.getLineNo(), mMessageKey);
        }
    }

    /**
     * Checks to see if the text block contains the tag.
     *
     * @param ast the AST being visited
     * @return true if contains the tag
     */
    private boolean containsJavadocTag(final DetailAST ast) {
        final TextBlock javadoc = getFileContents().getJavadocBefore(ast.getLineNo());
        if (javadoc == null) {
            return false;
        }

        int currentLine = javadoc.getStartLineNo();
        boolean found = false;

        final String[] lines = javadoc.getText();
        for (String line : lines) {
            if (mMatchTag.matcher(line).find()) {
                if (found) {
                    log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG, mTag);
                }
                found = true;
            }
            currentLine++;
        }

        return found;
    }
}
