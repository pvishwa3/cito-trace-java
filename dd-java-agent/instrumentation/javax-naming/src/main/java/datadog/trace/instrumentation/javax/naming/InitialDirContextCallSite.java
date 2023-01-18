package datadog.trace.instrumentation.javax.naming;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.model.VulnerabilityTypes;
import datadog.trace.api.iast.sink.LdapInjectionModule;
import datadog.trace.api.iast.telemetry.Sink;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.Name;
import javax.naming.directory.SearchControls;

@Sink(VulnerabilityTypes.LDAP_INJECTION)
@CallSite(spi = IastAdvice.class)
public class InitialDirContextCallSite {

  @CallSite.Before(
      "javax.naming.NamingEnumeration javax.naming.directory.InitialDirContext.search(java.lang.String, java.lang.String, javax.naming.directory.SearchControls)")
  public static void beforeSearch(
      @CallSite.Argument @Nullable final String name,
      @CallSite.Argument @Nonnull final String filter,
      @CallSite.Argument @Nullable final SearchControls cons) {
    onDirContextSearch(name, filter, null);
  }

  @CallSite.Before(
      "javax.naming.NamingEnumeration javax.naming.directory.InitialDirContext.search(java.lang.String, java.lang.String, java.lang.Object[], javax.naming.directory.SearchControls)")
  public static void beforeSearch(
      @CallSite.Argument @Nullable final String name,
      @CallSite.Argument @Nonnull final String filterExpr,
      @CallSite.Argument @Nullable Object[] filterArgs,
      @CallSite.Argument @Nullable final SearchControls cons) {
    onDirContextSearch(name, filterExpr, filterArgs);
  }

  @CallSite.Before(
      "javax.naming.NamingEnumeration javax.naming.directory.InitialDirContext.search(javax.naming.Name, java.lang.String, javax.naming.directory.SearchControls)")
  public static void beforeSearch(
      @CallSite.Argument @Nullable final Name name,
      @CallSite.Argument @Nonnull final String filter,
      @CallSite.Argument @Nullable final SearchControls cons) {
    onDirContextSearch(null, filter, null);
  }

  @CallSite.Before(
      "javax.naming.NamingEnumeration javax.naming.directory.InitialDirContext.search(javax.naming.Name, java.lang.String, java.lang.Object[], javax.naming.directory.SearchControls)")
  public static void beforeSearch(
      @CallSite.Argument @Nullable final Name name,
      @CallSite.Argument @Nonnull final String filterExpr,
      @CallSite.Argument @Nullable Object[] filterArgs,
      @CallSite.Argument @Nullable final SearchControls cons) {
    onDirContextSearch(null, filterExpr, filterArgs);
  }

  private static void onDirContextSearch(
      @Nullable final String name,
      @Nonnull final String filterExpr,
      @Nullable final Object[] filterArgs) {
    final LdapInjectionModule module = InstrumentationBridge.LDAP_INJECTION;
    if (module != null) {
      try {
        module.onDirContextSearch(name, filterExpr, filterArgs);
      } catch (final Throwable e) {
        module.onUnexpectedException("onDirContextSearch threw", e);
      }
    }
  }
}
