/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import javafx.util.Pair;
import standrews.classification.FeatureVectorGenerator;
import standrews.constextract.SimpleExtractor;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.HatParser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
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
                    final String goldFile,
                    final String parseFile,
                    final int m,
                    final int n,
                    final SimpleExtractor extractor) {
//        final ConstTreebank subbank = treebank.part(m, m + n);

        writeTreebankToFile(treebank.getTestNegraTreebank(), goldFile);

        PrintWriter parsedWriter = null;
        try {
            parsedWriter = new PrintWriter(parseFile, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }

        reportFine("Testing model");

        Optional<Pair<List<ConstTree>, List<double[][]>>> miniBatchOptional = treebank.getNextTestMiniBatch();
        while (miniBatchOptional.isPresent()) {
            Pair<List<ConstTree>, List<double[][]>> miniBatch = miniBatchOptional.get();
            List<ConstTree> trees = miniBatch.getKey();
            List<double[][]> embeddingsList = miniBatch.getValue();

            for (int i = 0; i < trees.size(); i++) {
                ConstTree tree = trees.get(i);
                double[][] embeddings = embeddingsList.get(i);

                HatParser parser = makeParser(tree);
                ConstTree parsed = parser.parse(extractor, embeddings);

                parsedWriter.println(parsed);
            }

            miniBatchOptional = treebank.getNextTestMiniBatch();
        }
        parsedWriter.close();

//        String command = "discodop eval " + goldFile + " " + parseFile;
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

        return 0;
    }

    private void writeTreebankToFile(ConstTreebank treebank,
                                     final String corpusCopy) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(corpusCopy, "UTF-8");
            writer.print("" + treebank);
            writer.close();
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }
    }

    protected HatParser makeParser(final ConstTree tree) {
        return new HatParser(tree);
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
