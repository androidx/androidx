# Layout overview

## Overview

The layout system work with **LayoutManager** that can layout **LayoutComponent** and **Component**.
LayoutComponents are able to be laid out as well as resized, while Components can only be laid out.

The following diagram shows the runtime class hiearchy:

<pre>
<div class="mermaid">
---
config:
  class:
    hideEmptyMembersBox: true
---
classDiagram
    Component <|-- RootLayoutComponent
    Component <|-- LayoutComponent
    LayoutComponent <|-- LayoutManager
    LayoutManager <|-- BoxLayout
    LayoutManager <|-- ColumnLayout
    LayoutManager <|-- RowLayout
    LayoutManager <|-- FitBoxLayout
    LayoutManager <|-- CoreText
    BoxLayout <|-- CanvasLayout
    ColumnLayout <|-- CollapsibleColumnLayout
    RowLayout <|-- CollapsibleRowLayout
    RowLayout <|-- FlowLayout
    LayoutManager <|-- ImageLayout
    LayoutManager <|-- StateLayout
</div>
</pre>

## Components

**Component** is the base class representing Origami Components.
                    
We create components when loading the document, out of the **ComponentStart**
and **ComponentEnd** operations in the original operations stream, grouping a list of operations:

<pre>
<div class="mermaid">
graph LR
    operations["`ComponentStart
    Operation 1
    ...
    Operation n
    ComponentEnd 3`"]
    operations --> Result

    Result
    subgraph Result [Runtime Component]
        Component --> OP1[Operation 1]
        Component --> OP2[...]
        Component --> OP3[Operation n]
    end
</div>
</pre>

## LayoutComponents

**LayoutComponent** represents layoutable components, which can be measured and may layout sub-components.
It keeps track of a list of modifiers as well as a list of child components.           

<pre>
<div class="mermaid">
graph TD
    LC[LayoutComponent]
    LC --> Mods[Modifiers]
    Mods --> M1[modifier 1]
    Mods --> M2[modifier 2]
    Mods --> M3[...]
    Mods --> MN[modifier n]
    
    LC --> Children[children]
    Children --> C1[component 1]
    Children --> C2[component 2]
    Children --> C3[...]
    Children --> CN[component n]
</div>
</pre>

### Modifiers

Modifiers are a list of operations that change the way a component can be displayed.

We have several modifiers that are related to the layout itself:

| Modifiers                | Role                      |
|--------------------------|---------------------------|
| WidthModifierOperation   | width as fixed dimension  |
| HeightModifierOperation  | height as fixed dimension |
| PaddingModifierOperation | padding information       |

As well as modifiers that are able to change the way the component paint itself:

| Modifiers                        | Role                               |
|----------------------------------|------------------------------------|
| BackgroundModifierOperation      | Draw a plain color background      |
| BorderModifierOperation          | Draw a border around the component |
| ClipRectModifierOperation        | Add a clipping rect                |
| RoundedClipRectModifierOperation | Add a clipping round rect          |

Modifiers are being applied in order, with Size and Padding impacting the draw-related
modifiers. Modifiers are doing a simple version of the layout pass (i.e. we don't ask modifiers
to measure and then layout them, we only layout them).

Padding modifiers can be used before or after the content modifiers, which means that we do not
need a "margin" concept as in other UI systems. Instead we can set up something like:

<pre>
<div class="mermaid">
graph LR
    P1[Padding 4] --> BG[Background]
    BG --> P2[Padding 8]
    P2 --> Content[/Content/]
</div>
</pre>

Which would result in a "margin" of 4 followed by painting the background, with a "padding" of 8
for the actual content.

#### Sizing

If a component is set to compute its own size (WRAP), Padding will be added to the computed size.

But to note, if a component uses a modifier for indicating a fixed dimension, such as:

<pre>
<div class="mermaid">
graph LR
    P1[Padding 4] --> S[Size 10x10]
    S --> P2[Padding 2]
</div>
</pre>

The computed size of the component for its parent layout manager will be 14x14.


#### Borders

Borders are always applied on top of the content, so putting the border before a background or after would result in the same visuals:


<pre>
<div class="mermaid">
graph LR
    B1[Border] --> BG1[Background]
    BG1 == "visual equivalent" ==> BG2[Background]
    BG2 --> B2[Border]
</div>
</pre>

Borders are purely a visual decoration, they do not impact sizing/padding, but they are impacted
by them, so have to be evaluated in order.


            
## Layout Managers

The actual layout managers are implemented as subclass of LayoutManager:

| Layout Manager          | Role                                              |
|-------------------------|---------------------------------------------------|
| BoxLayout               | Layout elements in the same place                 |
| RowLayout               | Layout elements in a row                          |
| ColumnLayout            | Layout elements in a column                       |
| CollapsibleRowLayout    | as RowLayout, but skip elements that don't fit    |
| CollapsibleColumnLayout | as ColumnLayout, but skip elements that don't fit |
| FitBoxLayout		          | pick the first child that fits                    |
| FlowLayout              | as RowLayout, but wrap elements if they don't fit |

### Scroll areas

Scroll areas are handled as subclasses of LayoutContent:

<pre>
<div class="mermaid">
graph TD
    SCH[ScrollContentHost]
    SCH --> SC[ScrollContent]
    SCH --> LC[LayoutContent]
</div>
</pre>

## Layout / Measure cycle

For a runtime document, we end up with the following structure for the layout operations:

<pre>
<div class="mermaid">
graph TD
    RLC[RootLayoutComponent]
    RLC --> LM1[LayoutManager]
    LM1 --> LM2[LayoutManager]
    LM2 --> C1[Component]
    LM2 --> C2[Component]
    LM1 --> C3[Component]
</div>
</pre>

The general layout / measure cycle works as follow:
 - for each LayoutComponent, measure their children recursively
 - capture that measure in a MeasurePass object
 - then recursively layout the components by using the MeasurePass information.

<pre>
<div class="mermaid">
graph TD
    M[Measure]
    M -- "ideally single pass, using intrinsic sizes (multi-measure is discouraged, but allowed)" --> M
    M --> MP[MeasurePass]
    MP -- "contains position+size of all the components" --> L[Layout]
    L -- "we can animate upon receiving a new layout(measurePass) request " --> A[Animate]
</div>
</pre>

1. RootLayoutComponent.layout(width, height) will measure all its children (Components), and gather
   the result of the measure in a MeasurePass object. The MeasurePass is then used to layout
   the components. The measure on its own never change the components, it's only role is to
   gather the resulting dimensions / positions.

2. Components implements the Measurable interface.

3. The measure pass is ideally done as a single pass, asking each child to measure itself
   given min/max constraints for width and height: the contract for the child is to enforce
   those min/max constraints and set a size that is within those.
   
4. By default, when a component receive a new layout with a measure information, we animate
   the change.
   
## Measure
   
Measure is done by asking components to add a ComponentMeasure to MeasurePass;
ComponentMeasure contains the position, dimension and visibility of the component.
Each component has an implicit ID that we use to map a given ComponentMeasure to a component.

The measure itself consists in passing a set of (min, max) for width and height to the component.

As part of the measurement, the list of modifiers may need to be evaluated (in order), to take in account
dimension modifiers (size, padding..). 

### Multi-measure

In order to limit the needs for multi-measures, Measurable components have the concept
of intrinsic sizes (min & max) -- they should be handled as equivalent to a measure query,
but should be cached (or even pre-cached) by the component.

Layout managers then can ask each child to measure themselves given min/max constraints,
or if they need to, ask the child for their intrinsic sizes, then measure them.

Layout managers can call measure multiple times on children (i.e. there is
nothing preventing multi-measure) but this should be avoided as possible, given the
potential performance impact (re-measuring a layout high in the hierarchy has the potential
for a large performance hit).

A component can indicate that its size/content changed and thus a layout pass is needed
by calling invalidateMeasure(); this will flag the chain of component until the root
and trigger the layout pass.     

## Request layout / Request measure

Generally, the layout system will react to change in dimensions, propagated from the root.
Another important mechanism is the ability for components to invalidate themselves and trigger
a relayout/remeasure pass.

Components can do that simply by calling `invalidateMeasure()` -- this will mark themselves
as needing both a repaint and a remeasure, and will walk the tree up until the root component,
invalidating them as the tree is traversed. The root component then uses this to trigger a measure pass.

