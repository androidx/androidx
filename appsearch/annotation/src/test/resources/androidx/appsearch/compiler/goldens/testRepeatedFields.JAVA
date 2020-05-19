package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyConfig;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.TreeSet;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("listOfString")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("setOfInt")
      .setDataType(PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("repeatedByteArray")
      .setDataType(PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("byteArray")
      .setDataType(PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    List<String> listOfStringCopy = dataClass.listOfString;
    if (listOfStringCopy != null) {
      String[] listOfStringConv = listOfStringCopy.toArray(new String[0]);
      builder.setProperty("listOfString", listOfStringConv);
    }
    TreeSet<Integer> setOfIntCopy = dataClass.setOfInt;
    if (setOfIntCopy != null) {
      long[] setOfIntConv = new long[setOfIntCopy.size()];
      int i = 0;
      for (Integer item : setOfIntCopy) {
        setOfIntConv[i++] = item;
      }
      builder.setProperty("setOfInt", setOfIntConv);
    }
    byte[][] repeatedByteArrayCopy = dataClass.repeatedByteArray;
    if (repeatedByteArrayCopy != null) {
      builder.setProperty("repeatedByteArray", repeatedByteArrayCopy);
    }
    byte[] byteArrayCopy = dataClass.byteArray;
    if (byteArrayCopy != null) {
      builder.setProperty("byteArray", byteArrayCopy);
    }
    return builder.build();
  }
}
