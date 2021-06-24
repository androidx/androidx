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
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RoomRxJava3TypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.processor.DatabaseViewProcessor
import androidx.room.processor.TableEntityProcessor
import androidx.room.solver.CodeGenScope
import androidx.room.testing.context
import androidx.room.verifier.DatabaseVerifier
import androidx.room.writer.ClassWriter
import com.squareup.javapoet.ClassName
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import java.io.File

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

    val FLOW by lazy {
        loadJavaCode("common/input/Flow.java", KotlinTypeNames.FLOW.toString())
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

    val PAGING_SOURCE by lazy {
        loadJavaCode(
            "common/input/PagingSource.java",
            PagingTypeNames.PAGING_SOURCE.toString()
        )
    }

    val LIMIT_OFFSET_PAGING_SOURCE by lazy {
        loadJavaCode(
            "common/input/LimitOffsetPagingSource.java",
            RoomTypeNames.LIMIT_OFFSET_PAGING_SOURCE.toString()
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

fun loadJavaCode(fileName: String, qName: String): Source {
    val contents = File("src/test/data/$fileName").readText(Charsets.UTF_8)
    return Source.java(qName, contents)
}

fun loadTestSource(fileName: String, qName: String): Source {
    val contents = File("src/test/data/$fileName")
    return Source.load(contents, qName, fileName)
}

fun createVerifierFromEntitiesAndViews(invocation: XTestInvocation): DatabaseVerifier {
    return DatabaseVerifier.create(
        invocation.context, mock(XElement::class.java),
        invocation.getEntities(), invocation.getViews()
    )!!
}

fun XTestInvocation.getViews(): List<androidx.room.vo.DatabaseView> {
    return roundEnv.getElementsAnnotatedWith(DatabaseView::class.qualifiedName!!)
        .filterIsInstance<XTypeElement>()
        .map {
            DatabaseViewProcessor(context, it).process()
        }
}

fun XTestInvocation.getEntities(): List<androidx.room.vo.Entity> {
    val entities = roundEnv.getElementsAnnotatedWith(Entity::class.qualifiedName!!)
        .filterIsInstance<XTypeElement>()
        .map {
            TableEntityProcessor(context, it).process()
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
