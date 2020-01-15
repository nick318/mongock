package com.github.cloudyrock.mongock;

import com.github.cloudyrock.mongock.test.changelogs.AnotherMongockTestResource;
import com.github.cloudyrock.mongock.test.changelogs.MongockTestResource;
import com.github.cloudyrock.mongock.test.changelogs.versioned.MongockVersioningTestResource;
import com.github.cloudyrock.mongock.utils.ChangeLogWithDuplicate;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 *
 * @since 27/07/2014
 */
public class ChangeServiceTest {

  private String executionId = "executionId";

  @Test
  public void shouldFindChangeLogClasses() {
    // given
    String scanPackage = MongockTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);
    // when
    List<Class<?>> foundClasses = service.fetchChangeLogsSorted();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }

  @Test
  public void shouldFindChangeSetMethods() throws MongockException {
    // given
    String scanPackage = MongockTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSetsSorted(MongockTestResource.class);

    // then
    assertTrue(foundMethods != null);
    assertEquals(4, foundMethods.size());
  }

  @Test
  public void shouldFindVersionedChangeSetMethods() throws MongockException {
    String scanPackage = MongockVersioningTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);

    assertEquals(10, service.fetchChangeSetsSorted(MongockVersioningTestResource.class).size());

    service.setStartVersion("2018");
    assertEquals(3, service.fetchChangeSetsSorted(MongockVersioningTestResource.class).size());

    service.setStartVersion("1.0");
    assertEquals(6, service.fetchChangeSetsSorted(MongockVersioningTestResource.class).size());

    service.setStartVersion("1.0");
    service.setEndVersion("2018");
    assertEquals(3, service.fetchChangeSetsSorted(MongockVersioningTestResource.class).size());
  }

  @Test
  public void shouldFindAnotherChangeSetMethods() throws MongockException {
    // given
    String scanPackage = MongockTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSetsSorted(AnotherMongockTestResource.class);

    // then
    assertTrue(foundMethods != null);
    assertEquals(5, foundMethods.size());
  }

  @Test
  public void shouldFindIsRunAlwaysMethod() throws MongockException {
    // given
    String scanPackage = MongockTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSetsSorted(AnotherMongockTestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")) {
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
  }

  @Test
  public void shouldCreateEntry() throws MongockException {

    // given
    String scanPackage = MongockTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);
    List<Method> foundMethods = service.fetchChangeSetsSorted(MongockTestResource.class);

    for (Method foundMethod : foundMethods) {

      // when
      ChangeEntry entry = service.createChangeEntry(executionId, foundMethod, null);

      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(MongockTestResource.class.getName(), entry.getChangeLogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test(expected = MongockException.class)
  public void shouldFailOnDuplicatedChangeSets() throws MongockException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new ChangeService();
    service.setChangeLogsBasePackage(scanPackage);
    service.fetchChangeSetsSorted(ChangeLogWithDuplicate.class);
  }

}
