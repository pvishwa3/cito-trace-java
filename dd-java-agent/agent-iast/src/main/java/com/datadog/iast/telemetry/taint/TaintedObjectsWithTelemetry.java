package com.datadog.iast.telemetry.taint;

import static datadog.trace.api.iast.telemetry.IastMetrics.EXECUTED_TAINTED;
import static datadog.trace.api.iast.telemetry.IastMetrics.REQUEST_TAINTED;
import static datadog.trace.api.iast.telemetry.IastMetrics.TAINTED_FLAT_MODE;

import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import com.datadog.iast.taint.TaintedObject;
import com.datadog.iast.taint.TaintedObjects;
import datadog.trace.api.iast.telemetry.Verbosity;
import javax.annotation.Nonnull;

public abstract class TaintedObjectsWithTelemetry {

  private TaintedObjectsWithTelemetry() {}

  public static TaintedObjects withTelemetry(
      final Verbosity verbosity, final TaintedObjects taintedObjects) {
    if (verbosity.allows(Verbosity.DEBUG)) {
      return new TaintedObjectsDebug(taintedObjects);
    }
    if (verbosity.allows(Verbosity.INFORMATION)) {
      return new TaintedObjectsInformation(taintedObjects);
    }
    return taintedObjects;
  }

  private static class TaintedObjectsInformation implements TaintedObjects {
    protected final TaintedObjects delegate;

    public TaintedObjectsInformation(final TaintedObjects delegate) {
      this.delegate = delegate;
    }

    @Override
    public TaintedObject taintInputString(@Nonnull String obj, @Nonnull Source source) {
      return delegate.taintInputString(obj, source);
    }

    @Override
    public TaintedObject taint(@Nonnull Object obj, @Nonnull Range[] ranges) {
      return delegate.taint(obj, ranges);
    }

    @Override
    public TaintedObject get(@Nonnull Object obj) {
      return delegate.get(obj);
    }

    @Override
    public void release() {
      delegate.release();
      if (delegate.isFlat()) {
        TAINTED_FLAT_MODE.add(1);
      } else {
        REQUEST_TAINTED.add(delegate.getEstimatedSize());
      }
    }

    @Override
    public long getEstimatedSize() {
      return delegate.getEstimatedSize();
    }

    @Override
    public boolean isFlat() {
      return delegate.isFlat();
    }
  }

  private static class TaintedObjectsDebug extends TaintedObjectsInformation {

    public TaintedObjectsDebug(final TaintedObjects delegate) {
      super(delegate);
    }

    @Override
    public TaintedObject taintInputString(@Nonnull String obj, @Nonnull Source source) {
      final TaintedObject result = delegate.taintInputString(obj, source);
      EXECUTED_TAINTED.add(1);
      return result;
    }

    @Override
    public TaintedObject taint(@Nonnull Object obj, @Nonnull Range[] ranges) {
      final TaintedObject result = delegate.taint(obj, ranges);
      EXECUTED_TAINTED.add(1);
      return result;
    }
  }
}
