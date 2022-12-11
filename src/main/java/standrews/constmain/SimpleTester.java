/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureSpecification;
import standrews.classification.FeatureVectorGenerator;
import standrews.constbase.EmbeddingsBank;
import standrews.constextract.SimpleExtractor;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.SimpleParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class SimpleTester {
    protected FeatureVectorGenerator featureVectorGenerator;

    public SimpleTester(final FeatureVectorGenerator featureVectorGenerator) {
        this.featureVectorGenerator = featureVectorGenerator;
    }

	/*
	public int test(final ConstTreebank treebank,
					final ConstTreebank treebankParsed,
					final SimpleExtractor extractor) {
		return test(treebank, null, treebankParsed, extractor);
	}
	*/

    public int test(final ConstTreebank treebank,
                    final String corpusCopy,
                    final String corpusParsed,
                    final int m,
                    final int n,
                    final SimpleExtractor extractor) {
        final ConstTreebank subbank = treebank.part(m, m + n);
        copyTraining(subbank, corpusCopy);
        PrintWriter parsedWriter = null;
        try {
            parsedWriter = new PrintWriter(corpusParsed, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }
        int i = 0;
        for (ConstTree gold : subbank.getTrees()) {
            DeterministicParser parser = makeParser(gold);
            ConstTree parsed = parser.parse(extractor);
            assert parsedWriter != null;
            parsedWriter.println(parsed);
            i++;
        }
        assert parsedWriter != null;
        parsedWriter.close();

//        String command = "discodop eval " + corpusCopy + " " + corpusParsed;
//
//        try {
//            Process process = Runtime.getRuntime().exec(command);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//
//            reader.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return i;
    }

    private void copyTraining(ConstTreebank treebank,
                              final String corpusCopy) {
        PrintWriter trainWriter = null;
        try {
            if (corpusCopy != null)
                trainWriter = new PrintWriter(corpusCopy, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }
        trainWriter.print("" + treebank);
        trainWriter.close();
    }

    protected DeterministicParser makeParser(final ConstTree tree) {
        return new SimpleParser(tree);
    }

    private static Logger logger() {
        final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
        log.setParent(Logger.getGlobal());
        return log;
    }

    /**
     * Report failure.
     *
     * @param message The thing that failed.
     */
    protected static void fail(final String message) {
        final Logger log = logger();
        log.setParent(Logger.getGlobal());
        log.severe(message);
    }

    /**
     * Report fine comment.
     *
     * @param message The message.
     */
    private static void reportFine(final String message) {
        logger().fine(message);
    }

}
