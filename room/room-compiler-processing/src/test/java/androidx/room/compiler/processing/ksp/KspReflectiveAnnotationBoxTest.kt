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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Test
import kotlin.reflect.KClass

class KspReflectiveAnnotationBoxTest {
    enum class TestEnum {
        VAL1,
        VAL2
    }

    annotation class TestAnnotation(
        val strProp: String = "abc",
        val intProp: Int = 3,
        val enumProp: TestEnum = TestEnum.VAL2,
        val enumArrayProp: Array<TestEnum> = [TestEnum.VAL1, TestEnum.VAL2, TestEnum.VAL1],
        val annProp: TestAnnotation2 = TestAnnotation2(3),
        val annArrayProp: Array<TestAnnotation2> = [TestAnnotation2(1), TestAnnotation2(5)],
        val typeProp: KClass<*> = Int::class,
        val typeArrayProp: Array<KClass<*>> = [Int::class, String::class]
    )

    annotation class TestAnnotation2(
        val intProp: Int = 0
    )

    @Test
    @TestAnnotation // putting annotation here to read it back easily :)
    fun simple() {
        runKspTest(sources = emptyList()) { invocation ->
            val box = KspReflectiveAnnotationBox(
                env = invocation.processingEnv as KspProcessingEnv,
                annotationClass = TestAnnotation::class.java,
                annotation = getAnnotationOnMethod("simple")
            )
            assertThat(box.value.strProp).isEqualTo("abc")
            assertThat(box.value.intProp).isEqualTo(3)
            assertThat(box.value.enumProp).isEqualTo(TestEnum.VAL2)
            assertThat(box.value.enumArrayProp).isEqualTo(
                arrayOf(TestEnum.VAL1, TestEnum.VAL2, TestEnum.VAL1)
            )
            box.getAsAnnotationBox<TestAnnotation2>("annProp").let {
                assertThat(it.value.intProp).isEqualTo(3)
            }
            box.getAsAnnotationBoxArray<TestAnnotation2>("annArrayProp").let {
                assertThat(
                    it.map { it.value.intProp }
                ).containsExactly(1, 5)
            }
            box.getAsType("typeProp")?.let {
                assertThat(it is KspType).isTrue()
                assertThat(it.typeName).isEqualTo(TypeName.INT)
            }
            box.getAsTypeList("typeArrayProp").let {
                assertThat(it.all { it is KspType }).isTrue()
                assertThat(it.map { it.typeName }).containsExactly(
                    TypeName.INT, ClassName.get(String::class.java)
                )
            }
        }
    }

    private inline fun <reified T : Annotation> getAnnotationOnMethod(methodName: String): T {
        return KspReflectiveAnnotationBoxTest::class.java.getMethod(methodName).annotations
            .first {
                it is TestAnnotation
            } as T
    }
}