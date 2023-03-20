package util;

/*
refactor pre-/postcondition checks
 */
public class Checker {
    public void checkNotNull(Object o) {
        if(o == null) throw new IllegalArgumentException("object " + o + "is null");
    }

    public void checkStringNotEmpty(String str) {
        if(str.isEmpty()) throw new IllegalArgumentException("string can't be empty");
    }
}
