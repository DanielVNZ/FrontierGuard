package com.danielvnz.landclaimer.database.dao;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for Data Access Objects providing common CRUD operations
 * @param <T> The entity type
 * @param <ID> The ID type
 */
public interface BaseDao<T, ID> {
    
    /**
     * Saves an entity to the database
     * @param entity The entity to save
     * @return CompletableFuture that completes when the save operation is done
     */
    CompletableFuture<Void> save(T entity);
    
    /**
     * Finds an entity by its ID
     * @param id The ID to search for
     * @return CompletableFuture containing an Optional with the entity if found
     */
    CompletableFuture<Optional<T>> findById(ID id);
    
    /**
     * Finds all entities
     * @return CompletableFuture containing a list of all entities
     */
    CompletableFuture<List<T>> findAll();
    
    /**
     * Updates an existing entity
     * @param entity The entity to update
     * @return CompletableFuture that completes when the update operation is done
     */
    CompletableFuture<Void> update(T entity);
    
    /**
     * Deletes an entity by its ID
     * @param id The ID of the entity to delete
     * @return CompletableFuture that completes when the delete operation is done
     */
    CompletableFuture<Void> deleteById(ID id);
    
    /**
     * Checks if an entity exists by its ID
     * @param id The ID to check
     * @return CompletableFuture containing true if the entity exists, false otherwise
     */
    CompletableFuture<Boolean> existsById(ID id);
    
    /**
     * Counts the total number of entities
     * @return CompletableFuture containing the count of entities
     */
    CompletableFuture<Long> count();
}