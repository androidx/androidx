# Ink Rendering Module

This module provides logic for rendering freehand strokes constructed with the
`strokes` module. Currently, there is a well-supported `android` submodule which
uses Android platform rendering APIs.

There is also an experimental `metal` submodule in alpha release which can be
used to do rendering on iOS using Kotlin Multiplatform. Note that the testing
for the Metal renderer is mostly upstream, due to lacking infrastructure for iOS
image-diff testing in Jetpack build/CI.
