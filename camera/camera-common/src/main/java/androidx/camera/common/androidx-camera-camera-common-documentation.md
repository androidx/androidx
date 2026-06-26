# Module root

androidx.camera.common camera-common

# Package androidx.camera.common

CameraX Common is a collection of well documented common types, interfaces, wrappers, math and utilities designed to enable library and application developers to build on a shared set of types regardless of CameraX or Camera2 usage. This library is intentionally designed to be lightweight to ensure that libraries or applications that depend on it have minimal impact to their library and code size.

Library developers that can consume media content from a camera are encouraged to use these interfaces and types as part of their permanent public API surface instead of library-specific subtypes when possible. Developers that expose or develop camera libraries or applications that utilize Camera2 directly are encouraged to depend on and supply instances of these public API types directly.
