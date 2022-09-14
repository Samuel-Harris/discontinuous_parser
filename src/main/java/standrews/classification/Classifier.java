/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import standrews.aux.StringFrequencies;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class Classifier {

	/**
	 * The file name used for saving and loading the classifier.
	 * Null if the classifier does not need to be saved and loaded.
	 */
	protected final String filename;

	/**
	 * The values for the features that were seen in string-to-string
	 * mapping.
	 */
	protected final SortedMap<String,TreeSet<String>> stringSeenValues;

	/**
	 * How often have values been seen.
	 */
	protected final SortedMap<String,StringFrequencies> stringFreqValues;

	/**
	 * The features in string-to-string mapping that were associated with null values.
	 */
	protected final SortedSet<String> stringNullValues;

	/**
	 * The values for the features that were seen in string-to-set
	 * mapping.
	 */
	protected final SortedMap<String,TreeSet<String>> setSeenValues;

	/**
	 * How often have values been seen.
	 */
	protected final SortedMap<String,StringFrequencies> setFreqValues;

	/**
	 * Vector length. Assumed to be always the same.
	 */
	protected final SortedMap<String,Integer> vectorLength;

	/**
	 * The response values that were seen.
	 */
	protected final SortedSet<String> seenResponses;

	/**
	 * How often have responses been seen.
	 */
	protected final StringFrequencies freqResponses;

	/**
	 * Number of predictors. Available once training has started.
	 */
	protected int nPredictors;
	/**
	 * Number of response values. Available once training has started.
	 */
	protected int nResponses;
	/**
	 * Labels of responses. Available once training has started.
	 */
	protected String[] responseLabels;

	/**
	 * The observations for training.
	 */
	protected final List<Observation> observations;

	/**
	 * Should a null feature value be treated as a value like any other?
	 */
	protected boolean treatNullAsValue;

	/**
	 * Has (first) training been done?
	 */
	protected boolean trainingStarted;

	/**
	 * Is training continuous, with training happening after each batch of observations?
	 */
	protected boolean continuousTraining;

	/**
	 * How many observations are taken together. Only relevant for certain classifiers and with
	 * continuousTraining.
	 */
	protected int batchSize;

	/**
	 * For suitable classifiers, how often should learning be repeated.
	 */
	protected int nEpochs;

	/**
	 * Construct classifier.
	 * @param filename Filename.
	 */
	public Classifier(String filename) {
		this.filename = filename;
		stringSeenValues = new TreeMap<>();
		stringFreqValues = new TreeMap<>();
		stringNullValues = new TreeSet<>();
		setSeenValues = new TreeMap<>();
		setFreqValues = new TreeMap<>();
		vectorLength = new TreeMap<>();
		seenResponses = new TreeSet<>();
		freqResponses = new StringFrequencies();
		observations = new ArrayList<>();
		treatNullAsValue = false;
		trainingStarted = false;
		continuousTraining = false;
		batchSize = 200;
		nEpochs = 1;
	}

	public Classifier() {
		this(null);
	}

	public void setTreatNullAsValue(final boolean b) {
		treatNullAsValue = b;
	}

	public void setContinuousTraining(final boolean b) {
		continuousTraining = b;
	}

	public void setTrainingStarted(final boolean b) {
		trainingStarted = b;
	}

	public void setBatchSize(final int n) {
		batchSize = n;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setNEpochs(final int n) {
		nEpochs = n;
	}

	public int getNEpochs() {
		return nEpochs;
	}

	/**
	 * Store model and interpretation of predictors and responses.
	 */
	public void save() {
		saveSymbols();
		saveModel();
	}

	/**
	 * Load model and interpretation of predictors and responses.
	 */
	public void load() {
		loadSymbols();
		loadModel();
	}

	/**
	 * Save trained model.
	 */
	public abstract void saveModel();

	/**
	 * Load trained model.
	 */
	public abstract void loadModel();

	/**
	 * Check that filename is provided and report error otherwise.
	 */
	protected void checkFilename() {
		if (filename == null) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("No filename provided for classifier");
			System.exit(1);
		}
	}

	/**
	 * Add observation consisting of predictors and response.
	 * @param feats Predictors.
	 * @param response Response value.
	 */
	public void addObservation(final Features feats, final String response) {
		if (!trainingStarted) {
			for (String feat : feats.stringKeys()) {
				addStringValue(feat, feats.stringVal(feat));
			}
			for (String feat : feats.setKeys()) {
				addSetValues(feat, feats.setVal(feat));
			}
			for (String feat : feats.vectorKeys()) {
				setVectorLength(feat, feats.vectorVal(feat).length);
			}
			seenResponses.add(response);
			freqResponses.add(response);
		}
		observations.add(new Observation(feats, response));
		if (continuousTraining && observations.size() >= batchSize)
			train();
	}

	/**
	 * Add possible value of string predictor, without observation.
	 */
	public void addStringValue(final String feat, final String val) {
		if (!stringSeenValues.containsKey(feat)) {
			stringSeenValues.put(feat, new TreeSet<>());
			stringFreqValues.put(feat, new StringFrequencies());
		}
		if (val == null)
			stringNullValues.add(feat);
		else
			stringSeenValues.get(feat).add(val);
		stringFreqValues.get(feat).add(val);
	}

	/**
	 * Add possible values of set predictor, without observation.
	 */
	public void addSetValues(final String feat, final Collection<String> vals) {
		if (!setSeenValues.containsKey(feat)) {
			setSeenValues.put(feat, new TreeSet<>());
			setFreqValues.put(feat, new StringFrequencies());
		}
		setSeenValues.get(feat).addAll(vals);
		for (String val : vals) {
			setFreqValues.get(feat).add(val);
		}
	}

	/**
	 * Set length of vector for feature.
	 * Should be the same as any length for the same feature observed before.
	 */
	public void setVectorLength(final String feat, final int len) {
		if (!vectorLength.containsKey(feat))
			vectorLength.put(feat, len);
		else if (vectorLength.get(feat) != len) {
			reportFail("Inconsistent length for " + feat + " " + len +
			" and " + vectorLength.get(feat));
		}
	}

	/**
	 * Add response value.
	 */
	public void addResponseValue(final String response) {
		seenResponses.add(response);
		freqResponses.add(response);
	}

	/**
	 * Feature values as vector of doubles.
	 * @param feats
	 * @return
	 */
	protected ArrayList<Double> doubleVector(Features feats) {
		final ArrayList<Double> vector = new ArrayList<>();
		for (String feat : feats.stringKeys()) {
			String val = feats.stringVal(feat);
			for (String elem : stringSeenValues.get(feat))
				vector.add(elem.equals(val) ? 1.0 : 0.0);
			if (treatNullAsValue && stringNullValues.contains(feat))
				vector.add(val == null ? 1.0 : 0.0);
		}
		for (String feat : feats.setKeys()) {
			Set<String> vals = feats.setVal(feat);
			for (String elem : setSeenValues.get(feat))
				vector.add(vals.contains(elem) ? 1.0 : 0.0);
		}
		for (String feat : feats.vectorKeys()) {
			double[] vec = feats.vectorVal(feat);
			List<Double> list = Arrays.stream(vec).boxed().collect(Collectors.toList());
			vector.addAll(list);
		}
		return vector;
	}


	protected double[] predictorsDoubleArray(Features feats) {
		final ArrayList<Double> vector = doubleVector(feats);
		return vector.stream().mapToDouble(d -> d).toArray();
	}

	protected double[] responseDoubleArray(String response) {
		final ArrayList<Double> vector = new ArrayList<>();
		for (String elem : responseLabels)
			vector.add(elem.equals(response) ? 1.0 : 0.0);
		return vector.stream().mapToDouble(d -> d).toArray();
	}

	/**
	 * Get number of observations.
	 */
	public int nObservations() {
		return observations.size();
	}

	/**
	 * Get number of atomic predictors (true or false).
	 */
	public int nAtomicPredictors() {
		int n = 0;
		for (String feat : stringSeenValues.keySet()) {
			n += stringSeenValues.get(feat).size();
			if (treatNullAsValue && stringNullValues.contains(feat))
				n++;
		}
		for (String feat : setSeenValues.keySet()) {
			n += setSeenValues.get(feat).size();
		}
		for (String feat : vectorLength.keySet()) {
			n += vectorLength.get(feat);
		}
		return n;
	}

	public abstract void train();

	public abstract String predict(final Features feats);

	public abstract String[] predictAll(final Features feats);

	public double probability(final Features feats, String response) {
		reportFail("Probabilities not defined for this classifier");
		return 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (String feat : stringSeenValues.keySet()) {
			buf.append(feat + "->");
			for (String val : stringSeenValues.get(feat))
				buf.append(" " + val);
			if (stringNullValues.contains(feat))
				buf.append(" <<can be null>>");
			buf.append("\n");
		}
		for (String feat : setSeenValues.keySet()) {
			buf.append(feat + "->");
			for (String val : setSeenValues.get(feat))
				buf.append(" " + val);
			buf.append("\n");
		}
		for (String response : seenResponses)
			buf.append(" " + response);
		return buf.toString();
	}

	public String toFrequencies() {
		StringBuffer buf = new StringBuffer();
		for (String feat : stringFreqValues.keySet()) {
			final StringFrequencies freqs = stringFreqValues.get(feat);
			buf.append(feat + "->");
			for (String val : stringFreqValues.get(feat).keySet()) {
				int f = freqs.get(val);
				buf.append(" " + val + "[" + f + "]");
			}
			int fNull = freqs.getNull();
			if (fNull > 0)
				buf.append(" <<null>>[" + fNull + "]");
			buf.append("\n");
		}
		for (String feat : setFreqValues.keySet()) {
			final StringFrequencies freqs = setFreqValues.get(feat);
			buf.append(feat + "->");
			for (String val : setFreqValues.get(feat).keySet()) {
				int f = freqs.get(val);
				buf.append(" " + val + "[" + f + "]");
			}
		}
		for (String response : freqResponses.keySet()) {
			int f = freqResponses.get(response);
			buf.append(" " + response + "[" + f + "]");
		}
		return buf.toString();
	}

	private void saveSymbols() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			saveSymbols(doc);
			DOMSource source = new DOMSource(doc);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 2);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult result = new StreamResult(new File(filename + ".xml"));
			transformer.transform(source, result);
		} catch (ParserConfigurationException e) {
			reportFail(e.getMessage());

		} catch (TransformerException e) {
			reportFail(e.getMessage());
		}
	}

	private void saveSymbols(Document doc) {
		Element rootEl = doc.createElement("symbols");
		rootEl.setAttribute("treatnullasvalue",
				String.valueOf(treatNullAsValue));
		for (String feat : stringSeenValues.keySet()) {
			for (String val : stringSeenValues.get(feat)) {
				Element stringEl = doc.createElement("string");
				stringEl.setAttribute("feat", feat);
				stringEl.setAttribute("val", val);
				rootEl.appendChild(stringEl);
			}
		}
		for (String feat : stringNullValues) {
			Element nullEl = doc.createElement("stringnull");
			nullEl.setAttribute("feat", feat);
			rootEl.appendChild(nullEl);
		}
		for (String feat : setSeenValues.keySet()) {
			for (String val : setSeenValues.get(feat)) {
				Element setEl = doc.createElement("set");
				setEl.setAttribute("feat", feat);
				setEl.setAttribute("val", val);
				rootEl.appendChild(setEl);
			}
		}
		for (String feat : vectorLength.keySet()) {
			Element setEl = doc.createElement("vector");
			setEl.setAttribute("feat", feat);
			setEl.setAttribute("len", "" + vectorLength.get(feat));
			rootEl.appendChild(setEl);
		}
		for (String resp : seenResponses) {
			Element respEl = doc.createElement("response");
			respEl.setAttribute("name", resp);
			rootEl.appendChild(respEl);
		}
		doc.appendChild(rootEl);
	}

	private void loadSymbols() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(filename + ".xml"));
			loadSymbols(doc);
		} catch (ParserConfigurationException e) {
			reportFail(e.getMessage());
		} catch (SAXException e) {
			reportFail(e.getMessage());
		} catch (IOException e) {
			reportFail(e.getMessage());
		}
	}

	private void loadSymbols(Document doc) {
		for (Element el : elements(doc, "symbols")) {
			treatNullAsValue = Boolean.parseBoolean(el.getAttribute("treatnullasvalue"));
		}
		stringSeenValues.clear();
		stringNullValues.clear();
		for (Element el : elements(doc, "string")) {
			String feat = el.getAttribute("feat");
			String val = el.getAttribute("val");
			addStringValue(feat, val);
		}
		for (Element el : elements(doc, "stringnull")) {
			String feat = el.getAttribute("feat");
			addStringValue(feat, null);
		}
		setSeenValues.clear();
		for (Element el : elements(doc, "set")) {
			String feat = el.getAttribute("feat");
			String val = el.getAttribute("val");
			TreeSet<String> vals = new TreeSet<>();
			vals.add(val);
			addSetValues(feat, vals);
		}
		vectorLength.clear();
		for (Element el : elements(doc, "vector")) {
			String feat = el.getAttribute("feat");
			String len = el.getAttribute("len");
			vectorLength.put(feat, Integer.parseInt(len));
		}
		seenResponses.clear();
		for (Element el : elements(doc, "response")) {
			String resp = el.getAttribute("name");
			seenResponses.add(resp);
		}
	}

	private Vector<Element> elements(Document doc, String name) {
		Vector<Element> elems = new Vector<>();
		NodeList list = doc.getElementsByTagName(name);
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				elems.add((Element) node);
			}
		}
		return elems;
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
	protected static void reportFail(final String message) {
		logger().log(Level.WARNING, message);
	}

	/**
	 * Report fine comment.
	 *
	 * @param message The message.
	 */
	protected void reportFine(final String message) {
		logger().fine(message);
	}
}
