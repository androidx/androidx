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

package com.android.tools.build.jetifier.processor.transform.proguard

import org.junit.Test

class ClassFilterTest {

    @Test fun proGuard_classFilter() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-adaptclassstrings support.Activity, support.Fragment, keep.Me"
            )
            .rewritesTo(
                "-adaptclassstrings test.Activity, test.Fragment, keep.Me"
            )
    }

    @Test fun proGuard_classFilter_newLineIgnored() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-adaptclassstrings support.Activity, support.Fragment, keep.Me \n" +
                " support.Activity"
            )
            .rewritesTo(
                "-adaptclassstrings test.Activity, test.Fragment, keep.Me \n" +
                " support.Activity"
            )
    }

    @Test fun proGuard_classFilter_spacesRespected() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "  -adaptclassstrings  support.Activity ,  support.Fragment,keep.Me  "
            )
            .rewritesTo(
                "  -adaptclassstrings  test.Activity, test.Fragment, keep.Me"
            )
    }

    @Test fun proGuard_classFilter_negation() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "  -adaptclassstrings !support.Activity, !support.Fragment, !keep.Me  "
            )
            .rewritesTo(
                "  -adaptclassstrings !test.Activity, !test.Fragment, !keep.Me"
            )
    }

    @Test fun proGuard_classFilter_multiple_shouldMerge() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet(
                "support/Activity*" to setOf("test/Activity1*", "test/Activity2*"),
                "support/Annotation*" to setOf("test/Annotation1*", "test/Annotation2*")
            )
            .testThatGivenProGuard(
                "-adaptclassstrings support.Activity*, support.Annotation*, keep.Me"
            )
            .rewritesTo(
                "-adaptclassstrings test.Activity1*, test.Activity2*, test.Annotation1*, " +
                "test.Annotation2*, keep.Me"
            )
    }
}