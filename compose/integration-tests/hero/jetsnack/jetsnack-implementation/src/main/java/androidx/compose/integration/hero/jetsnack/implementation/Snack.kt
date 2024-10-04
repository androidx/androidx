/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.jetsnack.implementation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class Snack(
    val id: Long,
    val name: String,
    @DrawableRes val imageDrawable: Int,
    val price: Long,
    val tagline: String = "",
    val tags: Set<String> = emptySet()
)

/** Static data */
val snacks =
    listOf(
        Snack(
            id = 1L,
            name = "Cupcake",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 2L,
            name = "Donut",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 3L,
            name = "Eclair",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 4L,
            name = "Froyo",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 5L,
            name = "Gingerbread",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 499
        ),
        Snack(
            id = 6L,
            name = "Honeycomb",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 7L,
            name = "Ice Cream Sandwich",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 1299
        ),
        Snack(
            id = 8L,
            name = "Jellybean",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 9L,
            name = "KitKat",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 549
        ),
        Snack(
            id = 10L,
            name = "Lollipop",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 11L,
            name = "Marshmallow",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 12L,
            name = "Nougat",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 13L,
            name = "Oreo",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 14L,
            name = "Pie",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(id = 15L, name = "Chips", imageDrawable = R.drawable.eclair, price = 299),
        Snack(id = 16L, name = "Pretzels", imageDrawable = R.drawable.eclair, price = 299),
        Snack(id = 17L, name = "Smoothies", imageDrawable = R.drawable.donut, price = 299),
        Snack(id = 18L, name = "Popcorn", imageDrawable = R.drawable.donut, price = 299),
        Snack(id = 19L, name = "Almonds", imageDrawable = R.drawable.eclair, price = 299),
        Snack(id = 20L, name = "Cheese", imageDrawable = R.drawable.eclair, price = 299),
        Snack(
            id = 21L,
            name = "Apples",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 22L,
            name = "Apple sauce",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 23L,
            name = "Apple chips",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 24L,
            name = "Apple juice",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 25L,
            name = "Apple pie",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 26L,
            name = "Grapes",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        ),
        Snack(
            id = 27L,
            name = "Kiwi",
            tagline = "A tag line",
            imageDrawable = R.drawable.eclair,
            price = 299
        ),
        Snack(
            id = 28L,
            name = "Mango",
            tagline = "A tag line",
            imageDrawable = R.drawable.donut,
            price = 299
        )
    )
