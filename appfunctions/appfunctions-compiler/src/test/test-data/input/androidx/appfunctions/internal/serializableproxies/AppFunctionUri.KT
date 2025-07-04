package androidx.appfunctions.internal.serializableproxies

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.appfunctions.AppFunctionSerializableProxy

@RestrictTo(Scope.LIBRARY_GROUP)
@AppFunctionSerializableProxy(targetClass = Uri::class)
public data class AppFunctionUri(val uriString: String) {
    public fun toUri(): Uri {
        return Uri.parse(uriString)
    }

    public companion object {
        public fun fromUri(androidUri: Uri): AppFunctionUri {
            return AppFunctionUri(androidUri.toString())
        }
    }
}
