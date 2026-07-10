/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush.behavior

/**
 * A [ValueNode] is a non-terminal node in the graph; it produces a value to be consumed as an input
 * by other [Node]s, and may itself depend on zero or more inputs.
 */
public abstract class ValueNode
internal constructor(nativeAlloc: () -> Long, inputs: List<ValueNode>) : Node(nativeAlloc, inputs)
