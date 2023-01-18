package datadog.trace.plugin.csi.impl.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target([ ElementType.METHOD, ElementType.TYPE])
@Retention(RetentionPolicy.CLASS)
@interface Source {
  String value();
}
