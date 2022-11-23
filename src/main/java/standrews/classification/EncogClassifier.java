/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.persist.PersistError;
import standrews.aux.TimerMilli;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class EncogClassifier extends Classifier {

	private int[] layers;
	private int nIterations;

	private BasicNetwork network;

	public EncogClassifier(String filename) {
		super(filename);
		setParameters();
	}

	public EncogClassifier() {
		this(null);
	}

	private void setParameters() {
		layers = new int[]{120, 40};
		nIterations = 1000;
	}

	public void saveModel() {
		checkFilename();
		try {
			final File file = new File(filename);
			file.getParentFile().mkdirs();
			EncogDirectoryPersistence.saveObject(file, network);
		} catch (PersistError e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not save model, error saving to file: " +
					filename + "\n" + e);
			System.exit(1);
		}
	}

	public void loadModel() {
		checkFilename();
		try {
			final File file = new File(filename);
			if (!file.exists())
				throw new IOException("file does not exist");
			network = (BasicNetwork) EncogDirectoryPersistence.loadObject(file);
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not load model, error reading file: " +
					filename + "\n" + e);
			System.exit(1);
		}
	}

	public void train() {
		nPredictors = nAtomicPredictors();
		nResponses = seenResponses.size();
		responseLabels = seenResponses.toArray(new String[nResponses]);
		network = makeNetwork(nPredictors, nResponses);
		final MLDataSet dataSet = new BasicMLDataSet();
		for (Observation obs : observations) {
			System.exit(1);
//			final BasicMLData inputData = new BasicMLData(predictorsDoubleArray(obs.features));
			final BasicMLData outputData = new BasicMLData(responseDoubleArray(obs.response));
//			dataSet.add(inputData, outputData);
		}
		final MLTrain trainer = new ResilientPropagation(network, dataSet);
		final TimerMilli timer = new TimerMilli();
		timer.start();
		trainer.iteration(nIterations);
		trainer.finishTraining();
		timer.stop();
		reportFine("encog training took " + timer.seconds() + " s");
	}

	private BasicNetwork makeNetwork(final int nPredictors, final int nResponses) {
		final BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, nPredictors));
		for (final int n : layers) {
			network.addLayer(new BasicLayer(new ActivationSigmoid(), true, n));
		}
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, nResponses));
		network.getStructure().finalizeStructure();
		network.reset();
		return network;
	}

	public String predict(final Features feats) {
		final MLData input = new BasicMLData(predictorsDoubleArray(feats));
		final int index = network.classify(input);
		return responseLabels[index];
	}

	public String[] predictAll(final Features feats) {
		final MLData input = new BasicMLData(predictorsDoubleArray(feats));
		final double[] scores = network.compute(input).getData();
		return IntStream.range(0, nResponses)
				.boxed()
				.sorted((x, y) -> Double.compare(scores[y], scores[x]))
				.map(x -> responseLabels[x])
				.toArray(String[]::new);
	}


}
