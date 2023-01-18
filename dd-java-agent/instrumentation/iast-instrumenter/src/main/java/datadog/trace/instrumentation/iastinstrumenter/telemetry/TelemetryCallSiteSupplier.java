package datadog.trace.instrumentation.iastinstrumenter.telemetry;

import datadog.trace.agent.tooling.bytebuddy.csi.CallSiteSupplier;
import datadog.trace.agent.tooling.csi.CallSiteAdvice;
import datadog.trace.agent.tooling.csi.InvokeAdvice;
import datadog.trace.agent.tooling.csi.InvokeDynamicAdvice;
import datadog.trace.api.iast.telemetry.Verbosity;
import java.util.Iterator;
import javax.annotation.Nonnull;

public class TelemetryCallSiteSupplier implements CallSiteSupplier {

  private final Verbosity verbosity;
  private final CallSiteSupplier delegate;

  public TelemetryCallSiteSupplier(final Verbosity verbosity, final CallSiteSupplier delegate) {
    this.verbosity = verbosity;
    this.delegate = delegate;
  }

  @Override
  public Iterable<CallSiteAdvice> get() {
    final Iterable<CallSiteAdvice> iterable = delegate.get();
    return () -> new IteratorAdapter(verbosity, iterable.iterator());
  }

  private static class IteratorAdapter implements Iterator<CallSiteAdvice> {

    private final Verbosity verbosity;
    private final Iterator<CallSiteAdvice> delegate;

    private IteratorAdapter(
        @Nonnull final Verbosity verbosity, @Nonnull final Iterator<CallSiteAdvice> delegate) {
      this.verbosity = verbosity;
      this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public CallSiteAdvice next() {
      final CallSiteAdvice advice = delegate.next();
      if (advice == null) {
        return null;
      }
      return advice instanceof InvokeAdvice
          ? new TelemetryInvokeAdvice(verbosity, (InvokeAdvice) advice)
          : new TelemetryInvokeDynamicAdvice(verbosity, (InvokeDynamicAdvice) advice);
    }
  }
}
