/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.core.pom

import com.google.gson.annotations.SerializedName

/**
 * Represents a '<dependency>' XML node of a POM file.
 *
 * See documentation of the content at https://maven.apache.org/pom.html#Dependencies
 */
data class PomDependency(
    @SerializedName("groupId")
    val groupId: String?,

    @SerializedName("artifactId")
    val artifactId: String?,

    @SerializedName("version")
    var version: String? = null,

    @SerializedName("classifier")
    val classifier: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("scope")
    val scope: String? = null,

    @SerializedName("systemPath")
    val systemPath: String? = null,

    @SerializedName("optional")
    val optional: String? = null
) {

    /**
     * Returns a new dependency created by taking all the items from the [input] dependency and then
     * overwriting these with all of its non-null items.
     */
    fun rewrite(input: PomDependency, versions: DependencyVersions): PomDependency {
        var newVersion = input.version
        if (version != null) {
            newVersion = versions.applyOnVersionRef(version!!)
        }

        return PomDependency(
            groupId = groupId ?: input.groupId,
            artifactId = artifactId ?: input.artifactId,
            version = newVersion,
            classifier = classifier ?: input.classifier,
            type = type ?: input.type,
            scope = scope ?: input.scope,
            systemPath = systemPath ?: input.systemPath,
            optional = optional ?: input.optional
        )
    }

    /**
     * Returns the dependency in format "groupId:artifactId:version".
     */
    fun toStringNotation(): String {
        return "$groupId:$artifactId:$version"
    }

    /**
     * Returns the dependency in format "groupId:artifactId".
     */
    fun toStringNotationWithoutVersion(): String {
        return "$groupId:$artifactId"
    }
}