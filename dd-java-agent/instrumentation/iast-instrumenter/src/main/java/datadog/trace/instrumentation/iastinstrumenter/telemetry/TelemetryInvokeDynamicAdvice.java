package datadog.trace.instrumentation.iastinstrumenter.telemetry;

import datadog.trace.agent.tooling.csi.InvokeDynamicAdvice;
import datadog.trace.api.iast.telemetry.Verbosity;
import net.bytebuddy.jar.asm.Handle;

public class TelemetryInvokeDynamicAdvice extends AbstractTelemetryAdvice<InvokeDynamicAdvice>
    implements InvokeDynamicAdvice {

  public TelemetryInvokeDynamicAdvice(final Verbosity verbosity, final InvokeDynamicAdvice advice) {
    super(verbosity, advice);
  }

  @Override
  public void apply(
      final MethodHandler handler,
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    advice.apply(handler, name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    onCallSiteDiscovered(handler);
  }
}
