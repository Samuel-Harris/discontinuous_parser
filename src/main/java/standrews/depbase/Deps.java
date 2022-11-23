/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

/**
 * CoNLL-U token DEPS.
 * Note: In Finnish we see sometimes one token with two deprels,
 * so this is not a mapping.
 */
public class Deps {
    /**
     * May be null for an undefined value.
     */
    public final String depsStr;

    public Deps(final String depsStr) {
        if (depsStr.equals("_")) {
            this.depsStr = null;
        } else if (depsStr.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Wrong DEPS: " + depsStr);
        } else {
            this.depsStr = depsStr;
        }
    }

    @Override
    public String toString() {
        return depsStr == null ? "_" : depsStr;
    }
}
