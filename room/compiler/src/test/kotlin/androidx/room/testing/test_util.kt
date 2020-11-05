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
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XType
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RoomRxJava3TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.processor.DatabaseViewProcessor
import androidx.room.processor.TableEntityProcessor
import androidx.room.solver.CodeGenScope
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.verifier.DatabaseVerifier
import androidx.room.writer.ClassWriter
import com.google.common.io.Files
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ClassName
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider.getSystemJavaCompiler

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
        loadJavaCode(
            "common/input/ComputableLiveData.java",
            LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.toString()
        )
    }
    val PUBLISHER by lazy {
        loadJavaCode(
            "common/input/reactivestreams/Publisher.java",
            ReactiveStreamsTypeNames.PUBLISHER.toString()
        )
    }
    val RX2_FLOWABLE by lazy {
        loadJavaCode(
            "common/input/rxjava2/Flowable.java",
            RxJava2TypeNames.FLOWABLE.toString()
        )
    }
    val RX2_OBSERVABLE by lazy {
        loadJavaCode(
            "common/input/rxjava2/Observable.java",
            RxJava2TypeNames.OBSERVABLE.toString()
        )
    }
    val RX2_SINGLE by lazy {
        loadJavaCode(
            "common/input/rxjava2/Single.java",
            RxJava2TypeNames.SINGLE.toString()
        )
    }
    val RX2_MAYBE by lazy {
        loadJavaCode(
            "common/input/rxjava2/Maybe.java",
            RxJava2TypeNames.MAYBE.toString()
        )
    }
    val RX2_COMPLETABLE by lazy {
        loadJavaCode(
            "common/input/rxjava2/Completable.java",
            RxJava2TypeNames.COMPLETABLE.toString()
        )
    }

    val RX2_ROOM by lazy {
        loadJavaCode("common/input/Rx2Room.java", RoomRxJava2TypeNames.RX_ROOM.toString())
    }

    val RX3_FLOWABLE by lazy {
        loadJavaCode(
            "common/input/rxjava3/Flowable.java",
            RxJava3TypeNames.FLOWABLE.toString()
        )
    }
    val RX3_OBSERVABLE by lazy {
        loadJavaCode(
            "common/input/rxjava3/Observable.java",
            RxJava3TypeNames.OBSERVABLE.toString()
        )
    }
    val RX3_SINGLE by lazy {
        loadJavaCode(
            "common/input/rxjava3/Single.java",
            RxJava3TypeNames.SINGLE.toString()
        )
    }
    val RX3_MAYBE by lazy {
        loadJavaCode(
            "common/input/rxjava3/Maybe.java",
            RxJava3TypeNames.MAYBE.toString()
        )
    }
    val RX3_COMPLETABLE by lazy {
        loadJavaCode(
            "common/input/rxjava3/Completable.java",
            RxJava3TypeNames.COMPLETABLE.toString()
        )
    }

    val RX3_ROOM by lazy {
        loadJavaCode("common/input/Rx3Room.java", RoomRxJava3TypeNames.RX_ROOM.toString())
    }

    val DATA_SOURCE_FACTORY by lazy {
        loadJavaCode("common/input/DataSource.java", "androidx.paging.DataSource")
    }

    val POSITIONAL_DATA_SOURCE by lazy {
        loadJavaCode(
            "common/input/PositionalDataSource.java",
            PagingTypeNames.POSITIONAL_DATA_SOURCE.toString()
        )
    }

    val LISTENABLE_FUTURE by lazy {
        loadJavaCode(
            "common/input/guava/ListenableFuture.java",
            GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.toString()
        )
    }

    val GUAVA_ROOM by lazy {
        loadJavaCode(
            "common/input/GuavaRoom.java",
            RoomGuavaTypeNames.GUAVA_ROOM.toString()
        )
    }

    val CHANNEL by lazy {
        loadJavaCode(
            "common/input/coroutines/Channel.java",
            KotlinTypeNames.CHANNEL.toString()
        )
    }

    val SEND_CHANNEL by lazy {
        loadJavaCode(
            "common/input/coroutines/SendChannel.java",
            KotlinTypeNames.SEND_CHANNEL.toString()
        )
    }

    val RECEIVE_CHANNEL by lazy {
        loadJavaCode(
            "common/input/coroutines/ReceiveChannel.java",
            KotlinTypeNames.RECEIVE_CHANNEL.toString()
        )
    }
}
fun testCodeGenScope(): CodeGenScope {
    return CodeGenScope(mock(ClassWriter::class.java))
}

fun simpleRun(
    vararg jfos: JavaFileObject,
    classpathFiles: Set<File> = emptySet(),
    options: List<String> = emptyList(),
    f: (TestInvocation) -> Unit
): CompileTester {
    return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(jfos.toList() + JavaFileObjects.forSourceLines("NoOp", "final class NoOp {}"))
        .apply {
            if (classpathFiles.isNotEmpty()) {
                withClasspath(classpathFiles)
            }
        }
        .withCompilerOptions(options)
        .processedWith(
            TestProcessor.builder()
                .nextRunHandler {
                    f(it)
                    true
                }
                .forAnnotations("*")
                .build()
        )
}

fun loadJavaCode(fileName: String, qName: String): JavaFileObject {
    val contents = File("src/test/data/$fileName").readText(Charsets.UTF_8)
    return JavaFileObjects.forSourceString(qName, contents)
}

fun createVerifierFromEntitiesAndViews(invocation: TestInvocation): DatabaseVerifier {
    return DatabaseVerifier.create(
        invocation.context, mock(XElement::class.java),
        invocation.getEntities(), invocation.getViews()
    )!!
}

fun TestInvocation.getViews(): List<androidx.room.vo.DatabaseView> {
    return roundEnv.getElementsAnnotatedWith(DatabaseView::class.java).map {
        DatabaseViewProcessor(context, it.asTypeElement()).process()
    }
}

fun TestInvocation.getEntities(): List<androidx.room.vo.Entity> {
    val entities = roundEnv.getElementsAnnotatedWith(Entity::class.java).map {
        TableEntityProcessor(context, it.asTypeElement()).process()
    }
    return entities
}

/**
 * Create mocks of [XElement] and [XType] so that they can be used for instantiating a fake
 * [androidx.room.vo.Field].
 */
fun mockElementAndType(): Pair<XFieldElement, XType> {
    val element = mock(XFieldElement::class.java)
    val type = mock(XType::class.java)
    doReturn(type).`when`(element).type
    return element to type
}

fun compileLibrarySource(sourceName: String, code: String): Set<File> {
    val sourceCode = """
        package test.library;
        import androidx.room.*;
        $code
        """
    return compileLibrarySources(JavaFileObjects.forSourceString(sourceName, sourceCode))
}

/**
 * Compiles an array of sources and returns a set of files to be used as classpath pointing
 * to the .class files generated by the compilation. This method is useful for creating an
 * environment where the annotation processor has to process compiled classes from a library.
 */
fun compileLibrarySources(vararg sources: JavaFileObject): Set<File> {
    val lib = Files.createTempDir()
    val compiler = getSystemJavaCompiler()
    val fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), UTF_8)
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(lib))
    val task = compiler.getTask(null, fileManager, null, emptyList(), null, listOf(*sources))
    assertThat(task.call()).isTrue()
    return getSystemClasspathFiles() + lib
}

private fun getSystemClasspathFiles(): Set<File> {
    val pathSeparator = System.getProperty("path.separator")!!
    return System.getProperty("java.class.path")!!.split(pathSeparator).map { File(it) }.toSet()
}

fun String.toJFO(qName: String): JavaFileObject = JavaFileObjects.forSourceLines(qName, this)