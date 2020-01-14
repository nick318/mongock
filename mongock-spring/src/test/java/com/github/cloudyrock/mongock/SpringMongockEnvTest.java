package com.github.cloudyrock.mongock;

import com.github.cloudyrock.mongock.resources.EnvironmentMock;
import com.github.cloudyrock.mongock.test.changelogs.EnvironmentDependentTestResource;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by lstolowski on 13.07.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringMongockEnvTest extends SpringMongockTestBase {

  @Test
  public void shouldRunChangesetWithEnvironment() {
    // given
    changeService.setEnvironment(new EnvironmentMock());
    changeService.setChangeLogsBasePackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(changeEntryRepository.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntryMongo.KEY_CHANGE_ID, "Envtest1")
            .append(ChangeEntryMongo.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

  }

  @Test
  public void shouldRunChangesetWithNullEnvironment() throws Exception {
    // given
    changeService.setEnvironment(null);
    changeService.setChangeLogsBasePackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(changeEntryRepository.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntryMongo.KEY_CHANGE_ID, "Envtest1")
            .append(ChangeEntryMongo.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

  }

}
