package datadog.trace.instrumentation.jetty;

import datadog.appsec.api.blocking.BlockingContentType;
import datadog.trace.api.gateway.BlockResponseFunction;
import datadog.trace.api.gateway.Flow;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public class JettyBlockResponseFunction implements BlockResponseFunction {
  private final Request request;

  public JettyBlockResponseFunction(Request request) {
    this.request = request;
  }

  @Override
  public boolean tryCommitBlockingResponse(int statusCode, BlockingContentType templateType) {
    Response response = request.getResponse();
    return JettyBlockingHelper.block(
        request, response, new Flow.Action.RequestBlockingAction(statusCode, templateType));
  }
}
