import observer.Subject;

import java.util.concurrent.TimeUnit;

public class ProgressBar implements observer.Observer {
    private StringBuilder bar;
    private double percent, max;
    private Subject s;

    public ProgressBar(int length, int max) {
        bar = initializeBar(length);
        this.max = max;
    }

    public StringBuilder initializeBar(int length) {
        StringBuilder str = new StringBuilder();

        str.append('[');
        for(int i = 0; i < length; i++) str.append(' ');
        str.append(']');

        return str;
    }

    private double roundedPercentage(int curr) {
        return 0.01*(Math.round((curr/max)/0.01));
    }

    @Override
    public void update() {
        double updated = roundedPercentage((int) s.getState());
        if(updated != percent) {
            percent = updated;
            updateBar();
            print();
        }
    }

    public void update2(int curr) {
        double updated = roundedPercentage(curr);
        if(updated != percent) {
            percent = updated;
            updateBar();
            print();
        }
    }

    public void update3(int curr) {
        percent = curr/max;
        updateBar();
        print();
    }

    public void updateBar() {
        double step = 1/(double) (bar.length()-2);

        for(int i = 0; i < bar.length()-2; i++) {
            if(percent >= (i+1)*step) bar.setCharAt(i+1, '=');
            else break;
        }
    }

    public void setSubject(Subject s) {
        if(s == null) return;
        this.s = s;
    }

    public void print() {
        System.out.print("\r" + bar + " " + (int) (percent*100) + "%");
    }

    public static void main(String[] args) {
        ProgressBar bar = new ProgressBar(20, 1000);

        for(int i = 0; i <= 1000; i++) {
            bar.update2(i);
            bar.print();
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
