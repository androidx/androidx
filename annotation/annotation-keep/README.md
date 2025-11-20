# androidx.annotation-keep

Annotation library with annotations targeted at informing code shrinkers
about parts of the program that are used either externally from the program
itself or internally via reflection and therefore must be kept.

The annotations will over time align with those developed as part of R8, see
https://r8.googlesource.com/r8/+/refs/heads/main/src/keepanno/java/androidx/annotation/keep
for the source.

This repository should eventually be populated/updated using the following script
https://r8.googlesource.com/r8/+/refs/heads/main/tools/update-androidx-keep-annotations.py

The user guide on using the existing R8 annotations is:
https://r8.googlesource.com/r8/+/refs/heads/main/doc/keepanno-guide.md

The API docs are available for the existing R8 annotations:
https://r8.googlesource.com/r8/+/refs/heads/main/src/keepanno/java/androidx/annotation/keep
