# Module root

Web

# Package androidx.web

Web is a collection of modern APIs that can be used with `android.webkit`, `androidx.webkit`, and `androidx.web.compose`.

This package provides components to manage [WebView][android.webkit.WebView] state independently of the view hierarchy, allowing [WebView][android.webkit.WebView] state to outlive configuration changes.

Key components include:
*   [WebContent]: Persistent web engine session state that can safely outlive individual UI views or [Activities][android.app.Activity].
*   [WebContentView]: Specialized [WebView][android.webkit.WebView] designed to integrate with [WebContent].
*   [DetachedWebContentView]: Specialized [WebContentView] that [WebContent] attaches to when not attached to an [Activity][android.app.Activity].
*   [WebFeature]: Utility for checking the runtime availability of newer Web APIs.
