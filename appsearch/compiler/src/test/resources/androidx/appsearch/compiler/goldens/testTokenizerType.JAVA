package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokNoneInvalid")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokPlainInvalid")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokVerbatimInvalid")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokRfc822Invalid")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokPlain")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokVerbatim")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokRfc822")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_RFC822)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokNonePrefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokPlainPrefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokVerbatimPrefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("tokRfc822Prefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_RFC822)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String tokNoneInvalidCopy = document.tokNoneInvalid;
    if (tokNoneInvalidCopy != null) {
      builder.setPropertyString("tokNoneInvalid", tokNoneInvalidCopy);
    }
    String tokPlainInvalidCopy = document.tokPlainInvalid;
    if (tokPlainInvalidCopy != null) {
      builder.setPropertyString("tokPlainInvalid", tokPlainInvalidCopy);
    }
    String tokVerbatimInvalidCopy = document.tokVerbatimInvalid;
    if (tokVerbatimInvalidCopy != null) {
      builder.setPropertyString("tokVerbatimInvalid", tokVerbatimInvalidCopy);
    }
    String tokRfc822InvalidCopy = document.tokRfc822Invalid;
    if (tokRfc822InvalidCopy != null) {
      builder.setPropertyString("tokRfc822Invalid", tokRfc822InvalidCopy);
    }
    String tokNoneCopy = document.tokNone;
    if (tokNoneCopy != null) {
      builder.setPropertyString("tokNone", tokNoneCopy);
    }
    String tokPlainCopy = document.tokPlain;
    if (tokPlainCopy != null) {
      builder.setPropertyString("tokPlain", tokPlainCopy);
    }
    String tokVerbatimCopy = document.tokVerbatim;
    if (tokVerbatimCopy != null) {
      builder.setPropertyString("tokVerbatim", tokVerbatimCopy);
    }
    String tokRfc822Copy = document.tokRfc822;
    if (tokRfc822Copy != null) {
      builder.setPropertyString("tokRfc822", tokRfc822Copy);
    }
    String tokNonePrefixCopy = document.tokNonePrefix;
    if (tokNonePrefixCopy != null) {
      builder.setPropertyString("tokNonePrefix", tokNonePrefixCopy);
    }
    String tokPlainPrefixCopy = document.tokPlainPrefix;
    if (tokPlainPrefixCopy != null) {
      builder.setPropertyString("tokPlainPrefix", tokPlainPrefixCopy);
    }
    String tokVerbatimPrefixCopy = document.tokVerbatimPrefix;
    if (tokVerbatimPrefixCopy != null) {
      builder.setPropertyString("tokVerbatimPrefix", tokVerbatimPrefixCopy);
    }
    String tokRfc822PrefixCopy = document.tokRfc822Prefix;
    if (tokRfc822PrefixCopy != null) {
      builder.setPropertyString("tokRfc822Prefix", tokRfc822PrefixCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    String[] tokNoneInvalidCopy = genericDoc.getPropertyStringArray("tokNoneInvalid");
    String tokNoneInvalidConv = null;
    if (tokNoneInvalidCopy != null && tokNoneInvalidCopy.length != 0) {
      tokNoneInvalidConv = tokNoneInvalidCopy[0];
    }
    String[] tokPlainInvalidCopy = genericDoc.getPropertyStringArray("tokPlainInvalid");
    String tokPlainInvalidConv = null;
    if (tokPlainInvalidCopy != null && tokPlainInvalidCopy.length != 0) {
      tokPlainInvalidConv = tokPlainInvalidCopy[0];
    }
    String[] tokVerbatimInvalidCopy = genericDoc.getPropertyStringArray("tokVerbatimInvalid");
    String tokVerbatimInvalidConv = null;
    if (tokVerbatimInvalidCopy != null && tokVerbatimInvalidCopy.length != 0) {
      tokVerbatimInvalidConv = tokVerbatimInvalidCopy[0];
    }
    String[] tokRfc822InvalidCopy = genericDoc.getPropertyStringArray("tokRfc822Invalid");
    String tokRfc822InvalidConv = null;
    if (tokRfc822InvalidCopy != null && tokRfc822InvalidCopy.length != 0) {
      tokRfc822InvalidConv = tokRfc822InvalidCopy[0];
    }
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
    String[] tokVerbatimCopy = genericDoc.getPropertyStringArray("tokVerbatim");
    String tokVerbatimConv = null;
    if (tokVerbatimCopy != null && tokVerbatimCopy.length != 0) {
      tokVerbatimConv = tokVerbatimCopy[0];
    }
    String[] tokRfc822Copy = genericDoc.getPropertyStringArray("tokRfc822");
    String tokRfc822Conv = null;
    if (tokRfc822Copy != null && tokRfc822Copy.length != 0) {
      tokRfc822Conv = tokRfc822Copy[0];
    }
    String[] tokNonePrefixCopy = genericDoc.getPropertyStringArray("tokNonePrefix");
    String tokNonePrefixConv = null;
    if (tokNonePrefixCopy != null && tokNonePrefixCopy.length != 0) {
      tokNonePrefixConv = tokNonePrefixCopy[0];
    }
    String[] tokPlainPrefixCopy = genericDoc.getPropertyStringArray("tokPlainPrefix");
    String tokPlainPrefixConv = null;
    if (tokPlainPrefixCopy != null && tokPlainPrefixCopy.length != 0) {
      tokPlainPrefixConv = tokPlainPrefixCopy[0];
    }
    String[] tokVerbatimPrefixCopy = genericDoc.getPropertyStringArray("tokVerbatimPrefix");
    String tokVerbatimPrefixConv = null;
    if (tokVerbatimPrefixCopy != null && tokVerbatimPrefixCopy.length != 0) {
      tokVerbatimPrefixConv = tokVerbatimPrefixCopy[0];
    }
    String[] tokRfc822PrefixCopy = genericDoc.getPropertyStringArray("tokRfc822Prefix");
    String tokRfc822PrefixConv = null;
    if (tokRfc822PrefixCopy != null && tokRfc822PrefixCopy.length != 0) {
      tokRfc822PrefixConv = tokRfc822PrefixCopy[0];
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.tokNoneInvalid = tokNoneInvalidConv;
    document.tokPlainInvalid = tokPlainInvalidConv;
    document.tokVerbatimInvalid = tokVerbatimInvalidConv;
    document.tokRfc822Invalid = tokRfc822InvalidConv;
    document.tokNone = tokNoneConv;
    document.tokPlain = tokPlainConv;
    document.tokVerbatim = tokVerbatimConv;
    document.tokRfc822 = tokRfc822Conv;
    document.tokNonePrefix = tokNonePrefixConv;
    document.tokPlainPrefix = tokPlainPrefixConv;
    document.tokVerbatimPrefix = tokVerbatimPrefixConv;
    document.tokRfc822Prefix = tokRfc822PrefixConv;
    return document;
  }
}
