/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

import org.deeplearning4j.datasets.iterator.IteratorDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import standrews.aux.TimerMilli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class DeeplearningClassifier extends Classifier {

	private int[] layers;
	private double learningRate;
	private int reportSize;

	private MultiLayerNetwork network;

	public DeeplearningClassifier(String filename) {
		super(filename);
		setParameters();
	}

	public DeeplearningClassifier() {
		this(null);
	}

	private void setParameters() {
		layers = new int[]{256, 256, 256};
		// layers = new int[]{512, 512, 512};
		learningRate = 0.005;
        reportSize = 10000;
	}

	public void saveModel() {
		checkFilename();
		final File file = new File(filename);
		file.getParentFile().mkdirs();
		// network.save(filename);
	}

	public void loadModel() {
		checkFilename();
		/*
		try {
			final File file = new File(filename);
			if (!file.exists())
				throw new IOException("file does not exist");
			network = (MultiLayerPerceptron) MultiLayerPerceptron.createFromFile(file);
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not load model, error reading file: " +
					filename + "\n" + e);
			System.exit(1);
		}
		*/
	}

	public void train() {
		if (!trainingStarted) {
			nPredictors = nAtomicPredictors();
			nResponses = seenResponses.size();
			responseLabels = seenResponses.toArray(new String[nResponses]);
			network = makeNetwork(nPredictors, nResponses);
			trainingStarted = true;
		}
		if (observations.isEmpty())
			return;
		List<DataSet> data = new ArrayList<>();
		/*
		PrintWriter writerTrain = null;
		PrintWriter writerTest = null;
		int m = 0;
		boolean w = true;
		if (w && responseLabels.length == 5) {
			try {
				writerTrain = new PrintWriter("tmp_train.csv", "UTF-8");
				writerTest = new PrintWriter("tmp_test.csv", "UTF-8");
			} catch (Exception e) {
			}
		}
		*/
		for (final Observation obs : observations) {
			final double[] inputData = predictorsDoubleArray(obs.features);
			final double[] outputData = responseDoubleArray(obs.response);
			DataSet example = new DataSet(Nd4j.create(inputData),
					Nd4j.create(outputData));
			data.add(example);
			/*
			if (w && responseLabels.length == 5) {
				// System.out.println(inputData.length);
				int ind = -1;
				for (int i = 0; i < responseLabels.length; i++)
					if (responseLabels[i].equals(obs.response))
						ind = i;
				// if (m++ % 10000 > 0) {
					writerTrain.print(ind);
					for (int i = 0; i < inputData.length; i++)
						writerTrain.print("," + inputData[i]);
					writerTrain.println();

				} else {
					writerTest.print(ind);
					for (int i = 0; i < inputData.length; i++)
						writerTest.print("," + inputData[i]);
					writerTest.println();
				}

			}
			*/
		}
		/*
		if (w && responseLabels.length == 5) {
			writerTrain.close();
			writerTest.close();
		}
		*/
		// DataSetIterator iter = new IteratorDataSetIterator(data.iterator(), batchSize);
		/*
		RecordReader rr = new CSVRecordReader();
		try {
			rr.initialize(new FileSplit(new File("tmp_train.csv")));
		} catch (Exception e) {}
		DataSetIterator iterAlt = null;
		if (responseLabels.length == 555)
			iterAlt = new RecordReaderDataSetIterator(rr, batchSize,0, nResponses);
			*/
        // while(trainIter.hasNext()) { DataSet d = trainIter.next();  System.out.println(d);}
		/*
		while(iter.hasNext()) {
			if (iterAlt != null) {
				DataSet d = iter.next();
				System.out.println(d);
				DataSet dAlt = iterAlt.next();
				System.out.println("ALT " + dAlt);
			}
		}
		*/
		/*
		iter.reset();
		if (iterAlt != null)
			iterAlt.reset();
			*/

		/*
		RecordReader rr = new CSVRecordReader();
		try {
			rr.initialize(new FileSplit(new File("tmp_train.csv")));
		} catch (Exception e) {}
		DataSetIterator trainIter = new RecordReaderDataSetIterator(rr, batchSize,0, responseLabels.length);
		*/
		if (continuousTraining) {
			DataSetIterator iter = new IteratorDataSetIterator(data.iterator(), batchSize);
			network.fit(iter);
		} else {
			final TimerMilli timer = new TimerMilli();
			timer.start();
			for (int n = 0; n < nEpochs; n++) {
				// System.out.println("n " + n);
				// if (iterAlt != null) {
				// network.fit(iterAlt);
				// iterAlt.reset();
				// } else {
				DataSetIterator iter = new IteratorDataSetIterator(data.iterator(), batchSize);
				network.fit(iter);
				// iter.reset();
				// }
				// network.fit(trainIter);
			}
			timer.stop();
			reportFine("Deeplearning training took " + timer.seconds() + " s");
		}
		observations.clear();
	}

	private MultiLayerNetwork makeNetwork(final int nPredictors, final int nResponses) {
		final int[] layersAll = new int[layers.length + 2];
		layersAll[0] = nPredictors;
		System.arraycopy(layers, 0, layersAll, 1, layers.length);
		layersAll[layersAll.length-1] = nResponses;
		int seed = 123;
        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Nesterovs(learningRate, 0.7))
                .list();
        for (int n = 1; n < layersAll.length; n++) {
        	boolean last = n == layersAll.length - 1;
			builder = builder.layer(n - 1,
					(last ? new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) :
							new DenseLayer.Builder())
					.nIn(layersAll[n - 1]).nOut(layersAll[n])
					.weightInit(WeightInit.XAVIER)
					.activation(last ? Activation.SOFTMAX : Activation.RELU)
					.build());
		}
		MultiLayerConfiguration conf = builder.build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        network.setListeners(new ScoreIterationListener(reportSize));
		return network;
	}

	public String predict(final Features feats) {
		return predictAll(feats)[0];
	}

	public String[] predictAll(final Features feats) {
		final double[] inputData = predictorsDoubleArray(feats);
		INDArray in = Nd4j.create(inputData);
		INDArray prediction = network.output(in, false);
		final double[] scores = prediction.toDoubleVector();
		return IntStream.range(0, nResponses)
				.boxed()
				.sorted((x, y) -> Double.compare(scores[y], scores[x]))
				.map(x -> responseLabels[x])
				.toArray(String[]::new);
	}

	public double probability(final Features feats, String response) {
		final double[] inputData = predictorsDoubleArray(feats);
		INDArray in = Nd4j.create(inputData);
		INDArray prediction = network.output(in, false);
		final double[] scores = prediction.toDoubleVector();
		double norm = DoubleStream.of(scores).sum();
		int i = Arrays.asList(responseLabels).indexOf(response);
		return scores[i] / norm;
	}

}
