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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import androidx.annotation.Keep;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Matches on existence of androidx.annotation.Keep annotation
 */
@AutoService(BugChecker.class)
@BugPattern(
        name = "BanKeepAnnotation",
        category = ANDROID,
        summary = "Use of Keep annotation is not allowed, , please use a conditional keep rule in"
                + " proguard-rules.pro.",
        severity = ERROR)
public class BanKeepAnnotation extends BugChecker implements MethodTreeMatcher {

    private static final Matcher<Tree> HAS_KEEP_ANNOTATION =
            hasAnnotation(Keep.class.getCanonicalName());

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
        if (!HAS_KEEP_ANNOTATION.matches(tree, state)) {
            return NO_MATCH;
        }
        return describeMatch(tree);
    }
}
