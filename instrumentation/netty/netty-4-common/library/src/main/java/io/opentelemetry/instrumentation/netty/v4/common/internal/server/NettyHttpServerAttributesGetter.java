/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4.common.internal.server;

import static io.opentelemetry.semconv.SemanticAttributes.NetTransportValues.IP_TCP;
import static io.opentelemetry.semconv.SemanticAttributes.NetTransportValues.IP_UDP;

import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.netty.v4.common.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4.common.internal.ChannelUtil;
import io.opentelemetry.instrumentation.netty.v4.common.internal.HttpSchemeUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import javax.annotation.Nullable;

final class NettyHttpServerAttributesGetter
    implements HttpServerAttributesGetter<HttpRequestAndChannel, HttpResponse> {

  @Override
  public String getHttpRequestMethod(HttpRequestAndChannel requestAndChannel) {
    return requestAndChannel.request().getMethod().name();
  }

  @Override
  public List<String> getHttpRequestHeader(HttpRequestAndChannel requestAndChannel, String name) {
    return requestAndChannel.request().headers().getAll(name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      HttpRequestAndChannel requestAndChannel, HttpResponse response, @Nullable Throwable error) {
    return response.getStatus().code();
  }

  @Override
  public List<String> getHttpResponseHeader(
      HttpRequestAndChannel requestAndChannel, HttpResponse response, String name) {
    return response.headers().getAll(name);
  }

  @Override
  public String getUrlScheme(HttpRequestAndChannel requestAndChannel) {
    return HttpSchemeUtil.getScheme(requestAndChannel);
  }

  @Override
  public String getUrlPath(HttpRequestAndChannel requestAndChannel) {
    String fullPath = requestAndChannel.request().getUri();
    int separatorPos = fullPath.indexOf('?');
    return separatorPos == -1 ? fullPath : fullPath.substring(0, separatorPos);
  }

  @Override
  public String getUrlQuery(HttpRequestAndChannel requestAndChannel) {
    String fullPath = requestAndChannel.request().getUri();
    int separatorPos = fullPath.indexOf('?');
    return separatorPos == -1 ? null : fullPath.substring(separatorPos + 1);
  }

  @Override
  public String getTransport(HttpRequestAndChannel requestAndChannel) {
    return requestAndChannel.channel() instanceof DatagramChannel ? IP_UDP : IP_TCP;
  }

  @Override
  public String getNetworkTransport(
      HttpRequestAndChannel requestAndChannel, HttpResponse response) {
    return ChannelUtil.getNetworkTransport(requestAndChannel.channel());
  }

  @Override
  public String getNetworkProtocolName(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    return requestAndChannel.request().getProtocolVersion().protocolName();
  }

  @Override
  public String getNetworkProtocolVersion(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    HttpVersion version = requestAndChannel.request().getProtocolVersion();
    if (version.minorVersion() == 0) {
      return Integer.toString(version.majorVersion());
    }
    return version.majorVersion() + "." + version.minorVersion();
  }

  @Override
  @Nullable
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    SocketAddress address = requestAndChannel.remoteAddress();
    if (address instanceof InetSocketAddress) {
      return (InetSocketAddress) address;
    }
    return null;
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkLocalInetSocketAddress(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    SocketAddress address = requestAndChannel.channel().localAddress();
    if (address instanceof InetSocketAddress) {
      return (InetSocketAddress) address;
    }
    return null;
  }
}
