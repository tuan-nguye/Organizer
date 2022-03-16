package organizer;

import observer.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public abstract class Organizer implements observer.Subject<Integer> {
    protected Calendar calendar;
    protected String[] months;
    private List<Observer> obs;
    protected int count;
    protected StringBuilder errors;

    public Organizer() {
        obs = new ArrayList<>();
        calendar = Calendar.getInstance();
        months = new String[] {"jan", "feb", "maerz", "apr", "mai", "juni", "juli",
                "aug", "sept", "okt", "nov", "dez"};
        errors = new StringBuilder();
    }

    public abstract String copyAndOrganize(String source, String destination);

    public int getCount() {
        return this.count;
    }

    public void register(Observer o) {
        obs.add(o);
    }

    public void unregister(Observer o) {
        obs.remove(o);
    }

    public void notifyObservers() {
        for(Observer o : obs) {
            o.update();
        }
    }

    public Integer getState() {
        return count;
    }
}
