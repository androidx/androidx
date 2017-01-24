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

package com.android.support.room

import com.android.support.room.processor.Context
import com.android.support.room.processor.DatabaseProcessor
import com.android.support.room.processor.EntityProcessor
import com.android.support.room.vo.DaoMethod
import com.android.support.room.writer.DaoWriter
import com.android.support.room.writer.DatabaseWriter
import com.android.support.room.writer.EntityCursorConverterWriter
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element

/**
 * The annotation processor for Room.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
class RoomProcessor : BasicAnnotationProcessor() {
    override fun initSteps(): MutableIterable<ProcessingStep>? {
        val context = Context(processingEnv)
        return arrayListOf(EntityProcessingStep(context),
                DatabaseProcessingStep(context))
    }

    class DatabaseProcessingStep(context: Context) : ContextBoundProcessingStep(context) {
        override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>)
                : MutableSet<Element> {
            // TODO multi step support
            val databases = elementsByAnnotation[Database::class.java]
                    ?.map {
                        DatabaseProcessor(context).parse(MoreElements.asType(it))
                    }
            val allDaoMethods = databases?.flatMap { it.daoMethods }
            allDaoMethods?.let {
                prepareDaosForWriting(databases!!, it)
                it.forEach {
                    DaoWriter(it.dao).write(context.processingEnv)
                }
            }

            databases?.forEach {
                DatabaseWriter(it).write(context.processingEnv)
            }
            return mutableSetOf()
        }

        override fun annotations(): MutableSet<out Class<out Annotation>> {
            return mutableSetOf(Database::class.java, Dao::class.java)
        }

        /**
         * Traverses all dao methods and assigns them suffix if they are used in multiple databases.
         */
        private fun prepareDaosForWriting(databases: List<com.android.support.room.vo.Database>,
                                          daoMethods: List<DaoMethod>) {
            daoMethods.groupBy { it.dao.typeName }
                    // if used only in 1 database, nothing to do.
                    .filter { entry -> entry.value.size > 1 }
                    .forEach { entry ->
                        entry.value.groupBy { daoMethod ->
                            // first suffix guess: Database's simple name
                            val db = databases.first { db -> db.daoMethods.contains(daoMethod) }
                            db.typeName.simpleName()
                        }.forEach { entry ->
                            val dbName = entry.key
                            val methods = entry.value
                            if (methods.size == 1) {
                                //good, db names do not clash, use db name as suffix
                                methods.first().dao.setSuffix(dbName)
                            } else {
                                // ok looks like a dao is used in 2 different databases both of
                                // which have the same name. enumerate.
                                methods.forEachIndexed { index, method ->
                                    method.dao.setSuffix("${dbName}_$index")
                                }
                            }
                        }
                    }
        }
    }

    class EntityProcessingStep(context: Context) : ContextBoundProcessingStep(context) {
        override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>)
                : MutableSet<Element> {
            elementsByAnnotation[Entity::class.java]
                    ?.map {
                        EntityProcessor(context).parse(MoreElements.asType(it))
                    }
                    ?.forEach {
                        EntityCursorConverterWriter(it).write(context.processingEnv)
                    }
            return mutableSetOf()
        }

        override fun annotations(): MutableSet<out Class<out Annotation>> {
            return mutableSetOf(Entity::class.java)
        }
    }

    abstract class ContextBoundProcessingStep(val context: Context) : ProcessingStep
}
