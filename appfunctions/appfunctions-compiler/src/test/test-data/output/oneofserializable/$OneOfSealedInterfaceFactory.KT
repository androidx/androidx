package com.testdata.oneofserializable

import androidx.`annotation`.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.lang.IllegalArgumentException
import javax.`annotation`.processing.Generated

@RequiresApi(33)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$OneOfSealedInterfaceFactory` : AppFunctionSerializableFactory<OneOfSealedInterface> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): OneOfSealedInterface = when(appFunctionData.qualifiedName) {
    ASubclass::class.java.name ->  {
      val aSubclassFactory = `$ASubclassFactory`()
      aSubclassFactory.fromAppFunctionData(appFunctionData)}
    BSubclass::class.java.name ->  {
      val bSubclassFactory = `$BSubclassFactory`()
      bSubclassFactory.fromAppFunctionData(appFunctionData)}
    else -> throw IllegalArgumentException("""Unknown qualifiedName: ${appFunctionData.qualifiedName}""")
  }

  override fun toAppFunctionData(appFunctionSerializable: OneOfSealedInterface): AppFunctionData = when(appFunctionSerializable) {
    is ASubclass ->  {
      val aSubclassFactory = `$ASubclassFactory`()
      aSubclassFactory.toAppFunctionData(appFunctionSerializable)}
    is BSubclass ->  {
      val bSubclassFactory = `$BSubclassFactory`()
      bSubclassFactory.toAppFunctionData(appFunctionSerializable)}
  }
}
