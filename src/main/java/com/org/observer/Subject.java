package com.org.observer;


/**
 * Subject interface. Part of the Subject-Observer design pattern.
 *
 * @param <T>
 */
public interface Subject<T> {
    /**
     * Register a new observer object
     * @param o
     */
    void register(Observer o);

    /**
     * Unregister an existing observer object
     * @param o
     */
    void unregister(Observer o);

    /**
     * When the subject was updated, then notify all registered observers.
     */
    void notifyObservers();

    /**
     * Get the state of the object. This function is used by the observers
     * to pull the updates from the subject.
     * @return the subject's current state
     */
    T getState();
}
