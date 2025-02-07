package com.example.appsearch;

import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassMappingContext;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("name")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("collectBlob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("arrBlob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String nameCopy = document.name;
    if (nameCopy != null) {
      builder.setPropertyString("name", nameCopy);
    }
    AppSearchBlobHandle blobCopy = document.blob;
    if (blobCopy != null) {
      builder.setPropertyBlobHandle("blob", blobCopy);
    }
    Collection<AppSearchBlobHandle> collectBlobCopy = document.collectBlob;
    if (collectBlobCopy != null) {
      AppSearchBlobHandle[] collectBlobConv = collectBlobCopy.toArray(new AppSearchBlobHandle[0]);
      builder.setPropertyBlobHandle("collectBlob", collectBlobConv);
    }
    AppSearchBlobHandle[] arrBlobCopy = document.arrBlob;
    if (arrBlobCopy != null) {
      builder.setPropertyBlobHandle("arrBlob", arrBlobCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      DocumentClassMappingContext documentClassMappingContext) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    String[] nameCopy = genericDoc.getPropertyStringArray("name");
    String nameConv = null;
    if (nameCopy != null && nameCopy.length != 0) {
      nameConv = nameCopy[0];
    }
    AppSearchBlobHandle[] blobCopy = genericDoc.getPropertyBlobHandleArray("blob");
    AppSearchBlobHandle blobConv = null;
    if (blobCopy != null && blobCopy.length != 0) {
      blobConv = blobCopy[0];
    }
    AppSearchBlobHandle[] collectBlobCopy = genericDoc.getPropertyBlobHandleArray("collectBlob");
    List<AppSearchBlobHandle> collectBlobConv = null;
    if (collectBlobCopy != null) {
      collectBlobConv = Arrays.asList(collectBlobCopy);
    }
    AppSearchBlobHandle[] arrBlobConv = genericDoc.getPropertyBlobHandleArray("arrBlob");
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.name = nameConv;
    document.blob = blobConv;
    document.collectBlob = collectBlobConv;
    document.arrBlob = arrBlobConv;
    return document;
  }
}
