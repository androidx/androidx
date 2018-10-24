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

package androidx.customerrorprone;

import static com.google.errorprone.BugPattern.Category.ANDROID;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.ElementKind;


/**
 * Disallows direct comparison of SDK_INT to anything other than constants
 * in android.os.Build.VERSION_CODES
 */
// @AutoService(BugChecker.class) TODO: decide whether we want to keep this rule and uncomment
// the annotation or get rid of the class completely.
@BugPattern(
        name = "BanUnlistedSDKVersionComparison",
        summary = "Comparison of SDK_INT to non listed version constants is not allowed, please"
                + " compare to versions listed in VERSION_CODES instead",
        category = ANDROID,
        severity = ERROR
)
public class BanUnlistedSDKVersionComparison extends BugChecker implements BinaryTreeMatcher {

    private static final String VERSION_CLASSNAME = "android.os.Build.VERSION";
    /**
     * {@code android.os.Build.VERSION.SDK_INT} checks above this version will be flagged.
     */
    private static final int MAXIMUM_ALLOWED_RAW_SDK_VERSION = 28;

    @Override
    public Description matchBinary(BinaryTree tree, VisitorState state) {
        ExpressionTree lhs = tree.getLeftOperand();
        ExpressionTree rhs = tree.getRightOperand();

        if (isSdkInt(lhs)) {
            return matchAssumingSdkIntIsLhs(tree, tree.getKind(), rhs);
        } else if (isSdkInt(rhs)) {
            return matchAssumingSdkIntIsLhs(tree, reverseBinop(tree.getKind()), lhs);
        } else {
            return Description.NO_MATCH;
        }
    }

    private Description matchAssumingSdkIntIsLhs(BinaryTree tree, Kind op, ExpressionTree rhs) {
        if (op == Kind.PLUS) {
            // Most likely being attached to a string, this is not a comparison, allow.
            return Description.NO_MATCH;
        } else if (!isVersionCode(rhs) && isAtLeast(rhs, minUnsafeRhs(op))) {
            return describeMatch(
                    tree);
        }
        return Description.NO_MATCH;
    }

    private static boolean isAtLeast(ExpressionTree tree, long x) {
        Object value = ASTHelpers.constValue(tree);
        if (!(value instanceof Number)) {
            // Not a constant, most likely a variable
            // this should default to true to disallow its use.
            return true;
        }
        long actual = ((Number) value).longValue();
        return x <= actual;
    }

    private static boolean isVersionCode(ExpressionTree tree) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        return sym instanceof Symbol.VarSymbol && ((Symbol.VarSymbol) sym).owner.enclClass()
                .getQualifiedName().contentEquals("android.os.Build.VERSION_CODES");
    }

    /**
     * Returns the operator op' such that (X op SDK_INT iff SDK_INT op' X) for the given op.
     * In other words, this method allows pretending that all SDK_INT comparisons have SDK_INT
     * as the lhs and a constant as the rhs.
     */
    private static Kind reverseBinop(Kind op) {
        switch (op) {
            case LESS_THAN:
                return Kind.GREATER_THAN;
            case LESS_THAN_EQUAL:
                return Kind.GREATER_THAN_EQUAL;
            case GREATER_THAN:
                return Kind.LESS_THAN;
            case GREATER_THAN_EQUAL:
                return Kind.LESS_THAN_EQUAL;
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            default: // weird things like & | ^
                return op;
        }
    }

    /**
     * Returns the minimum X for which we want to flag a comparison SDK_INT op X for the given op.
     */
    private static int minUnsafeRhs(Kind op) {
        switch (op) {
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
                return MAXIMUM_ALLOWED_RAW_SDK_VERSION;
            case LESS_THAN:
            case GREATER_THAN_EQUAL:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            default: // weird things like & | ^
                return MAXIMUM_ALLOWED_RAW_SDK_VERSION + 1;
        }
    }

    private static boolean isSdkInt(ExpressionTree tree) {
        Symbol symbol = ASTHelpers.getSymbol(tree);
        // Match symbol's owner to android.os.Build.VERSION separately because can't get fully
        // qualified "android.os.Build.VERSION.SDK_INT" out of symbol, just "SDK_INT"
        return symbol != null
                && symbol.owner != null
                && symbol.getKind() == ElementKind.FIELD
                && symbol.isStatic()
                && VERSION_CLASSNAME.contentEquals(symbol.owner.getQualifiedName())
                && "SDK_INT".contentEquals(symbol.name);
    }
}
