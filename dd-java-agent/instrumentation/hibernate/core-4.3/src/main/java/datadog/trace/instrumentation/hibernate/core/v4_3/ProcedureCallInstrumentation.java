package datadog.trace.instrumentation.hibernate.core.v4_3;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.instrumentation.hibernate.SessionMethodUtils;
import datadog.trace.instrumentation.hibernate.SessionState;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.procedure.ProcedureCall;

@AutoService(Instrumenter.class)
public class ProcedureCallInstrumentation extends AbstractHibernateInstrumentation {

  @Override
  public String[] knownMatchingTypes() {
    return new String[] {"org.hibernate.procedure.internal.ProcedureCallImpl"};
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return implementsInterface(named("org.hibernate.procedure.ProcedureCall"));
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("org.hibernate.procedure.ProcedureCall", SessionState.class.getName());
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod().and(named("getOutputs")),
        ProcedureCallInstrumentation.class.getName() + "$ProcedureCallMethodAdvice");
  }

  public static class ProcedureCallMethodAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SessionState startMethod(
        @Advice.This final ProcedureCall call,
        @Advice.Origin("hibernate.procedure.#m") final String operationName) {

      final ContextStore<ProcedureCall, SessionState> contextStore =
          InstrumentationContext.get(ProcedureCall.class, SessionState.class);

      final SessionState state =
          SessionMethodUtils.startScopeFrom(
              contextStore, call, operationName, call.getProcedureName(), true);
      return state;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endMethod(
        @Advice.Enter final SessionState state, @Advice.Thrown final Throwable throwable) {
      SessionMethodUtils.closeScope(state, throwable, null, true);
    }
  }
}
