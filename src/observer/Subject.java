package observer;

import observer.Observer;

public interface Subject {
    void register(Observer o);
    void unregister(Observer o);
    void notifyObservers();
    Object getState();
}
