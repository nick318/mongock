package com.github.cloudyrock.mongock;

import org.bson.Document;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class LockEntryTest {

  @Test
  public void constructorAnGetters() {
    Date date = new Date();
    final LockEntry e = new LockEntryMongo("KEY", "STATUS", "OWNER", date);
    assertEquals("KEY", e.getKey());
    assertEquals("STATUS", e.getStatus());
    assertEquals("OWNER", e.getOwner());
    assertEquals(date, e.getExpiresAt());
  }

  @Test
  public void buildFullDBObject() {
    Document actual = new LockEntryMongo("KEY", "STATUS", "OWNER", new Date(1)).buildFullDBObject();
    Document expected = new Document()
        .append("key", "KEY")
        .append("status", "STATUS")
        .append("owner", "OWNER")
        .append("expiresAt", new Date(1));
    assertEquals(expected, actual);
  }

}
