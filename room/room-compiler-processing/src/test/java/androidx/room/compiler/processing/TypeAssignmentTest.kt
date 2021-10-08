/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeAssignmentTest {
    companion object {
        private val TEST_OBJECT = Source.java(
            "foo.bar.MyObject",
            """
            package foo.bar;
            import java.util.Set;
            import java.util.HashSet;
            import java.util.Map;
            class MyObject {
                String mString;
                Integer mInteger;
                Set<MyObject> mSet;
                Set<? extends MyObject> mVarianceSet;
                HashSet<MyObject> mHashSet;
                Map<String, ?> mUnboundedMap;
                Map<String, String> mStringMap;
            }
            """.trimIndent()
        )
    }

    @Test
    fun basic() {
        runTest {
            val testObject = processingEnv.requireTypeElement("foo.bar.MyObject")
            val string = testObject.getField("mString")
            val integer = testObject.getField("mInteger")
            assertThat(
                integer.type.isAssignableFromWithoutVariance(string.type)
            ).isFalse()
        }
    }

    @Test
    fun generics() {
        runTest {
            val testObject = processingEnv.requireTypeElement("foo.bar.MyObject")
            val set = testObject.getField("mSet").type
            val hashSet = testObject.getField("mHashSet").type
            assertThat(hashSet.isAssignableFromWithoutVariance(set)).isFalse()
            assertThat(set.isAssignableFromWithoutVariance(hashSet)).isTrue()
        }
    }

    @Test
    fun variance() {
        /**
         *  Set<User> userSet = null;
         *  Set<? extends User> userSet2 = null;
         *  userSet = userSet2;  // NOT OK for java but kotlin data classes hit this so we want
         *                       // to accept it
         */
        runTest {
            val testObject = processingEnv.requireTypeElement("foo.bar.MyObject")
            val set = testObject.getField("mSet").type
            val varianceSet = testObject.getField("mVarianceSet").type
            assertThat(varianceSet.isAssignableFromWithoutVariance(set)).isTrue()
            assertThat(set.isAssignableFromWithoutVariance(varianceSet)).isTrue()
            assertThat(
                varianceSet.extendsBound()
            ).isNull()
        }
    }

    @Test
    fun unboundedVariance() {
        runTest {
            val testObject = processingEnv.requireTypeElement("foo.bar.MyObject")
            val unbounded = testObject.getField("mUnboundedMap").type
            val objectMap = testObject.getField("mStringMap").type
            assertThat(objectMap.isAssignableFromWithoutVariance(unbounded)).isFalse()
            assertThat(unbounded.isAssignableFromWithoutVariance(objectMap)).isTrue()
        }
    }

    private fun runTest(handler: XTestInvocation.() -> Unit) {
        runProcessorTest(
            sources = listOf(TEST_OBJECT),
            handler = handler
        )
    }
}
