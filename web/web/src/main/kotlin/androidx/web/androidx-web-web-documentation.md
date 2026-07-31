# Module root

Web

# Package androidx.web

Web is a collection of modern APIs that can be used with android.webkit and androidx.webkit.

This package provides components to manage WebView state independently of the view hierarchy, allowing WebView state to outlive
configuration changes.

Key components include:
*   `WebContent`: Represents a persistent web engine session state that can safely outlive individual UI views or Activities.
*   `WebContentView`: A specialized `WebView` designed to integrate with `WebContent`.
*   `DetachedWebContentView`: A specialized `WebContentView` that `WebContent` is attached to when not attached to a particular Activity.
*   `WebFeature`: Utility for checking the runtime availability of newer Web APIs.