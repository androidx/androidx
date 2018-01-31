/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.proguard

import android.support.tools.jetifier.core.transform.proguard.patterns.GroupsReplacer
import android.support.tools.jetifier.core.transform.proguard.patterns.PatternHelper

/**
 * Parses and rewrites ProGuard rules that contain class specification. See ProGuard documentation
 * https://www.guardsquare.com/en/proguard/manual/usage#classspecification
 */
class ProGuardClassSpecParser(private val mapper : ProGuardTypesMapper) {

    companion object {
        private const val RULES = "(keep[a-z]*|whyareyoukeeping|assumenosideeffects)"
        private const val RULES_MODIFIERS =
            "(includedescriptorclasses|allowshrinking|allowoptimization|allowobfuscation)"

        private const val CLASS_NAME = "[\\w.$?*_%]+"
        private const val CLASS_MODIFIERS = "[!]?(public|final|abstract)"
        private const val CLASS_TYPES = "[!]?(interface|class|enum)"

        private const val ANNOTATION_TYPE = CLASS_NAME

        private const val FIELD_NAME = "[\\w?*_%]+"
        private const val FIELD_TYPE = CLASS_NAME
        private const val FIELD_MODIFIERS =
            "[!]?(public|private|protected|static|volatile|transient)"

        private const val METHOD_MODIFIERS =
            "[!]?(public|private|protected|static|synchronized|native|abstract|strictfp)"
        private const val RETURN_TYPE_NAME = CLASS_NAME
        private const val METHOD_NAME = "[\\w?*_]+"
        private const val ARGS = "[^)]*"
    }

    val replacer = GroupsReplacer(
        pattern = PatternHelper.build(
            "-$RULES ($RULES_MODIFIERS )*(@｟$ANNOTATION_TYPE｠ )?($CLASS_MODIFIERS )*$CLASS_TYPES " +
            "｟$CLASS_NAME｠( (extends|implements) ｟$CLASS_NAME｠)?+ *( *\\{｟[^}]*｠\\} *)?+"),
        groupsMap = listOf(
            { annotation : String -> mapper.replaceType(annotation) },
            { className : String -> mapper.replaceType(className) },
            { className2 : String -> mapper.replaceType(className2) },
            { bodyGroup : String -> rewriteBodyGroup(bodyGroup) }
        )
    )

    private val bodyReplacers = listOf(
        // [@annotation] [[!]public|private|etc...] <fields>;
        GroupsReplacer(
            pattern = PatternHelper.build(
                "^ *(@｟$ANNOTATION_TYPE｠ )?($FIELD_MODIFIERS )*<fields> *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) }
        )),

        // [@annotation] [[!]public|private|etc...] fieldType fieldName;
        GroupsReplacer(
            pattern = PatternHelper.build(
                "^ *(@｟$ANNOTATION_TYPE｠ )?($FIELD_MODIFIERS )*(｟$FIELD_TYPE｠ $FIELD_NAME) *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) },
                { fieldType : String -> mapper.replaceType(fieldType) }
        )),

        // [@annotation] [[!]public|private|etc...] <methods>;
        GroupsReplacer(
            pattern = PatternHelper.build(
                "^ *(@｟$ANNOTATION_TYPE｠ )?($METHOD_MODIFIERS )*<methods> *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) }
        )),

        // [@annotation] [[!]public|private|etc...] className(argumentType,...));
        GroupsReplacer(
            pattern = PatternHelper.build(
                "^ *(@｟$ANNOTATION_TYPE｠ )?($METHOD_MODIFIERS )*｟$CLASS_NAME｠ *\\(｟$ARGS｠\\) *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) },
                { className : String -> mapper.replaceType(className) },
                { argsType : String -> mapper.replaceMethodArgs(argsType) }
            )
        ),

        // [@annotation] [[!]public|private|etc...] <init>(argumentType,...));
        GroupsReplacer(
            pattern = PatternHelper.build(
                "^ *(@｟$ANNOTATION_TYPE｠ )?($METHOD_MODIFIERS )*<init> *\\(｟$ARGS｠\\) *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) },
                { argsType : String -> mapper.replaceMethodArgs(argsType) }
        )),

        // [@annotation] [[!]public|private|etc...] returnType methodName(argumentType,...));
        GroupsReplacer(
            pattern = PatternHelper.build("^ *(@｟$ANNOTATION_TYPE｠ )?($METHOD_MODIFIERS )*" +
                "｟$RETURN_TYPE_NAME｠ $METHOD_NAME *\\(｟$ARGS｠\\) *$"),
            groupsMap = listOf(
                { annotation : String -> mapper.replaceType(annotation) },
                { returnType : String -> mapper.replaceType(returnType) },
                { argsType : String -> mapper.replaceMethodArgs(argsType) }
        ))
    )

    private fun rewriteBodyGroup(bodyGroup: String) : String {
        if (bodyGroup == "*" || bodyGroup == "**") {
            return bodyGroup
        }

        return bodyGroup
            .split(';')
            .map {
                for (replacer in bodyReplacers) {
                    val matcher = replacer.pattern.matcher(it)
                    if (matcher.matches()) {
                        return@map replacer.runReplacements(matcher)
                    }
                }
                return@map it
            }
            .joinToString(";")
    }
}