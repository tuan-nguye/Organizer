package com.org.observer;


/**
 * Observer interface with a pull implementation. Part of the Subject-Observer
 * design pattern.
 */
public interface Observer {
    /**
     * update the observer by pulling the changed values
     * from the Subject.
     */
    void update();
}
