package com.github.cloudyrock.mongock;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpringBootMongock extends Mongock implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(SpringBootMongock.class);

  private ApplicationContext springContext;
  private MongoTemplate mongoTemplate;

  SpringBootMongock(ChangeEntryRepository changeEntryRepository,
                    Closeable mongoClientCloseable,
                    ChangeService changeService,
                    LockChecker lockChecker) {
    super(changeEntryRepository, mongoClientCloseable, changeService, lockChecker);
  }

  /**
   * @see ApplicationRunner#run(ApplicationArguments)
   * @see Mongock#execute()
   */
  @Override
  public void run(ApplicationArguments args) {
    execute();
  }


  @Override
  protected long executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance) throws BeansException, IllegalAccessException, InvocationTargetException {
    final long startingTime = System.currentTimeMillis();
    List<Object> changelogInvocationParameters = new ArrayList<>(changeSetMethod.getParameterTypes().length);
    for (Class<?> parameter : changeSetMethod.getParameterTypes()) {
      if (MongoTemplate.class.isAssignableFrom(parameter)) {
        changelogInvocationParameters.add(mongoTemplate);

      } else if (DB.class.isAssignableFrom(parameter)) {
        throw new UnsupportedOperationException("DB not supported by Mongock. Please use MongoDatabase");

      } else if (MongoDatabase.class.isAssignableFrom(parameter)) {
        changelogInvocationParameters.add(this.changelogMongoDatabase);

      } else {
        changelogInvocationParameters.add(springContext.getBean(parameter));
      }
    }
    logMethodWithArguments(changeSetMethod.getName(), changelogInvocationParameters);
    changeSetMethod.invoke(changeLogInstance, changelogInvocationParameters.toArray());
    return System.currentTimeMillis() - startingTime;
  }

  private void logMethodWithArguments(String methodName, List<Object> changelogInvocationParameters) {
    String arguments = changelogInvocationParameters.stream()
        .map(obj -> obj != null ? obj.getClass().getName() : "{null argument}")
        .collect(Collectors.joining(", "));
    logger.info("method[{}] with arguments: [{}]", methodName, arguments);

  }


  SpringBootMongock springContext(ApplicationContext springContext) {
    this.springContext = springContext;
    return this;
  }


  SpringBootMongock setMongoTemplate(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    return this;
  }

}
