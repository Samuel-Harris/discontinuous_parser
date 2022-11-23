/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import standrews.aux.TimerMilli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class LibSVMClassifier extends Classifier {

	/**
	 * The SVM model.
	 */
	private svm_model model;

	/**
	 * The parameters.
	 */
	private svm_parameter parameters;

	/**
	 * Indirection mapping of response value indices. Available after training.
	 */
	private int[] responseIndices;
	/**
	 * Scratch space for decision values. Used during prediction.
	 */
	private double[] decisionValues;

	public LibSVMClassifier(String filename) {
		super(filename);
		setParameters();
	}

	public LibSVMClassifier() {
		this(null);
	}

	/**
	 * Create the parameters for the SVM.
	 * This uses the default parameters from maltparser-1.9.0.
	 */
	private void setParameters() {
		parameters = new svm_parameter();

		parameters.svm_type = svm_parameter.C_SVC;
		parameters.kernel_type = svm_parameter.RBF;

		parameters.degree = 2;
		parameters.gamma = 0.2;
		parameters.coef0 = 0;
		parameters.nu = 0.5;

		parameters.cache_size = 100;

		parameters.C = 1;
		parameters.eps = 0.1;
		parameters.p = 0.1;

		// Changed from 1
		parameters.shrinking = 0;
		parameters.probability = 0;

		parameters.nr_weight = 0;
		parameters.weight_label = new int[0];
		parameters.weight = new double[0];
	}

	private class nullPrinter implements libsvm.svm_print_interface {
		public void print(String s) {};
	}

	public void saveModel() {
		checkFilename();
		try {
			svm.svm_save_model(filename + ".txt", model);
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
			model = svm.svm_load_model(filename);
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not load model, error reading file: " +
					filename + "\n" + e);
			System.exit(1);
		}
	}

	public void train() {
		final svm_problem problem = createProblem();
		checkParameterError(problem, parameters);
		svm.svm_set_print_string_function(new nullPrinter());
		final TimerMilli timer = new TimerMilli();
		timer.start();
		model = svm.svm_train(problem, parameters);
		timer.stop();
		reportFine("libsvm training took " + timer.seconds() + " s");

		nResponses = seenResponses.size();
		responseIndices = new int[nResponses];
		svm.svm_get_labels(model, responseIndices);
		responseLabels = seenResponses.toArray(new String[nResponses]);
		decisionValues = new double[nResponses * (nResponses - 1) / 2];
	}

	private svm_problem createProblem() {
		final svm_problem problem = new svm_problem();
		problem.l = observations.size();
		System.exit(1);
//		problem.x = predictorsList();
		problem.y = responsesList();
		return problem;
	}

//	private svm_node[][] predictorsList() {
//		return observations.stream()
//				.map(obs -> predictorsToNode(obs.features))
//				.toArray(svm_node[][]::new);
//	}

	private svm_node[] predictorsToNode(Features feats) {
		final ArrayList<Double> vector = doubleVector(feats);
		final ArrayList<svm_node> nodes = new ArrayList<>();
		for (int i = 0; i < vector.size(); i++) {
			double val = vector.get(i);
			if (val != 0) {
				final svm_node node = new svm_node();
				node.index = i + 1;
				node.value = val;
				nodes.add(node);
			}
		}
		return nodes.toArray(new svm_node[nodes.size()]);
	}

	private double[] responsesList() {
		return observations.stream()
				.mapToDouble(obs -> responseToIndex(obs.response))
				.toArray();
	}

	private double responseToIndex(String response) {
		return 1.0 * seenResponses.headSet(response).size();
	}

	/**
	 * Validate the parameters for a problem.
	 *
	 * @param problem    The problem for which the parameters are.
	 * @param parameters The parameters to validate.
	 */
	private void checkParameterError(final svm_problem problem, final svm_parameter parameters) {
		final String error = svm.svm_check_parameter(problem, parameters);
		if (error != null) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe(error);
			System.exit(1);
		}
	}

	public String predict(final Features feats) {
		final svm_node[] nodes = predictorsToNode(feats);
		final int index = (int) svm.svm_predict_values(model, nodes, decisionValues);
		return responseLabels[index];
	}

	public String[] predictAll(final Features feats) {
		final svm_node[] nodes = predictorsToNode(feats);
		svm.svm_predict_values(model, nodes, decisionValues);
		final int scores[] = new int[nResponses];
		int i = 0;
		for (int k = 0; k < nResponses; k++) {
			for (int m = k + 1; m < nResponses; m++) {
				final double comp = decisionValues[i];
				if (comp > 0)
					scores[k]++;
				else
					scores[m]++;
				i++;
			}
		}
		return IntStream.range(0, nResponses)
				.boxed()
				.sorted((x, y) -> Integer.compare(scores[y], scores[x]))
				.map(x -> responseLabels[responseIndices[x]])
				.toArray(String[]::new);
	}

}
