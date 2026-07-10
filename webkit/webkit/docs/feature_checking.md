# How does the boundary interfaces and feature checking work

The Android system will use different classloaders to load the WebView and
application code into the app process. This prevents any collisions between
class names in the two apps. Furthermore, the WebView code is distributed as
part of the System WebView package, and is updated over the air, while the
Jetpack Webkit library is compiled into the individual apps that use WebView,
and is only updated when the apps in question decide to upgrade to the next
version of the library.

This means that WebView must support both older and *newer* versions of the
Jetpack Webkit library, since a device that has not received any WebView
updates may have an app installed that uses a later version of the library, or
an app that uses an older version of the library may be installed on a device
that has received the latest version of WebView. It also means that the Jetpack
Webkit library must support both older and newer versions of WebView being
installed on the device.

The Jetpack Webkit library communicates with the installed WebView
implementation through reflection. This reflection is supported by the use of
the boundary interfaces, which represent the shared public API between WebView
and the Jetpack Webkit library.

The boundary interfaces are used by both WebView and the Jetpack Webkit library
to instantiate [Proxy
objects](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html)
which wrap [InvocationHandler
objects](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/InvocationHandler.html)
that in turn wrap the actual implementations of the various objects. The
details of this is implemented in
[BoundaryInterfaceReflectionUtil.java](https://source.chromium.org/chromium/chromium/src/+/main:android_webview/support_library/boundary_interfaces/src/org/chromium/support_lib_boundary/util/BoundaryInterfaceReflectionUtil.java),
which itself is shared between WebView and the Jetpack Webkit library. This
Proxy/IncoxationHandler mechanism is necessary to bridge the ClassLoader gap,
but also allows for late binding, which means that it is possible for methods
to be missing or added, without this causing any compile time issues.

The boundary interfaces are authored in the [Chromium
repository](https://source.chromium.org/chromium/chromium/src/+/main:android_webview/support_library/boundary_interfaces/src/org/chromium/support_lib_boundary/),
and then copied to the Jetpack repository as a dependency that is pulled in
during checkout.

When creating a Proxy object, it will correspond to the currently available
version of a boundary interface. However, the wrapped InvocationHandler may be
based on an older or newer version of the same interface. To ensure that no
unsupported methods are called, both WebView and the Jetpack Webkit library
provide arrays of supported features. WebView provides this array centrally in
[SupportLibWebViewChromiumFactory](https://source.chromium.org/chromium/chromium/src/+/main:android_webview/support_library/java/src/org/chromium/support_lib_glue/SupportLibWebViewChromiumFactory.java?q=symbol%3A%5Cborg.chromium.support_lib_glue.SupportLibWebViewChromiumFactory.sWebViewSupportedFeatures%5Cb%20case%3Ayes).
Each feature constant corresponds to one or more methods that were added to the
boundary interfaces in the same commit. By checking whether a given feature
string is available in this array, the Webkit Jetpack library can determine if
it is safe to call a given method.

For any interfaces that are meant to be provided by the application, e.g.
callbacks, the boundary interface must extend
[FeatureFlagHolderBoundaryInterface](https://source.chromium.org/chromium/chromium/src/+/main:android_webview/support_library/boundary_interfaces/src/org/chromium/support_lib_boundary/FeatureFlagHolderBoundaryInterface.java),
which allows the individual objects to provide a list of features they support,
which allows the WebView implementation to feature detect on objects received
from the embedding application.
