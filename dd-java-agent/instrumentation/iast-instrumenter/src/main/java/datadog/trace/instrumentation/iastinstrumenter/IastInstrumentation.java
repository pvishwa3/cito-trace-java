package datadog.trace.instrumentation.iastinstrumenter;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.bytebuddy.csi.CallSiteInstrumentation;
import datadog.trace.agent.tooling.bytebuddy.csi.CallSiteSupplier;
import datadog.trace.agent.tooling.csi.CallSiteAdvice;
import datadog.trace.api.Config;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector;
import datadog.trace.instrumentation.iastinstrumenter.telemetry.TelemetryCallSiteSupplier;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nonnull;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class IastInstrumentation extends CallSiteInstrumentation {

  private String[] helpers;

  public IastInstrumentation() {
    super("IastInstrumentation");
  }

  @Override
  public ElementMatcher<TypeDescription> callerType() {
    return IastMatcher.INSTANCE;
  }

  @Override
  public boolean isApplicable(final Set<TargetSystem> enabledSystems) {
    return enabledSystems.contains(TargetSystem.IAST);
  }

  @Override
  protected CallSiteSupplier callSites() {
    final CallSiteSupplier supplier = IastCallSiteSupplier.INSTANCE;
    final Config config = Config.get();
    if (!config.isTelemetryEnabled()) {
      return supplier;
    }
    return new TelemetryCallSiteSupplier(config.getIastTelemetryVerbosity(), supplier);
  }

  @Override
  public String[] helperClassNames() {
    if (null == helpers) {
      if (!Config.get().isTelemetryEnabled()) {
        helpers = super.helperClassNames();
      } else {
        helpers = append(super.helperClassNames(), IastTelemetryCollector.class.getName());
      }
    }
    return helpers;
  }

  private static String[] append(@Nonnull final String[] items, final String newItem) {
    final String[] result = new String[items.length + 1];
    System.arraycopy(items, 0, result, 0, items.length);
    result[result.length - 1] = newItem;
    return result;
  }

  public static final class IastMatcher
      extends ElementMatcher.Junction.ForNonNullValues<TypeDescription> {
    public static final IastMatcher INSTANCE = new IastMatcher();

    @Override
    protected boolean doMatch(TypeDescription target) {
      return IastExclusionTrie.apply(target.getName()) != 1;
    }
  }

  public static class IastCallSiteSupplier implements CallSiteSupplier {

    public static final CallSiteSupplier INSTANCE = new IastCallSiteSupplier(IastAdvice.class);

    private final Class<?> spiInterface;

    public IastCallSiteSupplier(final Class<?> spiInterface) {
      this.spiInterface = spiInterface;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterable<CallSiteAdvice> get() {
      final ClassLoader targetClassLoader = CallSiteInstrumentation.class.getClassLoader();
      return (ServiceLoader<CallSiteAdvice>) ServiceLoader.load(spiInterface, targetClassLoader);
    }
  }
}
