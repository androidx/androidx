package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.PropertyConfig;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("repeatReq")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("repeatNoReq")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("req")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("noReq")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();
}
