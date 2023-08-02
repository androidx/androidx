/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata

/**
 * From [androidx.compose.ui.tooling.preview.datasource.LoremIpsum]
 */
private val LOREM_IPSUM = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer sodales
laoreet commodo. Phasellus a purus eu risus elementum consequat. Aenean eu
elit ut nunc convallis laoreet non ut libero. Suspendisse interdum placerat
risus vel ornare. Donec vehicula, turpis sed consectetur ullamcorper, ante
nunc egestas quam, ultricies adipiscing velit enim at nunc. Aenean id diam
neque. Praesent ut lacus sed justo viverra fermentum et ut sem. Fusce
convallis gravida lacinia. Integer semper dolor ut elit sagittis lacinia.
Praesent sodales scelerisque eros at rhoncus. Duis posuere sapien vel ipsum
ornare interdum at eu quam. Vestibulum vel massa erat. Aenean quis sagittis
purus. Phasellus arcu purus, rutrum id consectetur non, bibendum at nibh.

Duis nec erat dolor. Nulla vitae consectetur ligula. Quisque nec mi est. Ut
quam ante, rutrum at pellentesque gravida, pretium in dui. Cras eget sapien
velit. Suspendisse ut sem nec tellus vehicula eleifend sit amet quis velit.
Phasellus quis suscipit nisi. Nam elementum malesuada tincidunt. Curabitur
iaculis pretium eros, malesuada faucibus leo eleifend a. Curabitur congue
orci in neque euismod a blandit libero vehicula.""".trim()

private val LOREM_IPSUM_WORDS = LOREM_IPSUM.split(" ")

private val names = listOf(
    "Jacob",
    "Sophia",
    "Noah",
    "Emma",
    "Mason",
    "Isabella",
    "William",
    "Olivia",
    "Ethan",
    "Ava",
    "Liam",
    "Emily",
    "Michael",
    "Abigail",
    "Alexander",
    "Mia",
    "Jayden",
    "Madison",
    "Daniel",
    "Elizabeth",
    "Aiden",
    "Chloe",
    "James",
    "Ella",
    "Elijah",
    "Avery",
    "Matthew",
    "Charlotte",
    "Benjamin",
    "Sofia"
)

private val surnames = arrayOf(
    "Smith",
    "Johnson",
    "Williams",
    "Brown",
    "Jones",
    "Garcia",
    "Miller",
    "Davis",
    "Rodriguez",
    "Martinez"
)

private val cities = arrayOf(
    "Shanghai",
    "Karachi",
    "Beijing",
    "Delhi",
    "Lagos",
    "Tianjin",
    "Istanbul",
    "Tokyo",
    "Guangzhou",
    "Mumbai",
    "Moscow",
    "São Paulo",
    "Shenzhen",
    "Jakarta",
    "Lahore",
    "Seoul",
    "Wuhan",
    "Kinshasa",
    "Cairo",
    "Mexico City",
    "Lima",
    "London",
    "New York City"
)

internal fun randomFirstName(): String = names.random()

internal fun randomLastName(): String = surnames.random()

internal fun randomFullName(): String = randomFirstName() + " " + randomLastName()

internal fun randomCity(): String = cities.random()

internal object LoremIpsum {
    fun string(wordCount: Int, withLineBreaks: Boolean = false): String =
        words(wordCount, withLineBreaks).joinToString(separator = " ")

    fun words(wordCount: Int, withLineBreaks: Boolean = false): List<String> =
        if (withLineBreaks) {
            // Source includes line breaks
            LOREM_IPSUM_WORDS.take(wordCount.coerceIn(1, LOREM_IPSUM_WORDS.size))
        } else {
            LOREM_IPSUM.filter { it != '\n' }.split(' ')
                .take(wordCount.coerceIn(1, LOREM_IPSUM_WORDS.size))
        }
}
