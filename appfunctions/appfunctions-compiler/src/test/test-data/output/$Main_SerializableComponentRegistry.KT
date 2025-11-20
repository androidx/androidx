package appfunctions_aggregated_deps

import androidx.appfunctions.AppFunctionComponentRegistry
import javax.`annotation`.processing.Generated

@AppFunctionComponentRegistry(
  componentCategory = "SERIALIZABLE",
  componentNames = [
    "com.testdata.DerivedInputToFakeFreeFormFunction",
    "com.testdata.DerivedInputToFakeFreeFormFunction#anotherData",
    "com.testdata.DerivedInputToFakeFreeFormFunction#data",
    "com.testdata.InputToFakeFreeFormFunction",
    "com.testdata.InputToFakeFreeFormFunction#data",
    "com.testdata.InputWithParametrizedTypes",
    "com.testdata.InputWithParametrizedTypes#parametrizedInt",
    "com.testdata.InputWithParametrizedTypes#parametrizedNullableInt",
    "com.testdata.InputWithParametrizedTypes#parametrizedNullableSerializable",
    "com.testdata.InputWithParametrizedTypes#parametrizedSerializable",
    "com.testdata.InputWithRepeatedTypes",
    "com.testdata.InputWithRepeatedTypes#intArray",
    "com.testdata.InputWithRepeatedTypes#serializableList",
    "com.testdata.InputWithRepeatedTypes#stringList",
    "com.testdata.NotDescribedByKdoc",
    "com.testdata.NotDescribedByKdoc#param",
    "com.testdata.Optional",
    "com.testdata.Optional#value",
  ],
  componentDocStrings = [
    "A child class of [InputToFakeFreeFormFunction]. ",
    "An int param. ",
    "A derived string param. ",
    "A test input class to the free-form function. ",
    "A string param. ",
    "An input class with parametrized types ",
    "Parametrized primitive param. ",
    "Parametrized nullable primitive param. ",
    "Parametrized nullable entity param. ",
    "Parametrized entity param. ",
    "An input class with repeated params. ",
    "Primitive array param. ",
    "Serializable list param. ",
    "String list param. ",
    "",
    "",
    "A generic serializable class. ",
    "The value. ",
  ],
)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$Main_SerializableComponentRegistry`
