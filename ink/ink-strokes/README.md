# Ink Strokes Module

This module provides logic for constructing and representing freehand strokes.
`InProgressStroke` is used to construct strokes and represent partial strokes
while pointer input is in progress. `Stroke` is used to represent finished
strokes.

Cross-platform implementation supports Android and non-Android JVM (supported on
Linux for 86_64 specifically). iOS support via Kotlin-native is work in
progress.
