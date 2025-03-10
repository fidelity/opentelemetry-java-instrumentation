/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.cassandra.v3_0;

import com.datastax.driver.core.ExecutionInfo;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.SqlClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.network.NetworkAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.semconv.SemanticAttributes;

public final class CassandraSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.cassandra-3.0";

  // could use RESPONSE "ResultSet" here, but using RESPONSE "ExecutionInfo" in cassandra-4.0
  // instrumentation (see comment over there for why), so also using here for consistency
  private static final Instrumenter<CassandraRequest, ExecutionInfo> INSTRUMENTER;

  static {
    CassandraSqlAttributesGetter attributesGetter = new CassandraSqlAttributesGetter();

    INSTRUMENTER =
        Instrumenter.<CassandraRequest, ExecutionInfo>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                DbClientSpanNameExtractor.create(attributesGetter))
            .addAttributesExtractor(
                SqlClientAttributesExtractor.builder(attributesGetter)
                    .setTableAttribute(SemanticAttributes.DB_CASSANDRA_TABLE)
                    .setStatementSanitizationEnabled(
                        CommonConfig.get().isStatementSanitizationEnabled())
                    .build())
            .addAttributesExtractor(
                NetworkAttributesExtractor.create(new CassandraNetworkAttributesGetter()))
            .buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  public static Instrumenter<CassandraRequest, ExecutionInfo> instrumenter() {
    return INSTRUMENTER;
  }

  private CassandraSingletons() {}
}
