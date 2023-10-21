package com.org.view;

import com.org.observer.Observer;
import com.org.observer.Subject;

import java.util.concurrent.TimeUnit;

public class ProgressBar implements Observer {
    private StringBuilder bar;
    private double percent, max;
    private Subject<Integer> s;

    public ProgressBar(int length, int max) {
        bar = initializeBar(length);
        this.max = max;
    }

    public StringBuilder initializeBar(int length) {
        StringBuilder str = new StringBuilder();
        return str.append('[').append(" ".repeat(length)).append(']');
    }

    private double roundedPercentage(int curr) {
        return 0.0001*(Math.round((curr/max)/0.0001));
    }

    @Override
    public void update() {
        double updated = roundedPercentage(s.getState());
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
            if(percent >= (i+1)*step) bar.setCharAt(i+1, '#');
            else break;
        }
    }

    public void setSubject(Subject<Integer> s) {
        if(s == null) return;
        this.s = s;
    }

    public void print() {
        String out = String.format("\r%s %.2f%%\t", bar, percent*100);
        System.out.print(out);
        if(percent == 1.0) System.out.println();
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
