# Ink Brush Module

This module contains logic for configuring the style of Ink's freehand strokes.
The precise style of a stroke is defined by `Brush`, which specifies a size,
color, epsilon (minimum unit considered visually distinguishable), and
`BrushFamily`. `BrushFamily` (analogous to font family) defines a general style
of stroke that can be in a variety of specific colors and sizes. The
experimental custom brush API brings the full flexibility of Ink's core
cross-platform implementation to Jetpack, allowing for configurable shapes,
textures, and dynamic behaviors. This lets Ink mimic strokes drawn by beautiful
and unique drawing tools (pencils, pens, markers, highlighters, brushes, and
more) but also far more unusual possibilities (washi tape, a laser pointer,
rainbows, a trail of clouds, the sky's the limit).
