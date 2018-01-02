package org.chromium.support_lib_boundary;

import java.lang.reflect.InvocationHandler;

public interface WebKitTypeAsMethodParameterBoundaryInterface {
    void method(InvocationHandler webViewClient);

    void methodX(InvocationHandler response);

    void urlCall(String url);
}
