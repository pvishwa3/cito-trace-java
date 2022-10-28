package datadog.trace.instrumentation.undertow;

import datadog.trace.api.gateway.BlockResponseFunction;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstanceStore;
import datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.URIDataAdapter;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

public class UndertowDecorator
    extends HttpServerDecorator<
        HttpServerExchange, HttpServerExchange, HttpServerExchange, HttpServerExchange> {
  public static final CharSequence UNDERTOW_REQUEST =
      UTF8BytesString.create("undertow-http.request");
  public static final CharSequence SERVLET_REQUEST = UTF8BytesString.create("servlet.request");
  public static final CharSequence UNDERTOW_HTTP_SERVER =
      UTF8BytesString.create("undertow-http-server");

  @SuppressWarnings("rawtypes")
  private static final ContextStore<String, AttachmentKey> attachmentStore =
      InstanceStore.of(AttachmentKey.class);

  @SuppressWarnings("unchecked")
  public static final AttachmentKey<Boolean> DD_HTTPSERVEREXCHANGE_DISPATCH =
      attachmentStore.putIfAbsent(
          "DD_HTTPSERVEREXCHANGE_DISPATCH", AttachmentKey.create(Boolean.class));

  @SuppressWarnings("unchecked")
  public static final AttachmentKey<AgentSpan> DD_UNDERTOW_SPAN =
      attachmentStore.putIfAbsent("DD_UNDERTOW_SPAN", AttachmentKey.create(AgentSpan.class));

  public static final UndertowDecorator DECORATE = new UndertowDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"undertow-http", "undertow-http-server"};
  }

  @Override
  protected CharSequence component() {
    return UNDERTOW_HTTP_SERVER;
  }

  @Override
  protected AgentPropagation.ContextVisitor<HttpServerExchange> getter() {
    return UndertowExtractAdapter.Request.GETTER;
  }

  @Override
  protected AgentPropagation.ContextVisitor<HttpServerExchange> responseGetter() {
    return UndertowExtractAdapter.Response.GETTER;
  }

  @Override
  public CharSequence spanName() {
    return UNDERTOW_REQUEST;
  }

  @Override
  protected String method(final HttpServerExchange exchange) {
    return exchange.getRequestMethod().toString();
  }

  @Override
  protected URIDataAdapter url(final HttpServerExchange exchange) {
    return new HttpServerExchangeURIDataAdapter(exchange);
  }

  @Override
  protected String peerHostIP(final HttpServerExchange exchange) {
    return exchange.getSourceAddress().getAddress().getHostAddress();
  }

  @Override
  protected int peerPort(final HttpServerExchange exchange) {
    return exchange.getDestinationAddress().getPort();
  }

  @Override
  protected int status(final HttpServerExchange exchange) {
    return exchange.getResponseCode();
  }

  @Override
  protected BlockResponseFunction createBlockResponseFunction(
      HttpServerExchange httpServerExchange) {
    return new UndertowBlockResponseFunction(httpServerExchange);
  }
}
