package com.github.cloudyrock.mongock.executor;

import com.github.cloudyrock.mongock.ChangeEntry;
import com.github.cloudyrock.mongock.ChangeEntryMongo;
import com.github.cloudyrock.mongock.ChangeEntryRepository;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.LogUtils;
import com.github.cloudyrock.mongock.MongockException;
import com.github.cloudyrock.mongock.change.ChangeLogItem;
import com.github.cloudyrock.mongock.change.ChangeSetItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MigrationExecutor {

  private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

  private final Map<Class, Object> dependencies;
  private final ChangeEntryRepository changeEntryRepository;
  private final Map<String, Object> metadata;

  public MigrationExecutor(Map<Class, Object> dependencies, ChangeEntryRepository changeEntryRepository, Map<String, Object> metadata) {
    this.dependencies = dependencies;
    this.changeEntryRepository = changeEntryRepository;
    this.metadata = metadata;
  }

  public void executeMigration(String executionId, List<ChangeLogItem> changeLogs) {
    logger.info("Mongock starting the data migration sequence..");

    for (ChangeLogItem changeLog : changeLogs) {
      try {
        List<ChangeSetItem> changeSets = changeLog.getChangeSetElements();
        for (ChangeSetItem changeSet : changeSets) {
          executeIfNewOrRunAlways(executionId, changeLog.getInstance(), changeSet);
        }

      } catch (IllegalAccessException e) {
        throw new MongockException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new MongockException(targetException.getMessage(), e);
      }

    }
  }


  private void executeIfNewOrRunAlways(String executionId, Object changelogInstance, ChangeSetItem changeSet) throws IllegalAccessException, InvocationTargetException {
    try {
      ChangeEntry changeEntry = createChangeEntry(executionId, changeSet.getMethod(), this.metadata);
      if (changeEntryRepository.isNewChange(changeEntry)) {
        final long executionTimeMillis = executeChangeSetMethod(changeSet.getMethod(), changelogInstance);
        changeEntry.setExecutionMillis(executionTimeMillis);
        changeEntryRepository.save(changeEntry);
        logger.info("APPLIED - {}", changeEntry);

      } else if (changeSet.isRunAlways()) {
        final long executionTimeMillis = executeChangeSetMethod(changeSet.getMethod(), changelogInstance);
        changeEntry.setExecutionMillis(executionTimeMillis);
        changeEntryRepository.save(changeEntry);
        logger.info("RE-APPLIED - {}", changeEntry);

      } else {
        logger.info("PASSED OVER - {}", changeEntry);
      }
    } catch (MongockException e) {
      logger.error(e.getMessage(), e);
    }
  }

  @Deprecated
  //TODO figure out how to fix this: could be a factory, specialization class, etc.
  private ChangeEntry createChangeEntry(String executionId, Method changesetMethod, Map<String, Object> metadata) {
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)) {
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);

      //TODO remove Mongo specialization
      return new ChangeEntryMongo(
          executionId,
          annotation.id(),
          annotation.author(),
          new Date(),
          changesetMethod.getDeclaringClass().getName(),
          changesetMethod.getName(),
          metadata);
    } else {
      return null;
    }
  }

  protected Optional<Object> getDependency(Class type) {
    return this.dependencies.entrySet().stream()
        .filter(entrySet -> type.isAssignableFrom(entrySet.getKey()))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private long executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance)
      throws IllegalAccessException, InvocationTargetException {
    final long startingTime = System.currentTimeMillis();
    List<Object> changelogInvocationParameters = new ArrayList<>(changeSetMethod.getParameterTypes().length);
    for (Class<?> parameter : changeSetMethod.getParameterTypes()) {
      Optional<Object> parameterOptional = this.getDependency(parameter);
      if (parameterOptional.isPresent()) {
        changelogInvocationParameters.add(parameterOptional.get());
      } else {
        throw new MongockException(String.format("Method[%s] using argument[%s] not injected", changeSetMethod.getName(), parameter.getName()));
      }
    }
    LogUtils.logMethodWithArguments(logger, changeSetMethod.getName(), changelogInvocationParameters);
    changeSetMethod.invoke(changeLogInstance, changelogInvocationParameters.toArray());
    return System.currentTimeMillis() - startingTime;
  }


}
