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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

// TODO(mount): regain the inline functions. These don't seem to work with jococo tests
enum class ErrorMessages(val message: String) {
    ComponentNodeHasParent("Inserting an instance that already has a parent"),
    SizeAlreadyExists("<Layout> can only be used once within a <MeasureBox>"),
    NoSizeAfterLayout("<MeasureBox> requires one <Layout> element"),
    OnlyComponents("Don't know how to add a non-composable element to the hierarchy"),
    NoMovingSingleElements("Cannot move elements that contain a maximum of one child"),
    NoChild("There is no child in this node"),
    IndexOutOfRange("index %1 is out of range"),
    SingleChildOnlyOneNode("Only one node may be removed. Trying to remove %1 nodes"),
    OwnerAlreadyAttached("Attaching to an owner when it is already attached"),
    ParentOwnerMustMatchChild("Attaching to a different owner than parent"),
    OwnerAlreadyDetached("Detaching a node that is already detached"),
    IllegalMoveOperation("Moving %1 items from %2 to %3 is not legal"),
    CannotFindLayoutInParent("Parent layout does not contain this layout as a child"),
    ChildrenUnsupported("Draw does not have children");

    /*inline */fun validateState(check: Boolean) {
        if (!check) state(message)
    }

    /*inline */fun state(): Nothing = throw IllegalStateException(message)
    /*inline */fun state(vararg args: Any?): Nothing =
        throw IllegalStateException(message.format(args))

    /*inline */fun validateArg(check: Boolean, value: Any?) {
        if (!check) arg(value)
    }

    /*inline */fun validateArgs(check: Boolean, vararg values: Any?) {
        if (!check) arg(values)
    }

    /*inline */fun arg(): Nothing = throw IllegalArgumentException(message)
    /*inline */fun arg(vararg args: Any?): Nothing =
        throw IllegalArgumentException(message.format(args))

    /*inline */fun unsupported(): Nothing = throw UnsupportedOperationException(message)
}