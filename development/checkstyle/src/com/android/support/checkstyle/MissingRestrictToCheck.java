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

import android.support.annotation.RestrictTo;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTagInfo;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtility;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to verify that both the {@link RestrictTo RestrictTo}
 * annotation and the hide javadoc tag are present when either one is present.
 * <p>
 * Both ways of flagging hidden APIs serve their own purpose. The
 * {@link RestrictTo RestrictTo} annotation is used for compilers and
 * development tools. The hide javadoc tag is used to document why something is
 * hidden and to prevent it from showing up in javadocs.
 * <p>
 * In order to properly mark something as hidden, both annotations should be
 * present.
 * <p>
 * To configure this check do the following:
 * <pre>&lt;module name="JavadocRestrictTo"/&gt;</pre>
 * <p>
 * In addition you can configure this check with skipNoJavadoc option to allow
 * it to ignore cases when JavaDoc is missing, but still warns when JavaDoc is
 * present but either hide is missing from JavaDoc or
 * {@link RestrictTo RestrictTo} is missing from the element. To configure this
 * check to allow it use:
 * <pre>   &lt;property name="skipNoJavadoc" value="true" /&gt;</pre>
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
public final class MissingRestrictToCheck extends Check {

    public static final String MSG_KEY_ANNOTATION_MISSING_HIDE = "annotation.missing.hide";
    public static final String MSG_KEY_JAVADOC_DUPLICATE_TAG = "javadoc.duplicateTag";
    public static final String MSG_KEY_JAVADOC_MISSING = "javadoc.missing";

    private static final String RESTRICT_TO = "RestrictTo";
    private static final String FQ_RESTRICT_TO = "android.support.annotation.RestrictTo";

    private static final Pattern MATCH_HIDE = CommonUtils.createPattern("@(hide)\\s+\\S");
    private static final Pattern MATCH_HIDE_MULTILINE_START =
            CommonUtils.createPattern("@(hide)\\s*$");
    private static final Pattern MATCH_HIDE_MULTILINE_CONT =
            CommonUtils.createPattern("(\\*/|@|[^\\s\\*])");

    private static final String END_JAVADOC = "*/";
    private static final String NEXT_TAG = "@";

    @Override
    public int[] getDefaultTokens() {
        return this.getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{15, 14, 157, 154, 9, 8, 10, 155, 161};
    }

    @Override
    public int[] getRequiredTokens() {
        return this.getAcceptableTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        TextBlock javadoc = this.getFileContents().getJavadocBefore(ast.getLineNo());
        boolean containsAnnotation = AnnotationUtility.containsAnnotation(ast, RESTRICT_TO)
                || AnnotationUtility.containsAnnotation(ast, FQ_RESTRICT_TO);
        boolean containsJavadocTag = this.containsJavadocTag(javadoc);
        if (containsAnnotation ^ containsJavadocTag) {
            this.log(ast.getLineNo(), MSG_KEY_ANNOTATION_MISSING_HIDE, new Object[0]);
        }

    }

    private boolean containsJavadocTag(TextBlock javadoc) {
        if (javadoc == null) {
            return false;
        } else {
            String[] lines = javadoc.getText();
            boolean found = false;
            int currentLine = javadoc.getStartLineNo() - 1;

            for (int i = 0; i < lines.length; ++i) {
                ++currentLine;
                String line = lines[i];
                Matcher javadocNoArgMatcher = MATCH_HIDE.matcher(line);
                Matcher noArgMultilineStart = MATCH_HIDE_MULTILINE_START.matcher(line);
                if (javadocNoArgMatcher.find()) {
                    if (found) {
                        this.log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG,
                                new Object[]{JavadocTagInfo.DEPRECATED.getText()});
                    }

                    found = true;
                } else if (noArgMultilineStart.find()) {
                    found = this.checkTagAtTheRestOfComment(lines, found, currentLine, i);
                }
            }

            return found;
        }
    }

    private boolean checkTagAtTheRestOfComment(String[] lines, boolean foundBefore,
            int currentLine, int index) {
        boolean found = false;

        for (int reindex = index + 1; reindex < lines.length; ++reindex) {
            Matcher multilineCont = MATCH_HIDE_MULTILINE_CONT.matcher(lines[reindex]);
            if (multilineCont.find()) {
                reindex = lines.length;
                String lFin = multilineCont.group(1);
                if (!lFin.equals(NEXT_TAG) && !lFin.equals(END_JAVADOC)) {
                    if (foundBefore) {
                        this.log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG,
                                new Object[]{JavadocTagInfo.DEPRECATED.getText()});
                    }

                    found = true;
                } else {
                    this.log(currentLine, MSG_KEY_JAVADOC_MISSING, new Object[0]);
                    if (foundBefore) {
                        this.log(currentLine, MSG_KEY_JAVADOC_DUPLICATE_TAG,
                                new Object[]{JavadocTagInfo.DEPRECATED.getText()});
                    }

                    found = true;
                }
            }
        }

        return found;
    }
}
