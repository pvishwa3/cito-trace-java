package datadog.trace.instrumentation.java.lang;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.model.VulnerabilityTypes;
import datadog.trace.api.iast.sink.PathTraversalModule;
import datadog.trace.api.iast.telemetry.Sink;
import javax.annotation.Nullable;

@Sink(VulnerabilityTypes.PATH_TRAVERSAL)
@CallSite(spi = IastAdvice.class)
public class FileOutputStreamCallSite {

  @CallSite.Before("void java.io.FileOutputStream.<init>(java.lang.String)")
  public static void beforeConstructor(@CallSite.Argument @Nullable final String path) {
    if (path != null) {
      final PathTraversalModule module = InstrumentationBridge.PATH_TRAVERSAL;
      if (module != null) {
        try {
          module.onPathTraversal(path);
        } catch (final Throwable e) {
          module.onUnexpectedException("beforeConstructor threw", e);
        }
      }
    }
  }
}
