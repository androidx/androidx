/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.ext.getAllFieldsIncludingPrivateSupers
import androidx.room.ext.isAssignableWithoutVariance
import androidx.room.testing.TestInvocation
import com.google.testing.compile.JavaFileObjects
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import simpleRun
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class TypeAssignmentTest {
    companion object {
        private val TEST_OBJECT = JavaFileObjects.forSourceString("foo.bar.MyObject",
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
            """.trimIndent())
    }

    @Test
    fun basic() {
        runTest {
            val testObject = typeElement("foo.bar.MyObject")
            val string = testObject.getField(processingEnv, "mString")
            val integer = testObject.getField(processingEnv, "mInteger")
            assertThat(typeUtils.isAssignableWithoutVariance(string.asType(),
                    integer.asType()),
                    `is`(false))
        }
    }

    @Test
    fun generics() {
        runTest {
            val testObject = typeElement("foo.bar.MyObject")
            val set = testObject.getField(processingEnv, "mSet").asType()
            val hashSet = testObject.getField(processingEnv, "mHashSet").asType()
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = set,
                    to = hashSet
            ), `is`(false))
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = hashSet,
                    to = set
            ), `is`(true))
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
            val testObject = typeElement("foo.bar.MyObject")
            val set = testObject.getField(processingEnv, "mSet").asType()
            val varianceSet = testObject.getField(processingEnv, "mVarianceSet").asType()
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = set,
                    to = varianceSet
            ), `is`(true))
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = varianceSet,
                    to = set
            ), `is`(true))
        }
    }

    @Test
    fun unboundedVariance() {
        runTest {
            val testObject = typeElement("foo.bar.MyObject")
            val unbounded = testObject.getField(processingEnv, "mUnboundedMap").asType()
            val objectMap = testObject.getField(processingEnv, "mStringMap").asType()
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = unbounded,
                    to = objectMap
            ), `is`(false))
            assertThat(typeUtils.isAssignableWithoutVariance(
                    from = objectMap,
                    to = unbounded
            ), `is`(true))
        }
    }

    private fun TypeElement.getField(
            env: ProcessingEnvironment,
            name: String): VariableElement {
        return getAllFieldsIncludingPrivateSupers(env).first {
            it.simpleName.toString() == name
        }
    }

    private fun runTest(handler: TestInvocation.() -> Unit) {
        simpleRun(TEST_OBJECT) {
            it.apply { handler() }
        }.compilesWithoutError()
    }
}