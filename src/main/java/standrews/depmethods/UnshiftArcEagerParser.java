/*
 * Copyright (c) 2021. University of St Andrews
 */

package standrews.depmethods;

/**
 * SHIFT:
 * alpha | b beta => alpha b^L | beta
 * RIGHTARC:
 * alpha a | b beta => alpha a b^R | beta
 * LEFTARC:
 * alpha a^L | b beta => alpha | b beta
 * REDUCE:
 * alpha a b^R | beta => alpha a | beta
 */
public class UnshiftArcEagerParser extends DeterministicParser {



}
