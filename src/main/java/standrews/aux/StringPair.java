/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.aux;

public class StringPair implements Comparable<StringPair> {
    public String s1;
    public String s2;

    public StringPair(String s1, String s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    public int hashCode() {
        return s1.hashCode() ^ s2.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof StringPair))
            return false;
        StringPair other = (StringPair) o;
        return this.s1.equals(other.s1) &&
                this.s2.equals(other.s2);
    }


    public int compareTo(StringPair other) {
        if (s1.compareTo(other.s1) != 0)
            return s1.compareTo(other.s1);
        else
            return s2.compareTo(other.s2);
    }

}
