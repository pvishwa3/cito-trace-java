package datadog.trace.instrumentation.iastinstrumenter.telemetry;

import datadog.trace.agent.tooling.csi.InvokeAdvice;
import datadog.trace.api.iast.telemetry.Verbosity;

public class TelemetryInvokeAdvice extends AbstractTelemetryAdvice<InvokeAdvice>
    implements InvokeAdvice {

  public TelemetryInvokeAdvice(final Verbosity verbosity, final InvokeAdvice advice) {
    super(verbosity, advice);
  }

  @Override
  public void apply(
      final MethodHandler handler,
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    advice.apply(handler, opcode, owner, name, descriptor, isInterface);
    onCallSiteDiscovered(handler);
  }
}
