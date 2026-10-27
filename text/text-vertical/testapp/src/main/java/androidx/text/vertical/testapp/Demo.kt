/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import androidx.compose.runtime.Composable

/** A demo with a [title] that [DemoApp] lists. */
sealed class Demo(val title: String) {
    override fun toString() = title
}

/** A demo that shows [content] when selected. */
class ComposableDemo(title: String, val content: @Composable () -> Unit) : Demo(title)

/** A category that lists [demos] when selected. */
class DemoCategory(title: String, val demos: List<Demo>) : Demo(title)

/**
 * Finds one demo by [query] and returns its path from [root] to the demo.
 *
 * [query] is a slash path ("HorizontalText/CJK font"), a breadcrumb path ("HorizontalText > CJK
 * font"), a title that is unique in the tree ("CJK font"), or a substring that matches one title
 * ("CJK"). Exact titles win over substrings.
 *
 * @throws IllegalArgumentException when no demo or more than one demo matches [query]
 */
fun findDemoPath(root: DemoCategory, query: String): List<Demo> {
    val paths = root.allPaths()
    val segments = query.split('/', '>').map(String::trim).filter(String::isNotEmpty)
    val matches =
        if (segments.size == 1) {
            val title = segments.single()
            paths
                .filter { it.last().title == title }
                .ifEmpty { paths.filter { it.last().title.contains(title) } }
        } else {
            paths.filter { path -> path.map(Demo::title).takeLast(segments.size) == segments }
        }
    require(matches.isNotEmpty()) { "No demo called \"$query\" could be found." }
    require(matches.size == 1) {
        "Found ${matches.size} demos matching \"$query\": " +
            matches.joinToString { it.drop(1).joinToString("/") }
    }
    return matches.single()
}

/** Returns the path from this category to itself and to every demo below it, depth first. */
private fun DemoCategory.allPaths(): List<List<Demo>> {
    val paths = mutableListOf<List<Demo>>()
    fun visit(demo: Demo, ancestors: List<Demo>) {
        val path = ancestors + demo
        paths += path
        if (demo is DemoCategory) demo.demos.forEach { visit(it, path) }
    }
    visit(this, emptyList())
    return paths
}
