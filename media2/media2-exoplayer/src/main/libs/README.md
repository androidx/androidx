This directory stores repackaged
[ExoPlayer](https://github.com/google/ExoPlayer) libraries for use by media2.

JARs are built from the ExoPlayer sources with the following modifications:
- The package `com.google.android.exoplayer2` is moved to
  `androidx.media2.exoplayer.external`.
- All classes are marked `@hide` and `@RestrictTo(Scope.LIBRARY_GROUP)`.
- `androidx.annotations` are used instead of `com.android.support.annotations`.
- The output JAR must be desugared.
