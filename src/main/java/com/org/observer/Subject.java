package com.org.observer;

public interface Subject<T> {
    void register(Observer o);
    void unregister(Observer o);
    void notifyObservers();
    T getState();
}
