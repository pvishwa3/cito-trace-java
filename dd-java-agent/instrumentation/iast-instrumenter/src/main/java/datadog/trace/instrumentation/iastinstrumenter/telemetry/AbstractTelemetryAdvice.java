package datadog.trace.instrumentation.iastinstrumenter.telemetry;

import static datadog.trace.api.iast.telemetry.IastTelemetryCollector.Metric;

import datadog.trace.agent.tooling.csi.CallSiteAdvice;
import datadog.trace.agent.tooling.csi.Pointcut;
import datadog.trace.api.iast.telemetry.*;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.TaggedMetric;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

public abstract class AbstractTelemetryAdvice<A extends CallSiteAdvice> implements CallSiteAdvice {

  private static final String IAST_COLLECTOR_TYPE =
      Type.getType(IastTelemetryCollector.class).getInternalName();
  private static final Type STRING_TYPE = Type.getType(String.class);
  private static final String INC_METHOD_NAME = "inc";

  private static final String INC_METHOD_DESCRIPTOR =
      Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE);

  private static final String INC_METHOD_WITH_TAG_DESCRIPTOR =
      Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, STRING_TYPE);

  protected final A advice;
  private final Metric instrumentationMetric;
  private final Metric runtimeMetric;
  private final String tag;

  protected AbstractTelemetryAdvice(final Verbosity verbosity, final A advice) {
    this.advice = advice;
    final Class<?> adviceClass = advice.getClass();
    Metric instrumentationMetric = null, runtimeMetric = null;
    boolean allowRuntime = verbosity.allows(Verbosity.INFORMATION);
    String tag = null;
    final Source source = adviceClass.getDeclaredAnnotation(Source.class);
    if (source != null) {
      instrumentationMetric = IastMetrics.INSTRUMENTED_SOURCE;
      runtimeMetric = allowRuntime ? IastMetrics.EXECUTED_SOURCE : null;
      tag = source.value();
    } else {
      final Sink sink = adviceClass.getDeclaredAnnotation(Sink.class);
      if (sink != null) {
        instrumentationMetric = IastMetrics.INSTRUMENTED_SINK;
        runtimeMetric = IastMetrics.EXECUTED_SINK;
        tag = sink.value();
      } else {
        final Propagation propagation = adviceClass.getDeclaredAnnotation(Propagation.class);
        if (propagation != null) {
          instrumentationMetric = IastMetrics.INSTRUMENTED_PROPAGATION;
          runtimeMetric = IastMetrics.EXECUTED_PROPAGATION;
          allowRuntime = verbosity.allows(Verbosity.DEBUG);
        }
      }
    }
    this.instrumentationMetric = instrumentationMetric;
    this.runtimeMetric = allowRuntime ? runtimeMetric : null;
    this.tag = tag;
  }

  @Override
  public Pointcut pointcut() {
    return advice.pointcut();
  }

  protected void onCallSiteDiscovered(final MethodHandler handler) {
    if (instrumentationMetric != null) {
      IastTelemetryCollector.add(instrumentationMetric, 1, tag);
    }
    if (runtimeMetric != null) {
      handler.loadConstant(runtimeMetric.getName());
      if (runtimeMetric instanceof TaggedMetric) {
        handler.loadConstant(tag);
        handler.method(
            Opcodes.INVOKESTATIC,
            IAST_COLLECTOR_TYPE,
            INC_METHOD_NAME,
            INC_METHOD_WITH_TAG_DESCRIPTOR,
            false);
      } else {
        handler.method(
            Opcodes.INVOKESTATIC,
            IAST_COLLECTOR_TYPE,
            INC_METHOD_NAME,
            INC_METHOD_DESCRIPTOR,
            false);
      }
    }
  }

  @Override
  public CallSiteAdvice unwrap() {
    return advice;
  }
}
