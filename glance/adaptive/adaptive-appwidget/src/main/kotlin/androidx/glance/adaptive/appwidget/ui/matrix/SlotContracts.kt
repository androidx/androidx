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

package androidx.glance.adaptive.appwidget.ui.matrix

/*
 * Slot component contracts of the Information Matrix.
 *
 * Every slot of a matrix accepts only components that implement its contract, so a component can
 * be placed in a slot only if it was built for it: a graph can go in the hero slot but not in an
 * action slot, and the compiler rejects anything else. A component may implement several contracts
 * when it suits several slots, as text does.
 *
 * The contracts are backend agnostic. Components are plain data, and each backend's matrix
 * renderer draws them. The contracts are sealed, so a backend renderer stops compiling until it
 * draws a newly added component, instead of failing at runtime.
 */

/**
 * What a component adds to a widget.
 *
 * A widget's capabilities are those of the components in its slots, so a template rule can ask for
 * "something that tracks" rather than for a particular component.
 */
internal enum class Capability {
    /** Shows a value or a label as text. */
    Text,

    /** Shows progress towards a goal. */
    Tracking,

    /** Shows how a value changed over time. */
    History,

    /** Shows a state at a glance. */
    Status,
}

/** A component that can be placed in a slot of the Information Matrix. */
internal sealed interface SlotComponent {
    /** What this component adds to the widget it is placed in. */
    val capabilities: Set<Capability>
}

/**
 * Contract of the header's primary slot: the headline, such as a metric.
 *
 * The matrix fits the primary component into the width the rest of the header leaves, so a new
 * component is fitted there as well as drawn by each backend.
 */
internal sealed interface PrimaryComponent : SlotComponent

/**
 * Contract of the header's secondary slot: the item after or beneath the primary one, such as a
 * unit label. The matrix deducts its width from the primary slot's budget.
 */
internal sealed interface SecondaryComponent : SlotComponent

/**
 * Contract of the header's leading slot: a square visual before the primary one, such as a progress
 * ring or an icon. The matrix decides its size (see [MatrixDefaults.LeadingSize]).
 */
internal sealed interface LeadingComponent : SlotComponent

/**
 * Contract of the hero slot: the card's main visual, such as a graph.
 *
 * The hero takes whatever height the other slots leave.
 */
internal sealed interface HeroComponent : SlotComponent

/**
 * Contract of the action row slots: a compact item in the bottom row, such as a supporting value, a
 * badge or a button.
 */
internal sealed interface ActionComponent : SlotComponent

/**
 * Contract of the bulb's single slot: a component that is the whole widget, centered, sized from
 * the container it is given.
 */
internal sealed interface BulbComponent : SlotComponent
