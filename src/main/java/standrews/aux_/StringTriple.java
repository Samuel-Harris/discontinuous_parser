/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.aux_;

public class StringTriple implements Comparable<StringTriple> {
    public String s1;
    public String s2;
    public String s3;

    public StringTriple(String s1, String s2, String s3) {
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
    }

    public int hashCode() {
        return s1.hashCode() ^ s2.hashCode() ^ s3.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof StringTriple))
            return false;
        StringTriple other = (StringTriple) o;
        return this.s1.equals(other.s1) &&
                this.s2.equals(other.s2) &&
                this.s3.equals(other.s3);
    }

    public int compareTo(StringTriple other) {
        if (s1.compareTo(other.s1) != 0)
            return s1.compareTo(other.s1);
        else if (s2.compareTo(other.s2) != 0)
            return s2.compareTo(other.s2);
        else
            return s3.compareTo(other.s3);
    }

}
