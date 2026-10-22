# Module root

Web Compose

# Package androidx.web.compose

Jetpack Compose integration for `androidx.web` that displays and manages web content in Compose UIs.

This package provides composables and helpers to present [WebContent][androidx.web.WebContent] in a Compose hierarchy and retain web session state across configuration changes.

Key components include:
*   [WebSurface]: Composable that presents the web page hosted by [WebContent][androidx.web.WebContent].
*   [retainWebContent]: Composable that creates and retains a [WebContent][androidx.web.WebContent] instance across configuration changes and transient compositions, automatically closing it when retired.
*   [WebViewBridge]: Specialized [WebContentView][androidx.web.WebContentView] that exposes [WebView][android.webkit.WebView] APIs not yet available directly on [WebContent][androidx.web.WebContent] when presenting via [WebSurface].
*   [WebChromeClientBridge]: Specialized [WebChromeClient][android.webkit.WebChromeClient] that exposes [WebChromeClient][android.webkit.WebChromeClient] callbacks not yet available directly on [WebContent][androidx.web.WebContent] when presenting via [WebSurface].
