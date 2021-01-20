package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexExact")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexPrefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String indexNoneCopy = dataClass.indexNone;
    if (indexNoneCopy != null) {
      builder.setPropertyString("indexNone", indexNoneCopy);
    }
    String indexExactCopy = dataClass.indexExact;
    if (indexExactCopy != null) {
      builder.setPropertyString("indexExact", indexExactCopy);
    }
    String indexPrefixCopy = dataClass.indexPrefix;
    if (indexPrefixCopy != null) {
      builder.setPropertyString("indexPrefix", indexPrefixCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String[] indexNoneCopy = genericDoc.getPropertyStringArray("indexNone");
    String indexNoneConv = null;
    if (indexNoneCopy != null && indexNoneCopy.length != 0) {
      indexNoneConv = indexNoneCopy[0];
    }
    String[] indexExactCopy = genericDoc.getPropertyStringArray("indexExact");
    String indexExactConv = null;
    if (indexExactCopy != null && indexExactCopy.length != 0) {
      indexExactConv = indexExactCopy[0];
    }
    String[] indexPrefixCopy = genericDoc.getPropertyStringArray("indexPrefix");
    String indexPrefixConv = null;
    if (indexPrefixCopy != null && indexPrefixCopy.length != 0) {
      indexPrefixConv = indexPrefixCopy[0];
    }
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.indexNone = indexNoneConv;
    dataClass.indexExact = indexExactConv;
    dataClass.indexPrefix = indexPrefixConv;
    return dataClass;
  }
}
