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

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isDirectImplementationOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;

import javax.lang.model.element.Modifier;

/**
 * Bug Pattern that detects implementation of Parcelable and causes an error.
 */
@AutoService(BugChecker.class)
@BugPattern(
        name = "BanParcelableUsage",
        summary = "Use of Parcelable is no longer recommended,"
                + " please use VersionedParcelable instead.",
        category = Category.ANDROID,
        severity = SeverityLevel.ERROR)
public class BanParcelableUsage extends BugChecker implements ClassTreeMatcher {

    /**
     * Matches if a non-public non-abstract class/interface is subtype of android.os.Parcelable
     */
    private static final Matcher<ClassTree> IMPLEMENTS_PARCELABLE =
            allOf(isDirectImplementationOf("android.os.Parcelable"),
                    not(hasModifier(Modifier.ABSTRACT)));

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (!IMPLEMENTS_PARCELABLE.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        return describeMatch(tree);
    }
}
