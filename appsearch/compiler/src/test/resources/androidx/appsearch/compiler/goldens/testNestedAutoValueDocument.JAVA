package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public class $$__AppSearch__AutoValue_Outer_Gift implements DocumentClassFactory<Outer.Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("property")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Outer.Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace(), document.id(), SCHEMA_NAME);
    String propertyCopy = document.property();
    if (propertyCopy != null) {
      builder.setPropertyString("property", propertyCopy);
    }
    return builder.build();
  }

  @Override
  public Outer.Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    String[] propertyCopy = genericDoc.getPropertyStringArray("property");
    String propertyConv = null;
    if (propertyCopy != null && propertyCopy.length != 0) {
      propertyConv = propertyCopy[0];
    }
    Outer.Gift document = Outer.Gift.create(idConv, namespaceConv, propertyConv);
    return document;
  }
}
