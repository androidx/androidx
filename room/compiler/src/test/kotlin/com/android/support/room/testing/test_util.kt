/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.android.support.room.Entity
import com.android.support.room.Query
import com.android.support.room.ext.LifecyclesTypeNames
import com.android.support.room.processor.EntityProcessor
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.verifier.DatabaseVerifier
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.squareup.javapoet.ClassName
import org.mockito.Mockito
import java.io.File
import javax.lang.model.element.Element
import javax.tools.JavaFileObject

object COMMON {
    val USER by lazy {
        loadJavaCode("common/input/User.java", "foo.bar.User")
    }
    val USER_TYPE_NAME by lazy {
        ClassName.get("foo.bar", "User")
    }
    val BOOK by lazy {
        loadJavaCode("common/input/Book.java", "foo.bar.Book")
    }
    val NOT_AN_ENTITY by lazy {
        loadJavaCode("common/input/NotAnEntity.java", "foo.bar.NotAnEntity")
    }
    val MULTI_PKEY_ENTITY by lazy {
        loadJavaCode("common/input/MultiPKeyEntity.java", "MultiPKeyEntity")
    }
    val LIVE_DATA by lazy {
        loadJavaCode("common/input/LiveData.java", LifecyclesTypeNames.LIVE_DATA.toString())
    }
    val COMPUTABLE_LIVE_DATA by lazy {
        loadJavaCode("common/input/ComputableLiveData.java",
                LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.toString())
    }
}
fun simpleRun(f: (TestInvocation) -> Unit): CompileTester {
    return Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
            .that(JavaFileObjects.forSourceString("foo.bar.MyClass",
                    """
                    package foo.bar;
                    abstract public class MyClass {
                    @com.android.support.room.Query("foo")
                    abstract public void setFoo(String foo);
                    }
                    """))
            .processedWith(TestProcessor.builder()
                    .nextRunHandler {
                        f(it)
                        true
                    }
                    .forAnnotations(Query::class)
                    .build())
}

fun loadJavaCode(fileName : String, qName : String) : JavaFileObject {
    val contents = File("src/test/data/$fileName").readText(Charsets.UTF_8)
    return JavaFileObjects.forSourceString(qName, contents)
}

fun createVerifierFromEntities(invocation: TestInvocation) : DatabaseVerifier {
    val entities = invocation.roundEnv.getElementsAnnotatedWith(Entity::class.java).map {
        EntityProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    return DatabaseVerifier.create(invocation.context, Mockito.mock(Element::class.java),
            entities)!!
}
