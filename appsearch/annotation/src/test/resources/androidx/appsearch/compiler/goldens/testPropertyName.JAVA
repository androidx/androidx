package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.PropertyConfig;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("newName")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();
}
