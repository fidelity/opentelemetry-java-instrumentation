/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jedis.v3_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Arrays.asList;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class JedisInstrumentationModule extends InstrumentationModule {

  public JedisInstrumentationModule() {
    super("jedis", "jedis-3.0");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // missing in 2.x
    return hasClassesNamed("redis.clients.jedis.commands.BasicCommands")
        // added in 4.0
        .and(not(hasClassesNamed("redis.clients.jedis.CommandArguments")));
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new JedisConnectionInstrumentation(), new JedisInstrumentation());
  }

  @Override
  public boolean isIndyModule() {
    // java.lang.NoClassDefFoundError:
    //      io/opentelemetry/javaagent/instrumentation/jedis/JedisRequestContext
    //  at redis.clients.jedis.BinaryJedis.flushAll(BinaryJedis.java:595)
    //  at io.opentelemetry.javaagent.instrumentation.jedis.v3_0
    //       .Jedis30ClientTest.setup(Jedis30ClientTest.java:50)
    return false;
  }
}
