package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("price")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.getUri(), SCHEMA_TYPE);
    builder.setProperty("price", dataClass.getPrice());
    return builder.build();
  }

  public Gift fromGenericDocument(GenericDocument genericDoc) {
    int priceConv = genericDoc.getPropertyLong("price");
    Gift dataClass = new Gift();
    dataClass.setPrice(priceConv);
    return dataClass;
  }
}
