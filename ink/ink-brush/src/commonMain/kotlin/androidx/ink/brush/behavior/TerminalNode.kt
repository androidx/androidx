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
 * A [TerminalNode] is a terminal node in the graph; it does not produce a value and cannot be used
 * as an input to other [Node]s, but instead applies a modification to the brush tip state. A
 * [androidx.ink.brush.BrushBehavior] consists of a list of [TerminalNode]s and the various
 * [ValueNode]s that they transitively depend on.
 */
public abstract class TerminalNode
internal constructor(nativeAlloc: () -> Long, inputs: List<ValueNode>) :
    Node(nativeAlloc, inputs) {}
