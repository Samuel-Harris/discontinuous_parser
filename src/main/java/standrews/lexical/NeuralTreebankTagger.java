/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class NeuralTreebankTagger extends TreebankTagger {

	private int nHidden = 128;
	private int tbpttSize = 25;
	private double learningRate = 0.25;
	private int printIterations = 100;
	private int batchSize = 10;
	private int nEpochs = 20;
	// private ComputationGraph net;
	private MultiLayerNetwork net;

	private int nPad;
	private int[] padding;

	public NeuralTreebankTagger(final String corpus, final int len) {
		super(corpus, len);
		nPad = 0;
		padding = new int[nPad];
		for (int i = 0; i < nPad; i++)
			padding[i] = outLen + i;
		/*
		ComputationGraphConfiguration configuration = makeConf(len, outLen + nPad);
		makeNet(configuration);
		*/
		MultiLayerConfiguration conf = makeConf2(len, outLen + nPad);
		makeNet(conf);
		CustomIterator iterator = new CustomIterator();
		train(iterator);
	}

	private ComputationGraphConfiguration makeConf(final int inLen,
											   final int outLen) {
		final int seed = 1234;
		ComputationGraphConfiguration configuration = new NeuralNetConfiguration.Builder()
				.weightInit(WeightInit.XAVIER)
				.updater(new Adam(learningRate))
				.seed(seed)
				.graphBuilder()
				.tBPTTBackwardLength(tbpttSize)
				.tBPTTForwardLength(tbpttSize)
				/*
				.addInputs("wordIn", "shiftOut")
				.setInputTypes(InputType.recurrent(inLen), InputType.recurrent(outLen))
				.addLayer("encoder", new LSTM.Builder().nIn(inLen).nOut(nHidden).activation(Activation.SOFTSIGN).build(), "wordIn")
				.addVertex("lastTimeStep",
						new LastTimeStepVertex("wordIn"), "encoder")
				.addVertex("duplicateTimeStep",
						new DuplicateToTimeSeriesVertex("shiftOut"), "lastTimeStep")
				.addLayer("decoder",
						new LSTM.Builder().nIn(outLen + nHidden).nOut(nHidden).activation(Activation.SOFTSIGN).build(), "shiftOut", "duplicateTimeStep")
				.addLayer("output",
						new RnnOutputLayer.Builder().nIn(nHidden).nOut(outLen).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "decoder")
						*/
				.addInputs("wordIn")
				.setInputTypes(InputType.recurrent(inLen))
				.addLayer("encoder", new LSTM.Builder().nIn(inLen).nOut(nHidden).activation(Activation.SOFTSIGN).build(), "wordIn")
				// .addLayer("encoder", new Bidirectional.Builder().nIn(inLen).nOut(nHidden).activation("tanh").build(), "wordIn")
				.addLayer("output",
						new RnnOutputLayer.Builder().nIn(nHidden).nOut(outLen).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "encoder")
				.setOutputs("output")
				.build();
		return configuration;
	}

	private MultiLayerConfiguration makeConf2(final int inLen,
											   final int outLen) {
		final int seed = 1234;
		MultiLayerConfiguration conf2 = new NeuralNetConfiguration.Builder()
				.trainingWorkspaceMode(WorkspaceMode.ENABLED).inferenceWorkspaceMode(WorkspaceMode.ENABLED)
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.weightInit(WeightInit.XAVIER)
				.updater(new RmsProp.Builder().rmsDecay(0.95).learningRate(1e-2).build())
				.list()
				// .layer(0, new GravesLSTM.Builder().name("lstm1")
				.layer(0, new Bidirectional(new LSTM.Builder()
						.activation(Activation.TANH).nIn(inLen).nOut(nHidden/2).build()))
				// .layer(1, new GravesLSTM.Builder().name("lstm2")
						// .activation(Activation.TANH).nOut(80).build())
				.layer(1, new RnnOutputLayer.Builder().name("output")
						.activation(Activation.SOFTMAX).nIn(nHidden).nOut(outLen).lossFunction(LossFunctions.LossFunction.MSE)
						.build())
				.build();
		return conf2;
	}

	private void makeNet(ComputationGraphConfiguration configuration) {
		/*
		net = new ComputationGraph(configuration);
		net.init();
		net.setListeners(new ScoreIterationListener(printIterations));
		*/
	}

	private void makeNet(MultiLayerConfiguration configuration) {
		net = new MultiLayerNetwork(configuration);
		net.init();
		net.setListeners(new ScoreIterationListener(printIterations));
	}

	private class CustomIterator implements MultiDataSetIterator {
		private int current;
		public CustomIterator() {
			current = 0;
		}

		public boolean asyncSupported() {
        	return false;
    	}

		public MultiDataSetPreProcessor getPreProcessor() {
			return null;
		}

		public boolean hasNext() {
			// return current < Math.min(300, trainingSet.size());
			return current < trainingSet.size();
		}

		public MultiDataSet next() {
			return next(batchSize);
		}

		public MultiDataSet next(int num) {
			num = 1;
			int end = Math.min(current + num, trainingSet.size());
			List<INDArray> vecList = new ArrayList<>();
        	List<INDArray> shiftedPosList = new ArrayList<>();
        	List<INDArray> posList = new ArrayList<>();
			for (int e = current; e < end; e++) {
				Example ex = trainingSet.get(e);
				double[][] vecs = ex.in;
				int[] poss = ex.out;
				double[][] padBackVecs = padBackVecs(vecs, nPad);
				int[] padFrontPoss = padFrontPoss(poss, padding);
				int[] padBackPoss = padBackPoss(poss, padding);
				/*
				System.out.println("------");
				for (int i = 0; i < padBackVecs.length; i++) {
					for (int j = 0; j < padBackVecs[i].length; j++)
						System.out.print(" " + padBackVecs[i][j]);
					System.out.println();
				}
				System.out.println("\n------");
				for (int i = 0; i < padFrontPoss.length; i++) {
					System.out.print(" " + padFrontPoss[i]);
				}
				System.out.println("\n------");
				for (int i = 0; i < padBackPoss.length; i++) {
					System.out.print(" " + padBackPoss[i]);
				}
				System.out.println();
				*/
				double[][] frontPossOneHot = oneHotArray(padFrontPoss, outLen + nPad);
				double[][] backPossOneHot = oneHotArray(padBackPoss, outLen + nPad);
				INDArray vecArray = Nd4j.zeros(1, inLen, padBackVecs.length);
				for (int i = 0; i < padBackVecs.length; i++) {
					for (int j = 0; j < padBackVecs[i].length; j++) {
						// vecArray.putScalar(new int[]{0, j, i}, padBackVecs[i][j]);
						vecArray.putScalar(0, j, i, padBackVecs[i][j]);
					}
				}
				vecList.add(vecArray);
				INDArray shiftedPosArray = Nd4j.zeros(1, outLen + nPad, frontPossOneHot.length);
				for (int i = 0; i < frontPossOneHot.length; i++) {
					for (int j = 0; j < frontPossOneHot[i].length; j++) {
						// shiftedPosArray.putScalar(new int[]{0, j, i}, frontPossOneHot[i][j]);
						shiftedPosArray.putScalar(0, j, i, frontPossOneHot[i][j]);
					}
				}
				shiftedPosList.add(shiftedPosArray);
				// System.out.println(shiftedPosArray);
				INDArray posArray = Nd4j.zeros(1, outLen + nPad, backPossOneHot.length);
				for (int i = 0; i < backPossOneHot.length; i++) {
					for (int j = 0; j < backPossOneHot[i].length; j++) {
						// posArray.putScalar(new int[]{0, j, i}, backPossOneHot[i][j]);
						posArray.putScalar(0, j, i, backPossOneHot[i][j]);
					}
				}
				posList.add(shiftedPosArray);
				// shiftedPosList.add(Nd4j.create(new int[]{1}, frontPossOneHot));
				// posList.add(Nd4j.create(new int[]{1}, backPossOneHot));
				// System.out.println(Nd4j.create(padBackVecs));
			}
			INDArray vecSeq = vecList.get(0); // Nd4j.vstack(vecList);
			INDArray shiftedPosSeq = shiftedPosList.get(0); // .vstack(shiftedPosList);
			INDArray posSeq = posList.get(0); // Nd4j.vstack(posList);
			// System.out.println(vecSeq);
			INDArray[] inputs = new INDArray[]{vecSeq}; //, shiftedPosSeq};
			INDArray[] outputs = new INDArray[]{posSeq};
			current = end;
			return new org.nd4j.linalg.dataset.MultiDataSet(inputs, outputs);
		}

		public void reset() {
			current = 0;
		}

		public boolean resetSupported() {
			return false;
		}

		public void setPreProcessor(MultiDataSetPreProcessor preProcessor) {
    	}
    }

	private void train(CustomIterator iter) {
		for (int i = 0; i < nEpochs; i++) {
			net.fit(iter);
			iter.reset();
		}
	}

	protected int[] predictEncoded(final double[][] in) {
		double[][] inPadded = padBackVecs(in, nPad);
		net.rnnClearPreviousState();
		Vector<Integer> pos = new Vector<>();
		/*
		for (int i = 0; i < inPadded.length; i++) {
			double[] currentIn = inPadded[i];
			INDArray nextInput = Nd4j.create(currentIn, new int[] {1, inLen, 1});
			// INDArray b = net.feedForward(nextInput);
			// System.out.println(b);
			// double[][] padBackVecs = padBackVecs(in, nPad);
			// INDArray input = Nd4j.create(new double[][][]{padBackVecs});
			INDArray a = net.output(nextInput, false);
			// INDArray vecArray = Nd4j.zeros(1, inLen, padBackVecs.length);
			// INDArray a = net.feedForward(nextInput).get(0);
			// System.out.println(a.getRow(0));
			// INDArray[] a = net.rnnTimeStep(nextInput);
			if (i >= nPad)
				pos.add(maxIndex(a.getRow(0).toDoubleVector()));
		}
		*/
		INDArray vecArray = Nd4j.zeros(1, inLen, inPadded.length);
		for (int i = 0; i < inPadded.length; i++) {
			for (int j = 0; j < inPadded[i].length; j++) {
				vecArray.putScalar(0, j, i, inPadded[i][j]);
			}
		}
		INDArray a = net.output(vecArray, false);
		// System.out.println("len " + inPadded.length);
		// System.out.println(a);
		for (int i = 0; i < inPadded.length; i++) {
			// double[]
			System.out.println();
			pos.add(maxIndex(a.getRow(0).getColumn(i).toDoubleVector()));
		}
		/*
		double[] currentIn0 = inPadded[0];
		double[] currentIn1 = inPadded[1];
		double[] currentDecode0 = oneHot(pad0, outLen + nPad);
		double[] currentDecode1 = oneHot(pad0, outLen + nPad);
		INDArray inAr0 = Nd4j.create(currentIn0, new int[] {1, inLen, 1});
		INDArray inAr1 = Nd4j.create(currentIn1, new int[] {1, inLen, 1});
		INDArray inAr = Nd4j.vstack(inAr0, inAr1);
		INDArray decAr0 = Nd4j.create(currentDecode0, new int[] {1, outLen + nPad, 1});
		INDArray decAr1 = Nd4j.create(currentDecode1, new int[] {1, outLen + nPad, 1});
		INDArray decAr = Nd4j.vstack(decAr0, decAr1);
		net.feedForward(new INDArray[] {inAr, decAr}, false, false);
		org.deeplearning4j.nn.layers.recurrent.LSTM decoder =
				(org.deeplearning4j.nn.layers.recurrent.LSTM) net.getLayer("decoder");
		Layer output = net.getLayer("output");
		LayerWorkspaceMgr mgr = LayerWorkspaceMgr.noWorkspaces();
		INDArray decInp = decoder.getInput();
		System.out.println(decInp);
		*/

		/*
		INDArray decode = Nd4j.create(decodeArr, new int[] { 1, dict.size(), 1 });
		net.feedForward(new INDArray[] { in, decode }, false, false);
		LSTM decoder = net.getLayer("decoder");
		Layer output = net.getLayer("output");
		for (int i = 0; i < XX; i++) {
			mergeVertex.setInputs(decode, thoughtVector);
			INDArray merged = mergeVertex.doForward(false, mgr);
			INDArray activateDec = decoder.rnnTimeStep(merged, mgr);
            INDArray out = output.activate(activateDec, false, mgr);
		}
		*/
		return pos.stream().mapToInt(i->Math.min(i, outLen-1)).toArray();
	}
}
