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

package com.android.support.room.vo

import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element

data class Field(val element : Element, val name : String, val type: TypeName,
                 val primaryKey : Boolean) {
    lateinit var getter : FieldGetter
    lateinit var setter : FieldSetter
    /**
     * List of names that include variations.
     * e.g. if it is mUser, user is added to the list
     * or if it is isAdmin, admin is added to the list
     */
    val nameWithVariations by lazy {
        val result = arrayListOf(name)
        if (name.length > 1) {
            if (name.startsWith('_')) {
                result.add(name.substring(1))
            }
            if (name.startsWith("m") && name[1].isUpperCase()) {
                result.add(name.substring(1).decapitalize())
            }

            if (type == TypeName.BOOLEAN || type == TypeName.BOOLEAN.box()) {
                if (name.length > 2 && name.startsWith("is") && name[2].isUpperCase()) {
                    result.add(name.substring(2).decapitalize())
                }
                if (name.length > 3 && name.startsWith("has") && name[3].isUpperCase()) {
                    result.add(name.substring(3).decapitalize())
                }
            }
        }
        result
    }

    val getterNameWithVariations by lazy {
        nameWithVariations.map { "get${it.capitalize()}" }
    }

    val setterNameWithVariations by lazy {
        nameWithVariations.map { "set${it.capitalize()}" }
    }
}