/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;

import de.bwaldvogel.liblinear.*;
import standrews.aux.TimerMilli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class LibLinearClassifier extends Classifier {

	/**
	 * The model.
	 */
	private Model model;

	/**
	 * The parameters.
	 */
	private Parameter parameter;

	/**
	 * Indirection mapping of response value indices. Available after training.
	 */
	private int[] responseIndices;
	/**
	 * Scratch space for decision values. Used during prediction.
	 */
	private double[] decisionValues;

	public LibLinearClassifier(String filename) {
		super(filename);
		setParameters();
	}

	public LibLinearClassifier() {
		this(null);
	}

	/**
	 * Create the parameters for the SVM.
	 * This uses the default parameters from maltparser-1.9.0.
	 */
	private void setParameters() {
		// final SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL;
		final SolverType solver = SolverType.L2R_LR_DUAL;
		double C = 1.0;
		double eps = 1.0;
		parameter = new Parameter(solver, C, eps);
	}

	public void saveModel() {
		checkFilename();
		try {
			model.save(new File(filename));
		} catch (IOException e) {
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
			if (!Files.exists(Paths.get(filename)))
				throw new IOException("file does not exist");
			model = Model.load(new File(filename));
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not load model, error reading file: " +
					filename + "\n" + e);
			System.exit(1);
		}
	}

	public void train() {
		final Problem problem = createProblem();
		Linear.disableDebugOutput();
		final TimerMilli timer = new TimerMilli();
		timer.start();
		model = Linear.train(problem, parameter);
		timer.stop();
		reportFine("liblinear training took " + timer.seconds() + " s");

		nResponses = seenResponses.size();
		responseIndices = model.getLabels();
		responseLabels = seenResponses.toArray(new String[nResponses]);
		decisionValues = new double[nResponses];
	}

	private Problem createProblem() {
		final Problem problem = new Problem();
		problem.l = observations.size();
		problem.n = nAtomicPredictors();
//		problem.x = predictorsList();
		problem.y = responsesList();
		System.exit(1);
		return problem;
	}

//	private Feature[][] predictorsList() {
//		return observations.stream()
//				.map(obs -> predictorsToNode(obs.features))
//				.toArray(Feature[][]::new);
//	}

	private Feature[] predictorsToNode(Features feats) {
		final ArrayList<Double> vector = doubleVector(feats);
		final ArrayList<FeatureNode> nodes = new ArrayList<>();
		for (int i = 0; i < vector.size(); i++) {
			double val = vector.get(i);
			if (val != 0) {
				final FeatureNode node = new FeatureNode(i+1, val);
				nodes.add(node);
			}
		}
		return nodes.toArray(new Feature[nodes.size()]);
	}

	private double[] responsesList() {
		return observations.stream()
				.mapToDouble(obs -> responseToIndex(obs.response))
				.toArray();
	}

	private double responseToIndex(String response) {
		return 1.0 * seenResponses.headSet(response).size();
	}

	public String predict(final Features feats) {
		final Feature[] nodes = predictorsToNode(feats);
		final int index = (int) Linear.predict(model, nodes);
		return responseLabels[index];
	}

	public String[] predictAll(final Features feats) {
		final Feature[] nodes = predictorsToNode(feats);
		Linear.predictValues(model, nodes, decisionValues);
		return IntStream.range(0, nResponses)
				.boxed()
				.sorted((x, y) -> Double.compare(decisionValues[y], decisionValues[x]))
				.map(x -> responseLabels[responseIndices[x]])
				.toArray(String[]::new);
	}

}
