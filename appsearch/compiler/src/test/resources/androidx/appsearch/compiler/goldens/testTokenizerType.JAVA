package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  private static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokPlain")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_NAME);
    String tokNoneCopy = dataClass.tokNone;
    if (tokNoneCopy != null) {
      builder.setPropertyString("tokNone", tokNoneCopy);
    }
    String tokPlainCopy = dataClass.tokPlain;
    if (tokPlainCopy != null) {
      builder.setPropertyString("tokPlain", tokPlainCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String[] tokNoneCopy = genericDoc.getPropertyStringArray("tokNone");
    String tokNoneConv = null;
    if (tokNoneCopy != null && tokNoneCopy.length != 0) {
      tokNoneConv = tokNoneCopy[0];
    }
    String[] tokPlainCopy = genericDoc.getPropertyStringArray("tokPlain");
    String tokPlainConv = null;
    if (tokPlainCopy != null && tokPlainCopy.length != 0) {
      tokPlainConv = tokPlainCopy[0];
    }
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.tokNone = tokNoneConv;
    dataClass.tokPlain = tokPlainConv;
    return dataClass;
  }
}
