package com.github.cloudyrock.mongock;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;

/**
 * <p>Repository class to manage lock in database</p>
 *
 *
 * @since 04/04/2018
 */
class LockMongoRepository extends MongoRepositoryBase implements LockRepository {

  LockMongoRepository(String collectionName, MongoDatabase db) {
    super(db, collectionName, new String[]{LockEntryMongo.KEY_FIELD});
  }

  /**
   * If there is a lock in the database with the same key, updates it if either is expired or both share the same owner.
   * If there is no lock with the same key, it's inserted.
   *
   * @param newLock lock to replace the existing one or be inserted.
   * @throws LockPersistenceException if there is a lock in database with same key, but is expired and belong to
   *                                  another owner or cannot insert/update the lock for any other reason
   */
  public void insertUpdate(LockEntry newLock)  {
    insertUpdate(newLock, false);
  }

  /**
   * If there is a lock in the database with the same key and owner, updates it.Otherwise throws a LockPersistenceException
   *
   * @param newLock lock to replace the existing one.
   * @throws LockPersistenceException if there is no lock in the database with the same key and owner or cannot update
   *                                  the lock for any other reason
   */
  public void updateIfSameOwner(LockEntry newLock)  {
    insertUpdate(newLock, true);
  }

  /**
   * Retrieves a lock by key
   *
   * @param lockKey key
   * @return LockEntry
   */
  public LockEntry findByKey(String lockKey) {
    Document result = collection.find(new Document().append(LockEntryMongo.KEY_FIELD, lockKey)).first();
    if (result != null) {
      return new LockEntryMongo(
          result.getString(LockEntryMongo.KEY_FIELD),
          result.getString(LockEntryMongo.STATUS_FIELD),
          result.getString(LockEntryMongo.OWNER_FIELD),
          result.getDate(LockEntryMongo.EXPIRES_AT_FIELD)
      );
    }
    return null;
  }

  /**
   * Removes from database all the locks with the same key(only can be one) and owner
   *
   * @param lockKey lock key
   * @param owner   lock owner
   */
  public void removeByKeyAndOwner(String lockKey, String owner) {
    collection
        .deleteMany(Filters.and(Filters.eq(LockEntryMongo.KEY_FIELD, lockKey), Filters.eq(LockEntryMongo.OWNER_FIELD, owner)));
  }

  private void insertUpdate(LockEntry newLock, boolean onlyIfSameOwner)  {
    boolean lockHeld;
    try {

      final Bson acquireLockQuery =
          getAcquireLockQuery(newLock.getKey(), newLock.getOwner(), onlyIfSameOwner);

      final UpdateResult result = collection.updateMany(
          acquireLockQuery,
          new Document().append("$set", newLock.getItemForDB()),
          new UpdateOptions().upsert(!onlyIfSameOwner));

      lockHeld = result.getModifiedCount() <= 0 && result.getUpsertedId() == null;

    } catch (MongoWriteException ex) {
      lockHeld = ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY;
      if (!lockHeld) {
        throw ex;
      }

    } catch (DuplicateKeyException ex) {
      lockHeld = true;
    }

    if (lockHeld) {
      throw new LockPersistenceException("Lock is held");
    }
  }

  private Bson getAcquireLockQuery(String lockKey, String owner, boolean onlyIfSameOwner) {
    final Bson expiresAtCond = Filters.lt(LockEntryMongo.EXPIRES_AT_FIELD, new Date());
    final Bson ownerCond = Filters.eq(LockEntryMongo.OWNER_FIELD, owner);
    final Bson orCond = onlyIfSameOwner ? Filters.or(ownerCond) : Filters.or(expiresAtCond, ownerCond);
    return Filters
        .and(Filters.eq(LockEntryMongo.KEY_FIELD, lockKey), Filters.eq(LockEntryMongo.STATUS_FIELD, LockStatus.LOCK_HELD.name()),
            orCond);
  }

}
