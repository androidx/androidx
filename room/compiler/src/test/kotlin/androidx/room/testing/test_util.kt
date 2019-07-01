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

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.processor.TableEntityProcessor
import androidx.room.processor.DatabaseViewProcessor
import androidx.room.processor.QueryInterpreter
import androidx.room.solver.CodeGenScope
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.verifier.DatabaseVerifier
import androidx.room.writer.ClassWriter
import com.google.auto.common.MoreElements
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ClassName
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.nio.file.Paths
import javax.lang.model.element.Element
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

object COMMON {
    val USER by lazy {
        loadJavaCode("common/input/User.java", "foo.bar.User")
    }
    val USER_SUMMARY by lazy {
        loadJavaCode("common/input/UserSummary.java", "foo.bar.UserSummary")
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

    val PARENT by lazy {
        loadJavaCode("common/input/Parent.java", "foo.bar.Parent")
    }
    val CHILD1 by lazy {
        loadJavaCode("common/input/Child1.java", "foo.bar.Child1")
    }
    val CHILD2 by lazy {
        loadJavaCode("common/input/Child2.java", "foo.bar.Child2")
    }
    val INFO by lazy {
        loadJavaCode("common/input/Info.java", "foo.bar.Info")
    }

    val NOT_AN_ENTITY_TYPE_NAME by lazy {
        ClassName.get("foo.bar", "NotAnEntity")
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
    val PUBLISHER by lazy {
        loadJavaCode("common/input/reactivestreams/Publisher.java",
                ReactiveStreamsTypeNames.PUBLISHER.toString())
    }
    val FLOWABLE by lazy {
        loadJavaCode("common/input/rxjava2/Flowable.java",
                RxJava2TypeNames.FLOWABLE.toString())
    }
    val OBSERVABLE by lazy {
        loadJavaCode("common/input/rxjava2/Observable.java",
                RxJava2TypeNames.OBSERVABLE.toString())
    }
    val SINGLE by lazy {
        loadJavaCode("common/input/rxjava2/Single.java",
                RxJava2TypeNames.SINGLE.toString())
    }
    val MAYBE by lazy {
        loadJavaCode("common/input/rxjava2/Maybe.java",
                RxJava2TypeNames.MAYBE.toString())
    }
    val COMPLETABLE by lazy {
        loadJavaCode("common/input/rxjava2/Completable.java",
                RxJava2TypeNames.COMPLETABLE.toString())
    }

    val RX2_ROOM by lazy {
        loadJavaCode("common/input/Rx2Room.java", RoomRxJava2TypeNames.RX_ROOM.toString())
    }

    val DATA_SOURCE_FACTORY by lazy {
        loadJavaCode("common/input/DataSource.java", "androidx.paging.DataSource")
    }

    val POSITIONAL_DATA_SOURCE by lazy {
        loadJavaCode("common/input/PositionalDataSource.java",
                PagingTypeNames.POSITIONAL_DATA_SOURCE.toString())
    }

    val LISTENABLE_FUTURE by lazy {
        loadJavaCode("common/input/guava/ListenableFuture.java",
            GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.toString())
    }

    val GUAVA_ROOM by lazy {
        loadJavaCode("common/input/GuavaRoom.java",
            RoomGuavaTypeNames.GUAVA_ROOM.toString())
    }
}
fun testCodeGenScope(): CodeGenScope {
    return CodeGenScope(Mockito.mock(ClassWriter::class.java))
}

fun simpleRun(
    vararg jfos: JavaFileObject,
    classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    f: (TestInvocation) -> Unit
): CompileTester {
    return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(jfos.toList() + JavaFileObjects.forSourceLines("Dummy", "final class Dummy {}"))
            .withClasspathFrom(classLoader)
            .processedWith(TestProcessor.builder()
                    .nextRunHandler {
                        f(it)
                        true
                    }
                    .forAnnotations("*")
                    .build())
}

fun loadJavaCode(fileName: String, qName: String): JavaFileObject {
    val contents = File("src/test/data/$fileName").readText(Charsets.UTF_8)
    return JavaFileObjects.forSourceString(qName, contents)
}

fun createInterpreterFromEntitiesAndViews(invocation: TestInvocation): QueryInterpreter {
    val entities = invocation.roundEnv.getElementsAnnotatedWith(Entity::class.java).map {
        TableEntityProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    val views = invocation.roundEnv.getElementsAnnotatedWith(DatabaseView::class.java).map {
        DatabaseViewProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    return QueryInterpreter(entities + views)
}

fun createVerifierFromEntitiesAndViews(invocation: TestInvocation): DatabaseVerifier {
    val entities = invocation.roundEnv.getElementsAnnotatedWith(Entity::class.java).map {
        TableEntityProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    val views = invocation.roundEnv.getElementsAnnotatedWith(DatabaseView::class.java).map {
        DatabaseViewProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    return DatabaseVerifier.create(invocation.context, Mockito.mock(Element::class.java),
            entities, views)!!
}

/**
 * Create mocks of [Element] and [TypeMirror] so that they can be used for instantiating a fake
 * [androidx.room.vo.Field].
 */
fun mockElementAndType(): Pair<Element, TypeMirror> {
    val element = mock(Element::class.java)
    val type = mock(TypeMirror::class.java)
    doReturn(TypeKind.DECLARED).`when`(type).kind
    doReturn(type).`when`(element).asType()
    return element to type
}

fun compileLibrarySource(sourceName: String, code: String): ClassLoader {
    val sourceCode = """
        package test.library;
        import androidx.room.*;
        $code
        """
    return compileLibrarySources(JavaFileObjects.forSourceString(sourceName, sourceCode))
}

/**
 * Compiles an array of sources and returns a class loader that is able to use the .class files
 * generated by the compilation. This method is useful for creating an environment where the
 * annotation processor has to process compiled classes from a library.
 */
fun compileLibrarySources(vararg sources: JavaFileObject): ClassLoader {
    val tempDir = Files.createTempDir()
    javac().compile(*sources)
            .generatedFiles().forEach { classFile ->
                val tempFile = File(tempDir, classFile.toUri().path)
                Files.createParentDirs(tempFile)

                FileOutputStream(tempFile).use { output ->
                    classFile.openInputStream().use { input ->
                        ByteStreams.copy(input, output)
                    }
                }
            }
    val classesPath = Paths.get(tempDir.path, StandardLocation.CLASS_OUTPUT.name).toUri().toURL()
    return URLClassLoader(arrayOf(classesPath), ClassLoader.getSystemClassLoader())
}

fun String.toJFO(qName: String): JavaFileObject = JavaFileObjects.forSourceLines(qName, this)