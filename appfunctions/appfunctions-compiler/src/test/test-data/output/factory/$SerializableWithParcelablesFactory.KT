package com.testdata

import android.app.PendingIntent
import android.os.Bundle
import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableWithParcelablesFactory` : AppFunctionSerializableFactory<SerializableWithParcelables> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): SerializableWithParcelables {
    val appFunctionDataWithSpec = getAppFunctionDataWithSpec(appFunctionData = appFunctionData, qualifiedName = "com.testdata.SerializableWithParcelables")

    val pendingIntent = checkNotNull(appFunctionDataWithSpec.getParcelable("pendingIntent", PendingIntent::class.java))
    val pendingIntentList = checkNotNull(appFunctionDataWithSpec.getParcelableList("pendingIntentList", PendingIntent::class.java))
    val bundle = checkNotNull(appFunctionDataWithSpec.getParcelable("bundle", Bundle::class.java))
    val bundleList = checkNotNull(appFunctionDataWithSpec.getParcelableList("bundleList", Bundle::class.java))

    val resultSerializableWithParcelables = SerializableWithParcelables(pendingIntent, pendingIntentList, bundle, bundleList)
    return resultSerializableWithParcelables
  }

  override fun toAppFunctionData(appFunctionSerializable: SerializableWithParcelables): AppFunctionData {
    val serializableWithParcelables_appFunctionSerializable = appFunctionSerializable

    val builder = getAppFunctionDataBuilder("com.testdata.SerializableWithParcelables")
    val pendingIntent = serializableWithParcelables_appFunctionSerializable.pendingIntent
    builder.setParcelable("pendingIntent", pendingIntent)
    val pendingIntentList = serializableWithParcelables_appFunctionSerializable.pendingIntentList
    builder.setParcelableList("pendingIntentList", pendingIntentList)
    val bundle = serializableWithParcelables_appFunctionSerializable.bundle
    builder.setParcelable("bundle", bundle)
    val bundleList = serializableWithParcelables_appFunctionSerializable.bundleList
    builder.setParcelableList("bundleList", bundleList)

    return builder.build()
  }
}
