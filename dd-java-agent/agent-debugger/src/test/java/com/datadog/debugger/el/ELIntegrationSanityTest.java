package com.datadog.debugger.el;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.datadog.debugger.agent.JsonSnapshotSerializer;
import datadog.trace.bootstrap.debugger.DebuggerContext;
import datadog.trace.bootstrap.debugger.FieldExtractor;
import datadog.trace.bootstrap.debugger.Limits;
import datadog.trace.bootstrap.debugger.Snapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class ELIntegrationSanityTest {
  static class Name {
    private String value;

    public Name(String value) {
      this.value = value;
    }
  }

  static class Person {
    private static final String C1 = "constant1";
    private static final int C2 = 42;
    private static List<String> list = new ArrayList<>();
    private String strVal = "strval";
    private int intVal = 24;
    private Map<String, String> mapVal = new HashMap<>();
    private Object[] objArray = new Object[] {new AtomicLong()};
    private Name name = new Name("name");
  }

  @Test
  void extractAfterEl() {
    JsonSnapshotSerializer serializer =
        new JsonSnapshotSerializer(); // Mockito.spy(new JsonSnapshotSerializer());
    DebuggerContext.initSnapshotSerializer(serializer);
    Person p = new Person();
    // set the limit not to follow references to fields
    Limits limits = new Limits(1, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    // create new captured context
    Snapshot.CapturedContext capturedContext = new Snapshot.CapturedContext();
    // this will resolve only the first-level fields of the given object
    List<Snapshot.CapturedValue> flds = new ArrayList<>();
    FieldExtractor.extract(
        p,
        limits,
        (field, value, maxDepth) -> {
          flds.add(
              Snapshot.CapturedValue.of(
                  field.getName(),
                  field.getType().getName(),
                  value,
                  maxDepth,
                  limits.maxCollectionSize,
                  limits.maxLength,
                  limits.maxFieldCount));
        },
        (e, field) -> {},
        (field) -> {});

    capturedContext.addFields(flds.toArray(new Snapshot.CapturedValue[0]));

    // '.name.value' is not present in the snapshot - it needs to be retrieved via reflection
    Value<?> val = DSL.getMember(DSL.ref("name"), "value").evaluate(capturedContext);
    // make sure the nested field was properly resolved
    assertEquals(p.name.value, val.getValue());

    // freeze the captured context
    capturedContext.freeze();

    // after freezing the original value is removed and only the serialized json representation
    // remains
    assertNull(capturedContext.getFields().get("name").getValue());
    assertEquals(
        "{\"type\":\"com.datadog.debugger.el.ELIntegrationSanityTest$Name\",\"fields\":{\"value\":{\"type\":\"java.lang.String\",\"value\":\"name\"}}}",
        capturedContext.getFields().get("name").getStrValue());
  }
}
