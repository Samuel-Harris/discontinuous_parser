/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.aux;

import java.util.TreeMap;

public class Counter<T> extends TreeMap<T, Integer> {
    public void incr(final T t) {
        put(t, get(t) + 1);
    }

    @Override
    public Integer get(final Object t) {
        if (super.get(t) == null)
            return 0;
        else
            return super.get(t);
    }
}
