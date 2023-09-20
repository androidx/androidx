package com.example.appsearch;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchDocumentClassMap;
import com.google.auto.service.AutoService;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@AutoService(AppSearchDocumentClassMap.class)
@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__DocumentClassMap_71ecb22a3f48746c4261bd34b4ba3c2861632443de7f7c577156ec583523e7f6_1 extends AppSearchDocumentClassMap {
  @NonNull
  @Override
  protected Map<String, List<String>> getMap() {
    Map<String, List<String>> result = new HashMap<>();
    result.put("Gift", Arrays.asList("com.example.appsearch.Gift"));
    return result;
  }
}
