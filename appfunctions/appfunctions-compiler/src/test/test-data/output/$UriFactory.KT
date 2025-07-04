package androidx.appfunctions.`internal`.serializableproxies

import android.net.Uri
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$UriFactory` : AppFunctionSerializableFactory<Uri> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): Uri {

    val uriString = checkNotNull(appFunctionData.getStringOrNull("uriString"))

    val resultAppFunctionUri = AppFunctionUri(uriString)
    return resultAppFunctionUri.toUri()
  }

  override fun toAppFunctionData(appFunctionSerializable: Uri): AppFunctionData {
    val appFunctionUri_appFunctionSerializable = AppFunctionUri.fromUri(appFunctionSerializable)

    val builder = AppFunctionData.Builder("androidx.appfunctions.internal.serializableproxies.AppFunctionUri")
    val uriString = appFunctionUri_appFunctionSerializable.uriString
    builder.setString("uriString", uriString)

    return builder.build()
  }
}
