/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet;

import static java.util.Collections.emptyList;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public class ServletRequestParametersExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<
        ServletRequestContext<REQUEST>, ServletResponseContext<RESPONSE>> {
  private static final List<String> CAPTURE_REQUEST_PARAMETERS =
      InstrumentationConfig.get()
          .getList(
              "otel.instrumentation.servlet.experimental.capture-request-parameters", emptyList());

  private static final ConcurrentMap<String, AttributeKey<List<String>>>
      oldSemconvParameterKeysCache = new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, AttributeKey<List<String>>>
      stableSemconvParameterKeysCache = new ConcurrentHashMap<>();

  private final ServletAccessor<REQUEST, RESPONSE> accessor;

  public ServletRequestParametersExtractor(ServletAccessor<REQUEST, RESPONSE> accessor) {
    this.accessor = accessor;
  }

  public static boolean enabled() {
    return !CAPTURE_REQUEST_PARAMETERS.isEmpty();
  }

  public void setAttributes(
      REQUEST request, BiConsumer<AttributeKey<List<String>>, List<String>> consumer) {
    for (String name : CAPTURE_REQUEST_PARAMETERS) {
      List<String> values = accessor.getRequestParameterValues(request, name);
      if (!values.isEmpty()) {
        if (SemconvStability.emitOldHttpSemconv()) {
          consumer.accept(oldSemconvParameterAttributeKey(name), values);
        }
        if (SemconvStability.emitStableHttpSemconv()) {
          consumer.accept(stableSemconvParameterAttributeKey(name), values);
        }
      }
    }
  }

  @Override
  public void onStart(
      AttributesBuilder attributes,
      Context parentContext,
      ServletRequestContext<REQUEST> requestContext) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      ServletRequestContext<REQUEST> requestContext,
      @Nullable ServletResponseContext<RESPONSE> responseContext,
      @Nullable Throwable error) {
    // request parameters are extracted at the end of the request to make sure that we don't access
    // them before request encoding has been set
    REQUEST request = requestContext.request();
    setAttributes(request, attributes::put);
  }

  private static AttributeKey<List<String>> oldSemconvParameterAttributeKey(String headerName) {
    return oldSemconvParameterKeysCache.computeIfAbsent(
        headerName, ServletRequestParametersExtractor::createOldSemconvKey);
  }

  private static AttributeKey<List<String>> stableSemconvParameterAttributeKey(String headerName) {
    return stableSemconvParameterKeysCache.computeIfAbsent(
        headerName, ServletRequestParametersExtractor::createStableSemconvKey);
  }

  private static AttributeKey<List<String>> createOldSemconvKey(String parameterName) {
    // normalize parameter name similarly as is done with header names when header values are
    // captured as span attributes
    parameterName = parameterName.toLowerCase(Locale.ROOT);
    String key = "servlet.request.parameter." + parameterName.replace('-', '_');
    return AttributeKey.stringArrayKey(key);
  }

  private static AttributeKey<List<String>> createStableSemconvKey(String parameterName) {
    // normalize parameter name similarly as is done with header names when header values are
    // captured as span attributes
    parameterName = parameterName.toLowerCase(Locale.ROOT);
    String key = "servlet.request.parameter." + parameterName;
    return AttributeKey.stringArrayKey(key);
  }
}
