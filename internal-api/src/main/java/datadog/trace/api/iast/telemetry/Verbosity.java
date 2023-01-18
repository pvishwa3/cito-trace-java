package datadog.trace.api.iast.telemetry;

public enum Verbosity {
  MANDATORY,
  INFORMATION,
  DEBUG;

  public boolean allows(final Verbosity value) {
    return value.ordinal() <= ordinal();
  }
}
