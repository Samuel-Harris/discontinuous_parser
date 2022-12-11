/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.SimpleConfig;
import standrews.aux_.DataCollectionSum;

import standrews.aux_.TimerNano;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.depbase.generation.DependencyStructureGenerator;

import java.util.Random;
import java.util.TreeMap;

public class SimpleDynamicParserTester extends SimpleDynamicParser {

    private final SimpleDynamicParserLinear linear;
    private final SimpleDynamicParserQuadratic quadratic;
    private final SimpleDynamicParserCubicSimple simple;
    private final SimpleDynamicParserCubicComplex complex;

    private final DataCollectionSum timings;

    private static TimerNano timer = new TimerNano();

    public SimpleDynamicParserTester(final Token[] tokens,
                                     final boolean leftDependentsFirst,
                                     final boolean strict,
                                     final SimpleExtractor preliminaryExtractor,
                                     final DataCollectionSum timings) {
        super(tokens, leftDependentsFirst, strict, preliminaryExtractor);
        this.timings = timings;
        // this.repeat = repeat;
        linear = new SimpleDynamicParserLinear(tokens, leftDependentsFirst, strict,
                preliminaryExtractor);
        quadratic = new SimpleDynamicParserQuadratic(tokens, leftDependentsFirst, strict,
                preliminaryExtractor);
        simple = new SimpleDynamicParserCubicSimple(tokens, leftDependentsFirst, strict,
                preliminaryExtractor);
        complex = new SimpleDynamicParserCubicComplex(tokens, leftDependentsFirst, strict,
                preliminaryExtractor);
        linear.prepareTraining();
        quadratic.prepareTraining();
        simple.prepareTraining();
        complex.prepareTraining();
    }

    public void setProjective(final boolean b) {
        super.setProjective(b);
        linear.setProjective(b);
        quadratic.setProjective(b);
        simple.setProjective(b);
        complex.setProjective(b);
    }

    /**
     * @param extractor Extractor of features.
     */
    public void observe(final SimpleExtractor extractor) {
        prepareTraining();
        final SimpleConfig config = makeInitialConfig(tokens);
        while (!config.isFinal()) {
            if (config.prefixLength() >= 2) {
                scores(config);
            }
            /* don't bother with rest */

            final String[] stepAction = getStepAction(config);
            if (stepAction.length == 0) {
                fail("training", config);
                break;
            }
            apply(config, stepAction);
        }
    }

    protected TreeMap<String, Integer> scores(final SimpleConfig config) {
		/*
		int linRepeat = config.totalLength() < 10 ?
				repeat * 2 :
				config.totalLength() >= 50 ? repeat * 50 :
				repeat * 10;
		int quadRepeat = config.totalLength() < 10 ?
				repeat * 2 :
				config.totalLength() >= 50 ? repeat * 10 :
				repeat * 5;
				// int extraRepeat = repeat;
				*/
        // int customRepeat = config.prefixLength() < 5 ? 10000 : 5000;
        int length = config.totalLength();
        int nNeeded = 10000000;
        int minTime = 10;
        int maxTime = 10000000;
        TreeMap<String, Integer> outScores = null;

        // System.out.println(timings.nObservations("linear", length));

        if (timings.nObservations("linear", length) < nNeeded && 5 > 3) {
            if (isProjective) {
                double t = Long.MAX_VALUE;
                int repeat = 10;
                int retry = 0;
                while (t > maxTime && retry < 10) {
                    timer.init();
                    for (int i = 0; i < repeat; i++)
                        outScores = linear.scores(config);
                    t = timer.stopMsec();
                    if (t == 0)
                        System.out.println("underflow");
                    // repeat *= 2;
                    retry++;
                    if (t > maxTime)
                        System.out.println("" + retry + " " + t);
                }
                double timeLinear = t / repeat;
                timings.add("linear", length, timeLinear);
            }
        }

        if (timings.nObservations("quadratic", length) < nNeeded && 5 > 3) {
            if (isProjective) {
                double t = Long.MAX_VALUE;
                int repeat = 10;
                int retry = 0;
                while (t > maxTime && retry < 10) {
                    timer.init();
                    for (int i = 0; i < repeat; i++)
                        outScores = quadratic.scores(config);
                    t = timer.stopMsec();
                    if (t == 0)
                        System.out.println("underflow");
                    // repeat *= 2;
                    retry++;
                    if (t > maxTime)
                        System.out.println("" + retry + " " + t);
                }
                double timeQuadratic = t / repeat;
                timings.add("quadratic", length, timeQuadratic);
            }
        }

        if (timings.nObservations("simple", length) < nNeeded && 5 > 3) {
            double t = Long.MAX_VALUE;
            int repeat = 1;
            int retry = 0;
            while (t > maxTime * 100 && retry < 10) {
                timer.init();
                for (int i = 0; i < repeat; i++)
                    outScores = simple.scores(config);
                t = timer.stopMsec();
                // repeat *= 2;
                retry++;
                if (t > maxTime * 100)
                    System.out.println("" + retry + " " + t);
            }
            double timeSimple = t / repeat;
            timings.add("simple", length, timeSimple);
        }

		/*
		timerComplex.init();
		for (int i = 0; i < repeat; i++)
			complex.scores(config);
		final double timeComplex = 1.0 * timerComplex.stop() / repeat;
		*/

        // System.out.println(timeLinear);
        // System.out.println(timeQuadratic);
        // System.out.println(timeSimple);
        // System.out.println("len " + config.totalLength());
        // timings.add("complex " + config.totalLength(), timeComplex);

        // System.out.println("linear " + timeLinear);
        // System.out.println("quadratic " + timeQuadratic);
        // System.out.println("simple " + timerSimple);
        // System.out.println("complex " + timeComplex);
        // if (outScores == null)
        // outScores = linear.scores(config);
        // outScores = simple.scores(config);
        // final TreeMap<String, Integer> quadraticScores = quadratic.scores(config);

        // final TreeMap<String, Integer> complexScores = complex.scores(config);
        // final String linearStr = toString(linearScores);
        // final String quadraticStr = toString(quadraticScores);
        // final String simpleStr = toString(simpleScores);
        // final String complexStr = toString(complexScores);
		/*
		if (!linearScores.equals(quadraticScores)) {
			if (config.prefixLength() + config.suffixLength() < 20) {
				System.out.println("linear " + linearStr);
				System.out.println("quadratic " + quadraticStr);
				printConfig(config);
				System.out.println();
			}
		}
		*/

        if (isProjective) {
            final TreeMap<String, Integer> linearScores = linear.scores(config);
            final TreeMap<String, Integer> simpleScores = simple.scores(config);
            if (!linearScores.equals(simpleScores)) {
                if (config.prefixLength() + config.suffixLength() < 80) {
                    final String linearStr = toString(linearScores);
                    System.out.println("linear " + linearStr);
                    final String simpleStr = toString(simpleScores);
                    System.out.println("simple " + simpleStr);
                    printConfig(config);
                    System.out.println();
                }
            }
        }

		/*
		if (!simpleScores.equals(complexScores)) {
			if (config.prefixLength() + config.suffixLength() < 20) {
				System.out.println("simple " + simpleStr);
				System.out.println("complex " + complexStr);
				printConfig(config);
				System.out.println();
			}
		}
		*/
        return outScores;
    }

    public static void main(String[] args) {
        DataCollectionSum timings = new DataCollectionSum();
        final boolean random = true;

		/*
		final boolean leftFirst = true;
		final boolean strict = false;
		test(leftFirst, strict, random);
		*/

        // for (boolean leftFirst : new boolean[]{true}) {
        for (boolean leftFirst : new boolean[]{false, true}) {
            // for (boolean strict : new boolean[]{true}) {
            for (boolean strict : new boolean[]{false, true}) {
                System.out.println("leftFirst=" + leftFirst + " strict=" + strict);
                test(leftFirst, strict, random, timings);
            }
        }
        timings.list(3);
    }

    private static void test(final boolean leftFirst, final boolean strict, final boolean random,
                             final DataCollectionSum timings) {
        final DependencyStructureGenerator gen = new DependencyStructureGenerator(20, 0.55);
        if (random) {
            Random r = new Random();
            for (int i = 0; i < 100000; i++) {
                // for (int i = 0; i < 1000; i++) {
                Token[] struct = gen.generateDepStruct();
                SimpleDynamicParserTester parser = new SimpleDynamicParserTester(
                        struct, leftFirst, strict, null, timings);
                parser.prepareTraining();
                parser.setProjective(true);
                final SimpleConfig config = parser.makeInitialConfig(struct);
                for (int j = 0; j < (struct.length - 1) / 2; j++)
                    // for (int j = 0; j < struct.length; j++)
                    parser.shift(config);
                parser.getAction(config);
                int nRemove = r.nextInt(5);
                while (nRemove > 0 && config.prefixLength() > 1) {
                    int rem = r.nextInt(config.prefixLength() - 1) + 1;
                    config.removePrefixLeft(rem);
                    nRemove--;
                }
            }
        } else {
            int[][] edges = new int[][]{
                    {1, 2},
                    {2, 0},
                    {3, 2}
                    // {2, 9},
                    // {3, 7},
                    // {4, 5},
                    // {5, 3},
                    // {6, 3},
                    // {7, 2},
                    // {8, 9},
                    // {9, 0}
                    // {10,12},
                    //{11,10},
                    //{12,4},
                    //{13,14},
                    //{14,16},
                    //{15,14},
                    //{16,12}
            };
            Token[] struct = gen.generateDepStruct(edges);
            SimpleDynamicParserTester parser = new SimpleDynamicParserTester(
                    struct, leftFirst, strict, null, timings);
            parser.prepareTraining();
            final SimpleConfig config = parser.makeInitialConfig(struct);
            for (int j = 0; j < (struct.length - 1) / 2; j++)
                // for (int j = 0; j < struct.length; j++)
                parser.shift(config);
            parser.getAction(config);
        }
    }
}
