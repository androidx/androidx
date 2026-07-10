# Ink Storage Module

Logic for compactly serializing and deserializing Ink brushes and strokes. The
at-rest format is gzip-compressed binary-protobuf. The underlying protobuf
messages use a compact representation for stroke geometry, and the message
format is defined in the `.proto` files published
[here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:ink/ink-storage/src/commonMain/proto/).

Currently, this supports storage of `BrushFamily` and `StrokeInputBatch`
objects.

Cross-platform implementation supports Android and non-Android JVM (supported on
Linux for 86_64 specifically). iOS support via Kotlin-native is work in
progress.
