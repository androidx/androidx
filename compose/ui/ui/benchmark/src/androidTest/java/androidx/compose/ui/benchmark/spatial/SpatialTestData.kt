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

package androidx.compose.ui.benchmark.spatial

val occludingRectQueries =
    arrayOf(
        intArrayOf(490, 2073, 945, 2693),
        intArrayOf(84, 2777, 1356, 2993),
        intArrayOf(966, 2074, 1379, 2703),
        intArrayOf(84, 1548, 1356, 1821),
        intArrayOf(35, 2073, 490, 2693),
    )

val nearestNeighborQueries =
    arrayOf(
        intArrayOf(56, 2074, 469, 2703), // left side app image
        intArrayOf(288, 2812, 576, 3036), // bottom nav bar middle button
        intArrayOf(1048, 478, 1187, 548), // install button
        intArrayOf(983, 1580, 1300, 1790), // show button
        intArrayOf(1258, 1933, 1342, 2017), // more icon
    )

val pointerInputQueries =
    arrayOf(
        intArrayOf(1120, 1654), // Show button
        intArrayOf(1263, 1943), // three dots button
        intArrayOf(615, 2312), // app image
        intArrayOf(1100, 496), // install button
        intArrayOf(710, 215), // search bar
    )

class Item(
    val id: Int,
    val bounds: IntArray,
    val scrollable: Boolean,
    val focusable: Boolean,
    val pointerInput: Boolean,
) {
    val children: MutableList<Item> = mutableListOf()

    operator fun Item.unaryPlus() {
        @Suppress("LABEL_RESOLVE_WILL_CHANGE") this@Item.children.add(this)
    }
}

fun Item(
    id: Int,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
    scrollable: Boolean,
    focusable: Boolean,
    pointerInput: Boolean,
): Item = Item(id, intArrayOf(l, t, r, b), scrollable, focusable, pointerInput)

fun Item(
    id: Int,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
    scrollable: Boolean,
    focusable: Boolean,
    pointerInput: Boolean,
    scope: Item.() -> Unit
): Item {
    return Item(id, intArrayOf(l, t, r, b), scrollable, focusable, pointerInput).apply { scope() }
}

val rootItem =
    Item(0, 0, 0, 1440, 3120, false, false, false) {
        +Item(1, 0, 0, 1440, 3120, false, false, false) {
            +Item(2, 0, 0, 1, 1, false, false, false)
            +Item(3, 0, 0, 1440, 3120, false, false, false) {
                +Item(4, 0, 0, 1440, 3120, false, false, false) {
                    +Item(5, 0, 0, 1440, 3120, false, false, false) {
                        +Item(6, 0, 0, 1440, 3120, false, false, false) {
                            +Item(7, 0, 0, 1440, 3120, false, false, false) {
                                +Item(8, 0, 0, 1440, 3120, false, false, false) {
                                    +Item(9, 0, 0, 1440, 3120, false, false, false) {
                                        +Item(10, 0, 0, 1440, 3036, false, false, false) {
                                            +Item(11, 0, 0, 1440, 2812, false, false, false) {
                                                +Item(12, 0, 0, 1440, 2812, false, false, false) {
                                                    +Item(
                                                        13,
                                                        0,
                                                        0,
                                                        1440,
                                                        2812,
                                                        false,
                                                        false,
                                                        false
                                                    ) {
                                                        +Item(
                                                            14,
                                                            0,
                                                            145,
                                                            1440,
                                                            2812,
                                                            false,
                                                            false,
                                                            false
                                                        ) {
                                                            +Item(
                                                                15,
                                                                0,
                                                                145,
                                                                1440,
                                                                2812,
                                                                false,
                                                                false,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    16,
                                                                    0,
                                                                    145,
                                                                    1440,
                                                                    373,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        17,
                                                                        0,
                                                                        145,
                                                                        1440,
                                                                        369,
                                                                        false,
                                                                        false,
                                                                        true
                                                                    ) {
                                                                        +Item(
                                                                            18,
                                                                            0,
                                                                            145,
                                                                            1440,
                                                                            369,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        ) {
                                                                            +Item(
                                                                                19,
                                                                                14,
                                                                                173,
                                                                                182,
                                                                                341,
                                                                                false,
                                                                                false,
                                                                                false
                                                                            ) {
                                                                                +Item(
                                                                                    20,
                                                                                    14,
                                                                                    173,
                                                                                    182,
                                                                                    341,
                                                                                    false,
                                                                                    false,
                                                                                    false
                                                                                ) {
                                                                                    +Item(
                                                                                        21,
                                                                                        14,
                                                                                        173,
                                                                                        182,
                                                                                        341,
                                                                                        false,
                                                                                        false,
                                                                                        true
                                                                                    ) {
                                                                                        +Item(
                                                                                            22,
                                                                                            14,
                                                                                            173,
                                                                                            182,
                                                                                            341,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                23,
                                                                                                28,
                                                                                                187,
                                                                                                168,
                                                                                                327,
                                                                                                false,
                                                                                                true,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    24,
                                                                                                    56,
                                                                                                    215,
                                                                                                    140,
                                                                                                    299,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            +Item(
                                                                                25,
                                                                                196,
                                                                                -1076,
                                                                                1076,
                                                                                1591,
                                                                                false,
                                                                                false,
                                                                                false
                                                                            ) {
                                                                                +Item(
                                                                                    26,
                                                                                    196,
                                                                                    -1076,
                                                                                    1076,
                                                                                    1591,
                                                                                    false,
                                                                                    true,
                                                                                    false
                                                                                ) {
                                                                                    +Item(
                                                                                        27,
                                                                                        196,
                                                                                        216,
                                                                                        337,
                                                                                        300,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    )
                                                                                }
                                                                            }
                                                                            +Item(
                                                                                28,
                                                                                1090,
                                                                                173,
                                                                                1426,
                                                                                341,
                                                                                false,
                                                                                false,
                                                                                false
                                                                            ) {
                                                                                +Item(
                                                                                    29,
                                                                                    1090,
                                                                                    173,
                                                                                    1426,
                                                                                    341,
                                                                                    false,
                                                                                    false,
                                                                                    false
                                                                                ) {
                                                                                    +Item(
                                                                                        30,
                                                                                        1090,
                                                                                        173,
                                                                                        1426,
                                                                                        341,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    ) {
                                                                                        +Item(
                                                                                            31,
                                                                                            1090,
                                                                                            173,
                                                                                            1426,
                                                                                            341,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                32,
                                                                                                1090,
                                                                                                173,
                                                                                                1258,
                                                                                                341,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    33,
                                                                                                    1090,
                                                                                                    173,
                                                                                                    1258,
                                                                                                    341,
                                                                                                    false,
                                                                                                    false,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        34,
                                                                                                        1090,
                                                                                                        173,
                                                                                                        1258,
                                                                                                        341,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            35,
                                                                                                            1104,
                                                                                                            187,
                                                                                                            1244,
                                                                                                            327,
                                                                                                            false,
                                                                                                            true,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                36,
                                                                                                                1132,
                                                                                                                215,
                                                                                                                1216,
                                                                                                                299,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            +Item(
                                                                                                37,
                                                                                                1258,
                                                                                                173,
                                                                                                1426,
                                                                                                341,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    38,
                                                                                                    1258,
                                                                                                    173,
                                                                                                    1426,
                                                                                                    341,
                                                                                                    false,
                                                                                                    false,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        39,
                                                                                                        1258,
                                                                                                        173,
                                                                                                        1426,
                                                                                                        341,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            40,
                                                                                                            1272,
                                                                                                            187,
                                                                                                            1412,
                                                                                                            327,
                                                                                                            false,
                                                                                                            true,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                41,
                                                                                                                1300,
                                                                                                                215,
                                                                                                                1384,
                                                                                                                299,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    +Item(
                                                                        42,
                                                                        0,
                                                                        369,
                                                                        1440,
                                                                        373,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    )
                                                                }
                                                                +Item(
                                                                    43,
                                                                    0,
                                                                    373,
                                                                    1440,
                                                                    2812,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        44,
                                                                        0,
                                                                        373,
                                                                        1440,
                                                                        2812,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            45,
                                                                            0,
                                                                            373,
                                                                            1440,
                                                                            2812,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        ) {
                                                                            +Item(
                                                                                46,
                                                                                0,
                                                                                373,
                                                                                1440,
                                                                                2812,
                                                                                true,
                                                                                false,
                                                                                true
                                                                            ) {
                                                                                +Item(
                                                                                    47,
                                                                                    0,
                                                                                    1877,
                                                                                    1440,
                                                                                    2735,
                                                                                    false,
                                                                                    false,
                                                                                    false
                                                                                ) {
                                                                                    +Item(
                                                                                        48,
                                                                                        0,
                                                                                        1877,
                                                                                        1440,
                                                                                        2693,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    ) {
                                                                                        +Item(
                                                                                            49,
                                                                                            84,
                                                                                            1877,
                                                                                            1356,
                                                                                            2073,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                50,
                                                                                                84,
                                                                                                1947,
                                                                                                354,
                                                                                                2003,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    51,
                                                                                                    84,
                                                                                                    1947,
                                                                                                    302,
                                                                                                    2003,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                            +Item(
                                                                                                52,
                                                                                                354,
                                                                                                1933,
                                                                                                1188,
                                                                                                2017,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    53,
                                                                                                    354,
                                                                                                    1933,
                                                                                                    1012,
                                                                                                    2017,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        54,
                                                                                                        354,
                                                                                                        1933,
                                                                                                        1012,
                                                                                                        2017,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                            +Item(
                                                                                                55,
                                                                                                1258,
                                                                                                1933,
                                                                                                1342,
                                                                                                2017,
                                                                                                false,
                                                                                                true,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    56,
                                                                                                    1258,
                                                                                                    1933,
                                                                                                    1342,
                                                                                                    2017,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                        +Item(
                                                                                            57,
                                                                                            0,
                                                                                            2073,
                                                                                            1440,
                                                                                            2693,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                58,
                                                                                                0,
                                                                                                2073,
                                                                                                1440,
                                                                                                2693,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    59,
                                                                                                    0,
                                                                                                    2073,
                                                                                                    1440,
                                                                                                    2693,
                                                                                                    true,
                                                                                                    false,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        60,
                                                                                                        945,
                                                                                                        2073,
                                                                                                        1400,
                                                                                                        2693,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            61,
                                                                                                            966,
                                                                                                            2074,
                                                                                                            1379,
                                                                                                            2703,
                                                                                                            false,
                                                                                                            true,
                                                                                                            true
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                62,
                                                                                                                966,
                                                                                                                2074,
                                                                                                                1379,
                                                                                                                2487,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    63,
                                                                                                                    966,
                                                                                                                    2074,
                                                                                                                    1379,
                                                                                                                    2487,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                            }
                                                                                                            +Item(
                                                                                                                64,
                                                                                                                966,
                                                                                                                2515,
                                                                                                                1379,
                                                                                                                2703,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                    +Item(
                                                                                                        65,
                                                                                                        490,
                                                                                                        2073,
                                                                                                        945,
                                                                                                        2693,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            66,
                                                                                                            511,
                                                                                                            2074,
                                                                                                            924,
                                                                                                            2703,
                                                                                                            false,
                                                                                                            true,
                                                                                                            true
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                67,
                                                                                                                511,
                                                                                                                2074,
                                                                                                                924,
                                                                                                                2487,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    68,
                                                                                                                    511,
                                                                                                                    2074,
                                                                                                                    924,
                                                                                                                    2487,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                            }
                                                                                                            +Item(
                                                                                                                69,
                                                                                                                511,
                                                                                                                2515,
                                                                                                                924,
                                                                                                                2703,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                    +Item(
                                                                                                        70,
                                                                                                        1400,
                                                                                                        2073,
                                                                                                        1855,
                                                                                                        2693,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            71,
                                                                                                            1421,
                                                                                                            2074,
                                                                                                            1834,
                                                                                                            2703,
                                                                                                            false,
                                                                                                            true,
                                                                                                            true
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                72,
                                                                                                                1421,
                                                                                                                2074,
                                                                                                                1834,
                                                                                                                2487,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    73,
                                                                                                                    1421,
                                                                                                                    2074,
                                                                                                                    1834,
                                                                                                                    2487,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                            }
                                                                                                            +Item(
                                                                                                                74,
                                                                                                                1421,
                                                                                                                2515,
                                                                                                                1834,
                                                                                                                2703,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                    +Item(
                                                                                                        75,
                                                                                                        35,
                                                                                                        2073,
                                                                                                        490,
                                                                                                        2693,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            76,
                                                                                                            56,
                                                                                                            2074,
                                                                                                            469,
                                                                                                            2703,
                                                                                                            false,
                                                                                                            true,
                                                                                                            true
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                77,
                                                                                                                56,
                                                                                                                2074,
                                                                                                                469,
                                                                                                                2487,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    78,
                                                                                                                    56,
                                                                                                                    2074,
                                                                                                                    469,
                                                                                                                    2487,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                            }
                                                                                                            +Item(
                                                                                                                79,
                                                                                                                56,
                                                                                                                2515,
                                                                                                                469,
                                                                                                                2703,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                +Item(
                                                                                    80,
                                                                                    0,
                                                                                    2777,
                                                                                    1440,
                                                                                    2993,
                                                                                    false,
                                                                                    true,
                                                                                    false
                                                                                ) {
                                                                                    +Item(
                                                                                        81,
                                                                                        84,
                                                                                        2777,
                                                                                        1356,
                                                                                        2993,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    ) {
                                                                                        +Item(
                                                                                            82,
                                                                                            84,
                                                                                            2777,
                                                                                            1188,
                                                                                            2993,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                83,
                                                                                                84,
                                                                                                2777,
                                                                                                280,
                                                                                                2973,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    84,
                                                                                                    84,
                                                                                                    2777,
                                                                                                    280,
                                                                                                    2973,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                            +Item(
                                                                                                85,
                                                                                                336,
                                                                                                2777,
                                                                                                1188,
                                                                                                2993,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            )
                                                                                        }
                                                                                        +Item(
                                                                                            86,
                                                                                            1188,
                                                                                            2777,
                                                                                            1356,
                                                                                            2945,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                87,
                                                                                                1272,
                                                                                                2826,
                                                                                                1356,
                                                                                                2910,
                                                                                                false,
                                                                                                true,
                                                                                                false
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                                +Item(
                                                                                    88,
                                                                                    0,
                                                                                    373,
                                                                                    1440,
                                                                                    1849,
                                                                                    false,
                                                                                    true,
                                                                                    true
                                                                                ) {
                                                                                    +Item(
                                                                                        89,
                                                                                        84,
                                                                                        429,
                                                                                        1356,
                                                                                        645,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    ) {
                                                                                        +Item(
                                                                                            90,
                                                                                            84,
                                                                                            436,
                                                                                            280,
                                                                                            632,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                91,
                                                                                                84,
                                                                                                436,
                                                                                                280,
                                                                                                632,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            )
                                                                                        }
                                                                                        +Item(
                                                                                            92,
                                                                                            336,
                                                                                            436,
                                                                                            950,
                                                                                            652,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        )
                                                                                        +Item(
                                                                                            93,
                                                                                            992,
                                                                                            429,
                                                                                            1356,
                                                                                            639,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                94,
                                                                                                992,
                                                                                                429,
                                                                                                1356,
                                                                                                597,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    95,
                                                                                                    992,
                                                                                                    429,
                                                                                                    1356,
                                                                                                    597,
                                                                                                    false,
                                                                                                    false,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        96,
                                                                                                        992,
                                                                                                        443,
                                                                                                        1356,
                                                                                                        583,
                                                                                                        false,
                                                                                                        true,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            97,
                                                                                                            1048,
                                                                                                            443,
                                                                                                            1356,
                                                                                                            583,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                98,
                                                                                                                1048,
                                                                                                                478,
                                                                                                                1187,
                                                                                                                548,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    99,
                                                                                                                    1048,
                                                                                                                    478,
                                                                                                                    1187,
                                                                                                                    548,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                            }
                                                                                                            +Item(
                                                                                                                100,
                                                                                                                1229,
                                                                                                                443,
                                                                                                                1356,
                                                                                                                583,
                                                                                                                false,
                                                                                                                true,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    101,
                                                                                                                    1229,
                                                                                                                    443,
                                                                                                                    1233,
                                                                                                                    583,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                                +Item(
                                                                                                                    102,
                                                                                                                    1233,
                                                                                                                    443,
                                                                                                                    1356,
                                                                                                                    583,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                ) {
                                                                                                                    +Item(
                                                                                                                        103,
                                                                                                                        1256,
                                                                                                                        482,
                                                                                                                        1319,
                                                                                                                        545,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            +Item(
                                                                                                104,
                                                                                                1056,
                                                                                                429,
                                                                                                1293,
                                                                                                639,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    105,
                                                                                                    1056,
                                                                                                    583,
                                                                                                    1293,
                                                                                                    639,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    +Item(
                                                                                        106,
                                                                                        0,
                                                                                        687,
                                                                                        1440,
                                                                                        1849,
                                                                                        false,
                                                                                        false,
                                                                                        false
                                                                                    ) {
                                                                                        +Item(
                                                                                            107,
                                                                                            0,
                                                                                            687,
                                                                                            1440,
                                                                                            1849,
                                                                                            false,
                                                                                            false,
                                                                                            false
                                                                                        ) {
                                                                                            +Item(
                                                                                                108,
                                                                                                0,
                                                                                                687,
                                                                                                1440,
                                                                                                911,
                                                                                                true,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    109,
                                                                                                    0,
                                                                                                    799,
                                                                                                    84,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    110,
                                                                                                    84,
                                                                                                    729,
                                                                                                    377,
                                                                                                    869,
                                                                                                    false,
                                                                                                    true,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        111,
                                                                                                        171,
                                                                                                        733,
                                                                                                        291,
                                                                                                        803,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            112,
                                                                                                            171,
                                                                                                            733,
                                                                                                            242,
                                                                                                            803,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                        +Item(
                                                                                                            113,
                                                                                                            249,
                                                                                                            747,
                                                                                                            291,
                                                                                                            789,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                    +Item(
                                                                                                        114,
                                                                                                        84,
                                                                                                        810,
                                                                                                        377,
                                                                                                        866,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            115,
                                                                                                            338,
                                                                                                            818,
                                                                                                            377,
                                                                                                            857,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                }
                                                                                                +Item(
                                                                                                    116,
                                                                                                    377,
                                                                                                    799,
                                                                                                    433,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    117,
                                                                                                    433,
                                                                                                    762,
                                                                                                    437,
                                                                                                    837,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    118,
                                                                                                    437,
                                                                                                    799,
                                                                                                    493,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    119,
                                                                                                    493,
                                                                                                    729,
                                                                                                    839,
                                                                                                    869,
                                                                                                    false,
                                                                                                    true,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        120,
                                                                                                        631,
                                                                                                        733,
                                                                                                        701,
                                                                                                        803,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            121,
                                                                                                            631,
                                                                                                            733,
                                                                                                            701,
                                                                                                            803,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                    +Item(
                                                                                                        122,
                                                                                                        493,
                                                                                                        810,
                                                                                                        839,
                                                                                                        866,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            123,
                                                                                                            799,
                                                                                                            818,
                                                                                                            838,
                                                                                                            857,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                }
                                                                                                +Item(
                                                                                                    124,
                                                                                                    839,
                                                                                                    799,
                                                                                                    895,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    125,
                                                                                                    895,
                                                                                                    762,
                                                                                                    899,
                                                                                                    837,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    126,
                                                                                                    899,
                                                                                                    799,
                                                                                                    955,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    127,
                                                                                                    955,
                                                                                                    729,
                                                                                                    1235,
                                                                                                    869,
                                                                                                    false,
                                                                                                    true,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        128,
                                                                                                        1060,
                                                                                                        733,
                                                                                                        1130,
                                                                                                        803,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            129,
                                                                                                            1060,
                                                                                                            733,
                                                                                                            1130,
                                                                                                            803,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                    +Item(
                                                                                                        130,
                                                                                                        1022,
                                                                                                        810,
                                                                                                        1168,
                                                                                                        866,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            131,
                                                                                                            1129,
                                                                                                            818,
                                                                                                            1168,
                                                                                                            857,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                }
                                                                                                +Item(
                                                                                                    132,
                                                                                                    1235,
                                                                                                    799,
                                                                                                    1291,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    133,
                                                                                                    1291,
                                                                                                    762,
                                                                                                    1295,
                                                                                                    837,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    134,
                                                                                                    1295,
                                                                                                    799,
                                                                                                    1351,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                                +Item(
                                                                                                    135,
                                                                                                    1351,
                                                                                                    729,
                                                                                                    1631,
                                                                                                    869,
                                                                                                    false,
                                                                                                    true,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        136,
                                                                                                        1451,
                                                                                                        733,
                                                                                                        1532,
                                                                                                        803,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            137,
                                                                                                            1451,
                                                                                                            733,
                                                                                                            1532,
                                                                                                            803,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        )
                                                                                                    }
                                                                                                    +Item(
                                                                                                        138,
                                                                                                        1383,
                                                                                                        810,
                                                                                                        1599,
                                                                                                        866,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    )
                                                                                                }
                                                                                                +Item(
                                                                                                    139,
                                                                                                    1631,
                                                                                                    799,
                                                                                                    1687,
                                                                                                    799,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                )
                                                                                            }
                                                                                            +Item(
                                                                                                140,
                                                                                                0,
                                                                                                911,
                                                                                                1440,
                                                                                                1506,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    141,
                                                                                                    0,
                                                                                                    911,
                                                                                                    1440,
                                                                                                    1506,
                                                                                                    false,
                                                                                                    false,
                                                                                                    false
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        142,
                                                                                                        0,
                                                                                                        911,
                                                                                                        1440,
                                                                                                        1506,
                                                                                                        true,
                                                                                                        false,
                                                                                                        true
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            143,
                                                                                                            1348,
                                                                                                            911,
                                                                                                            2644,
                                                                                                            1506,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                144,
                                                                                                                1380,
                                                                                                                911,
                                                                                                                2644,
                                                                                                                1506,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    145,
                                                                                                                    1380,
                                                                                                                    911,
                                                                                                                    2644,
                                                                                                                    1506,
                                                                                                                    false,
                                                                                                                    true,
                                                                                                                    false
                                                                                                                ) {
                                                                                                                    +Item(
                                                                                                                        146,
                                                                                                                        1380,
                                                                                                                        911,
                                                                                                                        1975,
                                                                                                                        1506,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                    +Item(
                                                                                                                        147,
                                                                                                                        2031,
                                                                                                                        911,
                                                                                                                        2588,
                                                                                                                        1506,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    ) {
                                                                                                                        +Item(
                                                                                                                            148,
                                                                                                                            2031,
                                                                                                                            967,
                                                                                                                            2588,
                                                                                                                            1023,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            149,
                                                                                                                            2031,
                                                                                                                            1051,
                                                                                                                            2588,
                                                                                                                            1121,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            150,
                                                                                                                            2031,
                                                                                                                            1149,
                                                                                                                            2588,
                                                                                                                            1289,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            151,
                                                                                                                            2031,
                                                                                                                            1345,
                                                                                                                            2031,
                                                                                                                            1506,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                        +Item(
                                                                                                            152,
                                                                                                            84,
                                                                                                            911,
                                                                                                            1348,
                                                                                                            1506,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                153,
                                                                                                                84,
                                                                                                                911,
                                                                                                                1348,
                                                                                                                1506,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    154,
                                                                                                                    84,
                                                                                                                    911,
                                                                                                                    1348,
                                                                                                                    1506,
                                                                                                                    false,
                                                                                                                    true,
                                                                                                                    false
                                                                                                                ) {
                                                                                                                    +Item(
                                                                                                                        155,
                                                                                                                        84,
                                                                                                                        911,
                                                                                                                        679,
                                                                                                                        1506,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                    +Item(
                                                                                                                        156,
                                                                                                                        735,
                                                                                                                        911,
                                                                                                                        1292,
                                                                                                                        1506,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    ) {
                                                                                                                        +Item(
                                                                                                                            157,
                                                                                                                            735,
                                                                                                                            967,
                                                                                                                            1292,
                                                                                                                            1023,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            158,
                                                                                                                            735,
                                                                                                                            1051,
                                                                                                                            1292,
                                                                                                                            1191,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            159,
                                                                                                                            735,
                                                                                                                            1219,
                                                                                                                            1292,
                                                                                                                            1359,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                        +Item(
                                                                                                                            160,
                                                                                                                            735,
                                                                                                                            1415,
                                                                                                                            735,
                                                                                                                            1506,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            +Item(
                                                                                                161,
                                                                                                0,
                                                                                                1506,
                                                                                                1440,
                                                                                                1849,
                                                                                                false,
                                                                                                false,
                                                                                                false
                                                                                            ) {
                                                                                                +Item(
                                                                                                    162,
                                                                                                    84,
                                                                                                    1548,
                                                                                                    1356,
                                                                                                    1821,
                                                                                                    false,
                                                                                                    false,
                                                                                                    true
                                                                                                ) {
                                                                                                    +Item(
                                                                                                        163,
                                                                                                        84,
                                                                                                        1548,
                                                                                                        1356,
                                                                                                        1821,
                                                                                                        false,
                                                                                                        false,
                                                                                                        false
                                                                                                    ) {
                                                                                                        +Item(
                                                                                                            164,
                                                                                                            140,
                                                                                                            1548,
                                                                                                            1300,
                                                                                                            1821,
                                                                                                            false,
                                                                                                            false,
                                                                                                            false
                                                                                                        ) {
                                                                                                            +Item(
                                                                                                                165,
                                                                                                                140,
                                                                                                                1604,
                                                                                                                927,
                                                                                                                1765,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    166,
                                                                                                                    140,
                                                                                                                    1604,
                                                                                                                    865,
                                                                                                                    1688,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                )
                                                                                                                +Item(
                                                                                                                    167,
                                                                                                                    140,
                                                                                                                    1702,
                                                                                                                    469,
                                                                                                                    1765,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    false
                                                                                                                ) {
                                                                                                                    +Item(
                                                                                                                        168,
                                                                                                                        140,
                                                                                                                        1702,
                                                                                                                        203,
                                                                                                                        1765,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                    +Item(
                                                                                                                        169,
                                                                                                                        217,
                                                                                                                        1706,
                                                                                                                        469,
                                                                                                                        1762,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                }
                                                                                                            }
                                                                                                            +Item(
                                                                                                                170,
                                                                                                                983,
                                                                                                                1580,
                                                                                                                1300,
                                                                                                                1790,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false
                                                                                                            ) {
                                                                                                                +Item(
                                                                                                                    171,
                                                                                                                    1040,
                                                                                                                    1615,
                                                                                                                    1243,
                                                                                                                    1755,
                                                                                                                    false,
                                                                                                                    true,
                                                                                                                    true
                                                                                                                ) {
                                                                                                                    +Item(
                                                                                                                        172,
                                                                                                                        1040,
                                                                                                                        1654,
                                                                                                                        1103,
                                                                                                                        1717,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    )
                                                                                                                    +Item(
                                                                                                                        173,
                                                                                                                        1103,
                                                                                                                        1650,
                                                                                                                        1243,
                                                                                                                        1720,
                                                                                                                        false,
                                                                                                                        false,
                                                                                                                        false
                                                                                                                    ) {
                                                                                                                        +Item(
                                                                                                                            174,
                                                                                                                            1117,
                                                                                                                            1650,
                                                                                                                            1243,
                                                                                                                            1720,
                                                                                                                            false,
                                                                                                                            false,
                                                                                                                            false
                                                                                                                        )
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    +Item(
                                                        175,
                                                        42,
                                                        2770,
                                                        1398,
                                                        2770,
                                                        false,
                                                        false,
                                                        false
                                                    )
                                                }
                                            }
                                            +Item(176, 0, 2812, 1440, 3036, false, false, false) {
                                                +Item(
                                                    177,
                                                    0,
                                                    2812,
                                                    1440,
                                                    3036,
                                                    false,
                                                    false,
                                                    true
                                                ) {
                                                    +Item(
                                                        178,
                                                        0,
                                                        2812,
                                                        1440,
                                                        3036,
                                                        false,
                                                        false,
                                                        false
                                                    ) {
                                                        +Item(
                                                            179,
                                                            0,
                                                            2812,
                                                            1440,
                                                            3036,
                                                            false,
                                                            false,
                                                            false
                                                        ) {
                                                            +Item(
                                                                180,
                                                                0,
                                                                2812,
                                                                288,
                                                                3036,
                                                                false,
                                                                true,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    181,
                                                                    0,
                                                                    2812,
                                                                    288,
                                                                    3036,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        182,
                                                                        46,
                                                                        2847,
                                                                        242,
                                                                        2931,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            183,
                                                                            102,
                                                                            2847,
                                                                            186,
                                                                            2931,
                                                                            false,
                                                                            true,
                                                                            false
                                                                        )
                                                                    }
                                                                    +Item(
                                                                        184,
                                                                        14,
                                                                        2931,
                                                                        274,
                                                                        3001,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            185,
                                                                            74,
                                                                            2945,
                                                                            214,
                                                                            3001,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            +Item(
                                                                186,
                                                                288,
                                                                2812,
                                                                576,
                                                                3036,
                                                                false,
                                                                true,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    187,
                                                                    288,
                                                                    2812,
                                                                    576,
                                                                    3036,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        188,
                                                                        334,
                                                                        2847,
                                                                        530,
                                                                        2931,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            189,
                                                                            390,
                                                                            2847,
                                                                            474,
                                                                            2931,
                                                                            false,
                                                                            true,
                                                                            false
                                                                        )
                                                                    }
                                                                    +Item(
                                                                        190,
                                                                        302,
                                                                        2931,
                                                                        562,
                                                                        3001,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            191,
                                                                            381,
                                                                            2945,
                                                                            484,
                                                                            3001,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            +Item(
                                                                192,
                                                                576,
                                                                2812,
                                                                864,
                                                                3036,
                                                                false,
                                                                true,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    193,
                                                                    576,
                                                                    2812,
                                                                    864,
                                                                    3036,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        194,
                                                                        622,
                                                                        2847,
                                                                        818,
                                                                        2931,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            195,
                                                                            678,
                                                                            2847,
                                                                            762,
                                                                            2931,
                                                                            false,
                                                                            true,
                                                                            false
                                                                        )
                                                                    }
                                                                    +Item(
                                                                        196,
                                                                        590,
                                                                        2931,
                                                                        850,
                                                                        3001,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            197,
                                                                            649,
                                                                            2945,
                                                                            792,
                                                                            3001,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            +Item(
                                                                198,
                                                                864,
                                                                2812,
                                                                1152,
                                                                3036,
                                                                false,
                                                                true,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    199,
                                                                    864,
                                                                    2812,
                                                                    1152,
                                                                    3036,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        200,
                                                                        910,
                                                                        2847,
                                                                        1106,
                                                                        2931,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            201,
                                                                            966,
                                                                            2847,
                                                                            1050,
                                                                            2931,
                                                                            false,
                                                                            true,
                                                                            false
                                                                        )
                                                                    }
                                                                    +Item(
                                                                        202,
                                                                        878,
                                                                        2931,
                                                                        1138,
                                                                        3001,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            203,
                                                                            964,
                                                                            2945,
                                                                            1052,
                                                                            3001,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            +Item(
                                                                204,
                                                                1152,
                                                                2812,
                                                                1440,
                                                                3036,
                                                                false,
                                                                true,
                                                                false
                                                            ) {
                                                                +Item(
                                                                    205,
                                                                    1152,
                                                                    2812,
                                                                    1440,
                                                                    3036,
                                                                    false,
                                                                    false,
                                                                    false
                                                                ) {
                                                                    +Item(
                                                                        206,
                                                                        1198,
                                                                        2847,
                                                                        1394,
                                                                        2931,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            207,
                                                                            1254,
                                                                            2847,
                                                                            1338,
                                                                            2931,
                                                                            false,
                                                                            true,
                                                                            false
                                                                        )
                                                                    }
                                                                    +Item(
                                                                        208,
                                                                        1166,
                                                                        2931,
                                                                        1426,
                                                                        3001,
                                                                        false,
                                                                        false,
                                                                        false
                                                                    ) {
                                                                        +Item(
                                                                            209,
                                                                            1235,
                                                                            2945,
                                                                            1358,
                                                                            3001,
                                                                            false,
                                                                            false,
                                                                            false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        +Item(210, 0, 0, 1440, 3120, false, false, false) {
                                            +Item(211, 102, 2847, 186, 2931, false, false, false) {
                                                +Item(
                                                    212,
                                                    102,
                                                    2847,
                                                    186,
                                                    2931,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            }
                                            +Item(213, 390, 2847, 474, 2931, false, false, false) {
                                                +Item(
                                                    214,
                                                    390,
                                                    2847,
                                                    474,
                                                    2931,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            }
                                            +Item(215, 678, 2847, 762, 2931, false, false, false) {
                                                +Item(
                                                    216,
                                                    678,
                                                    2847,
                                                    762,
                                                    2931,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            }
                                            +Item(217, 966, 2847, 1050, 2931, false, false, false) {
                                                +Item(
                                                    218,
                                                    966,
                                                    2847,
                                                    1050,
                                                    2931,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            }
                                            +Item(
                                                219,
                                                1254,
                                                2847,
                                                1338,
                                                2931,
                                                false,
                                                false,
                                                false
                                            ) {
                                                +Item(
                                                    220,
                                                    1254,
                                                    2847,
                                                    1338,
                                                    2931,
                                                    false,
                                                    false,
                                                    false
                                                )
                                            }
                                        }
                                        +Item(221, 0, 0, 1, 1, false, false, false) {
                                            +Item(222, 0, 0, 1440, 196, false, false, false)
                                        }
                                    }
                                }
                                +Item(223, 0, 0, 1440, 3120, false, false, false) {
                                    +Item(224, 636, 1476, 804, 1644, false, false, false)
                                }
                                +Item(225, 0, 0, 1, 1, false, false, false)
                            }
                            +Item(226, 0, 0, 1440, 145, false, false, false) {
                                +Item(227, 0, 145, 1440, 145, false, false, false)
                            }
                        }
                    }
                    +Item(228, 0, 0, 1, 1, false, false, false)
                }
            }
        }
        +Item(229, 0, 3036, 1440, 3120, false, false, false)
        +Item(230, 0, 0, 1440, 145, false, false, false)
    }

val exampleLayoutRects: Array<IntArray> = run {
    val emptyIntArray = IntArray(0)
    val results = Array(231) { emptyIntArray }

    fun push(item: Item) {
        results[item.id] = item.bounds
        item.children.forEach { child -> push(child) }
    }
    push(rootItem)
    for (bounds in results) {
        assert(bounds !== emptyIntArray)
    }
    results
}

val scrollableItems: List<Item> = run {
    val results = mutableListOf<Item>()

    fun traverse(item: Item) {
        if (item.scrollable) {
            results.add(item)
        }
        item.children.forEach { child -> traverse(child) }
    }
    traverse(rootItem)
    results
}
