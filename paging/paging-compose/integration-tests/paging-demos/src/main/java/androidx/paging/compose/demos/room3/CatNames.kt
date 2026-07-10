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

package androidx.paging.compose.demos.room3

import kotlin.random.Random

private val topCatNames =
    listOf(
        "Luna",
        "Milo",
        "Oliver",
        "Leo",
        "Bella",
        "Charlie",
        "Lily",
        "Lucy",
        "Loki",
        "Max",
        "Willow",
        "Nala",
        "Simba",
        "Mochi",
        "Cleo",
        "Jasper",
        "Daisy",
        "Oreo",
        "Shadow",
        "Coco",
        "Poppy",
        "Oscar",
        "Pepper",
        "Felix",
        "Smore",
        "Sooty",
        "Misty",
        "Tigger",
        "George",
        "Alfie",
        "Millie",
        "Rosie",
        "Tilly",
        "Gizmo",
        "Bettsy",
        "Max",
        "Sox",
        "Fluffy",
        "Missy",
        "Sophie",
        "Belle",
        "Cookie",
        "Pebbles",
        "Harry",
        "Lola",
        "Mia",
        "Patch",
        "Ruby",
        "Bob",
        "Casper",
        "Ziggy",
        "Angel",
        "Bailey",
        "Fred",
        "Holly",
        "Maisie",
        "Billy",
        "Bonnie",
        "Freddie",
        "Princess",
        "Tabitha",
        "Tinkerbell",
        "Tommy",
        "Bobby",
        "Fifi",
        "Fudge",
        "Milly",
        "Snow",
        "Tia",
        "Tom",
        "Annie",
        "Bertie",
        "Brian",
        "Flo",
        "Jerry",
        "Kitty",
        "Maisy",
        "Meg",
        "Phoebe",
        "Teddy",
        "Evie",
        "Florence",
        "Minnie",
        "Ollie",
        "Polly",
        "Pumpkin",
        "Toby",
        "Benny",
        "Boo",
        "Bubbles",
        "Chloe",
        "Garfield",
        "Ginger",
        "Ginny",
        "Henry",
        "Izzy",
        "Joey",
        "Nemo",
        "Rio",
        "Miso",
    )

private val catSuffixes =
    listOf(
        "",
        "the 2nd",
        "the 3rd",
        "the 4th",
        "the 5th",
        "the 6th",
        "the 7th",
        "the 8th",
        "the 9th",
    )

internal fun getCatName() = topCatNames[Random.nextInt(0, topCatNames.size)]

internal fun getCatNameWithSuffix(name: String, count: Int): String =
    when (count) {
        0 -> name
        in 1..9 -> name + " " + catSuffixes[count]
        else -> "$name #$count"
    }
