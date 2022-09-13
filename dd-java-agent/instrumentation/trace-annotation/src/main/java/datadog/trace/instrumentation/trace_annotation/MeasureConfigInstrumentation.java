package datadog.trace.instrumentation.trace_annotation;

import static datadog.trace.agent.tooling.bytebuddy.matcher.ClassLoaderMatchers.hasClassesNamed;
import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.hasSuperType;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static datadog.trace.util.ConfigParser.parseMethods;
import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.api.Config;
import datadog.trace.util.Strings;
import de.thetaphi.forbiddenapis.SuppressForbidden;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;


/**
 * TraceConfig Instrumentation does not extend Default.
 *
 * <p>Instead it directly implements Instrumenter#instrument() and adds one default Instrumenter for
 * every configured class+method-list.
 *
 * <p>If this becomes a more common use case the building logic should be abstracted out into a
 * super class.
 */
@AutoService(Instrumenter.class)
public class MeasureConfigInstrumentation implements Instrumenter {
  private final Map<String, Set<String>> classMethodsToMeasure;

  @SuppressForbidden
  public MeasureConfigInstrumentation() {
    final String configString =
        Config.get().getMeasureMethods() == null ? null : Config.get().getMeasureMethods().trim();
    if (configString == null || configString.isEmpty()) {
      classMethodsToMeasure = Collections.emptyMap();
    } else {
      classMethodsToMeasure = Collections.unmodifiableMap(parseMethods(configString));
    }
  }

  @Override
  public boolean isApplicable(Set<TargetSystem> enabledSystems) {
    return enabledSystems.contains(TargetSystem.TRACING);
  }

  @Override
  public void instrument(TransformerBuilder transformerBuilder) {
    if (classMethodsToMeasure.isEmpty()) {
      return;
    }

    for (final Map.Entry<String, Set<String>> entry : classMethodsToMeasure.entrySet()) {
      final TracerClassInstrumentation tracerClassInstrumentation =
          new TracerClassInstrumentation(entry.getKey(), entry.getValue());
      transformerBuilder.applyInstrumentation(tracerClassInstrumentation);
    }
  }

  // Not Using AutoService to hook up this instrumentation
  public static class TracerClassInstrumentation extends Tracing implements ForTypeHierarchy {
    private final String className;
    private final Set<String> methodNames;

    /** No-arg constructor only used by muzzle and tests. */
    public TracerClassInstrumentation() {
      this("datadog.trace.api.Trace", Collections.singleton("noop"));
    }

    public TracerClassInstrumentation(final String className, final Set<String> methodNames) {
      super(
          "trace",
          "trace-config",
          "trace-config_"
              + className
              + (!Config.get().isDebugEnabled() ? "" : "[" + Strings.join(",", methodNames) + "]"));
      this.className = className;
      this.methodNames = methodNames;
    }

    @Override
    public ElementMatcher<ClassLoader> classLoaderMatcher() {
      // Optimization for expensive typeMatcher.
      return hasClassesNamed(className);
    }

    @Override
    public ElementMatcher<TypeDescription> hierarchyMatcher() {
      return hasSuperType(named(className));
    }

    @Override
    public String[] helperClassNames() {
      return new String[] {
          packageName + ".TraceDecorator",
      };
    }

    @Override
    public void adviceTransformations(AdviceTransformation transformation) {
      boolean hasWildcard = false;
      for (String methodName : methodNames) {
        hasWildcard |= methodName.equals("*");
      }
      ElementMatcher<MethodDescription> methodFilter;
      if (hasWildcard) {
        methodFilter =
            not(
                isHashCode()
                    .or(isEquals())
                    .or(isToString())
                    .or(isFinalizer())
                    .or(isGetter())
                    .or(isSetter())
                    .or(isSynthetic()));
      } else {
        methodFilter = namedOneOf(methodNames);
      }
      transformation.applyAdvice(isMethod().and(methodFilter), packageName + ".TraceAdvice");
    }
  }
}
