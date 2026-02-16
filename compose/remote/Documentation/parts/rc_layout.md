# Layout overview

## Overview

The layout system work with **LayoutManager** that can layout **LayoutComponent** and **Component**.
LayoutComponents are able to be laid out as well as resized, while Components can only be laid out.

The following diagram shows the runtime class hiearchy:

****************************************************************************************************
*                                        .---------.
*                                       | Component |
*                                        '----+----'                                                    
*                                             |            .-------------------.
*                                             |'----------| RootLayoutComponent |
*                                             |            '-------------------'
*                                     .-------+-------.   
*                                    | LayoutComponent |
*                                     '-------+-------'                                                   
*                                             |
*                                      .------+------.
*                                     | LayoutManager |
*                                      '------+------'                           
*                                             |
*       .--------------+---------------+------+------------------.
*      |               |               |                          |
*  .---+-----.   .-----+------.   .----+----.                     |    .--------------.  
* | BoxLayout | | ColumnLayout | | RowLayout |                    |'--| ImageComponent |
*  '---------'   '-----+------'   '----+----'                     |    '--------------'                                                    
*                      |               |                          |    .------------.
*       .--------------+--------.   .--+-----------------.        |'--| CanvasLayout |
*      | CollapsibleColumnLayout | | CollapsibleRowLayout |       |    '------------'
*       '-----------------------'   '--------------------'        |    .-----------.
*                                                                  '--| StateLayout |
*                                                                      '-----------'
****************************************************************************************************

The following runtime classes directly map to layout operations in the wire format:


           | Layout Operation | 
           |----|
           | **Component** |
           | **RootLayoutComponent** |
           | **BoxLayout** |
           | **ColumnLayout** |
           | **RowLayout** |
           | *CollapsibleColumnLayout* (1) |
           | *CollapsibleRowLayout* (1) |
           | *ImageComponent* (1) |
           | *CanvasLayout* (1) |
           | *StateLayout* (1) |
           | *LazyRow* (1) |
           | *LazyColumn* (1) |

(1) Work in Progress
## Components

**Component** is the base class representing Origami Components.
                    
We create components when loading the document, out of the **ComponentStart**
and **ComponentEnd** operations in the original operations stream, grouping a list of operations:
                   
****************************************************************************************************
*  .--------------.                 .---------.   
* | ComponentStart |               | Component |
*  '--------------'                 '--+------'
*    .---------.                       |   
*   | Operation |                      |'--> Operation  
*    '---------'         --->          |
*        ...                           |'--> ....
*    .---------.                       |
*   | Operation |                       '--> Operation
*    '---------'
*  .------------.
* | ComponentEnd |
*  '------------'
****************************************************************************************************

## LayoutComponents

**LayoutComponent** represents layoutable components, which can be measured and may layout sub-components.
It keeps track of a list of modifiers as well as a list of child components.           

****************************************************************************************************
*  .---------------.
* | LayoutComponent |
*  '-+-------------'                                                   
*    |      .---------.
*    |'--> | Modifiers | 
*    |      '---------'                      
*    |         |
*    |         |'--> modifier 1                                                
*    |         |
*    |         |'--> modifier 2                                                
*    |         |
*    |         |'--> ...                                                
*    |         |
*    |          '--> modifier n                                                
*    |      .--------.
*     '--> | children | 
*           '--------'                      
*             |
*             |'--> component 1                                                
*             |
*             |'--> component 2                                                
*             |
*             |'--> ...                                                
*             |
*              '--> component n                                                
****************************************************************************************************

### Modifiers

Modifiers are a list of operations that change the way a component can be displayed.

We have several modifiers that are related to the layout itself:

Modifiers                | Role
-------------------------|-------
WidthModifierOperation   | width as fixed dimension
HeightModifierOperation  | height as fixed dimension
PaddingModifierOperation | padding information

As well as modifiers that are able to change the way the component paint itself:

Modifiers    | Role
-------------|-------
BackgroundModifierOperation | Draw a plain color background
BorderModifierOperation | Draw a border around the component
ClipRectModifierOperation | Add a clipping rect
RoundedClipRectModifierOperation | Add a clipping round rect

Modifiers are being applied in order, with Size and Padding impacting the draw-related
modifiers. Modifiers are doing a simple version of the layout pass (i.e. we don't ask modifiers
to measure and then layout them, we only layout them).

Padding modifiers can be used before or after the content modifiers, which means that we do not
need a "margin" concept as in other UI systems. Instead we can set up something like:


****************************************************************************************************
*  .---------.      .----------.      .---------.        .---------.
* | Padding 4 |--> | Background |--> | Padding 8 | -->  / Content /
*  '---------'      '----------'      '---------'      '---------'
****************************************************************************************************

Which would result in a "margin" of 4 followed by painting the background, with a "padding" of 8
for the actual content.

#### Sizing

If a component is set to compute its own size (WRAP), Padding will be added to the computed size.

But to note, if a component uses a modifier for indicating a fixed dimension, such as:

****************************************************************************************************
*  .---------.      .----------.      .---------.
* | Padding 4 |--> | Size 10x10 |--> | Padding 2 |
*  '---------'      '----------'      '---------'
****************************************************************************************************

The computed size of the component for its parent layout manager will be 14x14.


#### Borders

Borders are always applied on top of the content, so putting the border before a background or after would result in the same visuals:


****************************************************************************************************
*  .------.      .----------.          .----------.      .------.
* | Border |--> | Background |   ==   | Background |--> | Border |
*  '------'      '----------'          '----------'      '------'
****************************************************************************************************

Borders are purely a visual decoration, they do not impact sizing/padding, but they are impacted
by them, so have to be evaluated in order.


            
## Layout Managers

The actual layout managers are implemented as subclass of LayoutManager:

Layout Manager          | Role
------------------------|-----
BoxLayout               | Layout elements in the same place
RowLayout               | Layout elements in a row
ColumnLayout            | Layout elements in a column
CollapsibleRowLayout    | as RowLayout, but skip elements that don't fit
CollapsibleColumnLayout | as ColumnLayout, but skip elements that don't fit
FitBoxLayout		| pick the first child that fits
FlowLayout              | as RowLayout, but wrap elements if they don't fit

### Scroll areas

Scroll areas are handled as subclasses of LayoutContent:

****************************************************************************************************
*  .-----------------.
* | ScrollContentHost |
*  '--+--------------'
*     |      .-------------.
*     |'--> | ScrollContent |
*     |      '-------------'
*     |      .-------------.
*      '--> | LayoutContent |
*            '-------------'
****************************************************************************************************

## Layout / Measure cycle

For a runtime document, we end up with the following structure for the layout operations:

****************************************************************************************************
*  .-------------------.
* | RootLayoutComponent |
*  '--+----------------'
*     |      .-------------.
*      '--> | LayoutManager |
*            '--+----------'
*               |      .-------------.
*               |'--> | LayoutManager |
*               |      '--+----------'
*               |         |      .---------.
*               |         |'--> | Component |
*               |         |      '---------'
*               |         |      .---------.
*               |          '--> | Component |
*               |                '---------'
*               |      .---------.
*                '--> | Component |
*                      '---------'
****************************************************************************************************

The general layout / measure cycle works as follow:
 - for each LayoutComponent, measure their children recursively
 - capture that measure in a MeasurePass object
 - then recursively layout the components by using the MeasurePass information.

****************************************************************************************************
*     .-------.
*    |         |<--.
*    | Measure |    | ideally single pass, using intrinsic sizes
*    |         +---'  (multi-measure is discouraged, but allowed)
*     '---+---'
*         | 
*         v
*   .-----------.
*  | MeasurePass |  contains position+size of all the components
*   '-----+-----'
*         |
*         v
*     .------.
*    | Layout |<----.   we can animate upon receiving a new layout(measurePass) request
*     '---+--'       |
*         |      .---+---.
*          '--> | Animate | 
*                '-------'
****************************************************************************************************

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

