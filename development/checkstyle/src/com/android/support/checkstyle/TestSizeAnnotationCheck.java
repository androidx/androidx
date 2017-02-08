/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtility;

/**
 * A check that verifies that all the methods marked with @Test have a matching test size
 * annotation such as @SmallTest, @MediumTest or @LargeTest. This is needed to make sure
 * that newly added tests get run in the automatted test runner.
 */
public class TestSizeAnnotationCheck extends AbstractCheck {
    private static final String SMALL_TEST = "SmallTest";
    private static final String MEDIUM_TEST = "MediumTest";
    private static final String LARGE_TEST = "LargeTest";
    private static final String TEST = "Test";

    private static final String MESSAGE = "Method with @Test annotation must have a @SmallTest, "
            + "@MediumTest, or @LargeTest annotation. See https://goo.gl/c2I0WP for recommended "
            + "timeouts";

    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF};
    }

    @Override
    public void visitToken(DetailAST ast) {
        final boolean classHasTestSizeAnnotation =
                AnnotationUtility.containsAnnotation(ast, SMALL_TEST)
                        || AnnotationUtility.containsAnnotation(ast, MEDIUM_TEST)
                        || AnnotationUtility.containsAnnotation(ast, LARGE_TEST);

        DetailAST objBlock = ast.findFirstToken(TokenTypes.OBJBLOCK);
        for (DetailAST child = objBlock.getFirstChild();
                child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.METHOD_DEF
                    && AnnotationUtility.containsAnnotation(child, TEST)) {
                final boolean methodHasTestSizeAnnotation =
                        AnnotationUtility.containsAnnotation(child, SMALL_TEST)
                                || AnnotationUtility.containsAnnotation(child, MEDIUM_TEST)
                                || AnnotationUtility.containsAnnotation(child, LARGE_TEST);
                if (!classHasTestSizeAnnotation && !methodHasTestSizeAnnotation) {
                    log(child.getLineNo(), MESSAGE);
                }
            }
        }
    }
}
