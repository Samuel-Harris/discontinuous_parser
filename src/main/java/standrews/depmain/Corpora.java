/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import java.io.File;
import java.util.ArrayList;

public class Corpora {

	public static final String universalDependencies =
			"../datasets/ud-treebanks-v2.10/";

	/**
	 * All directories with corpora.
	 */
	public static File[] dirs() {
		return new File(universalDependencies).listFiles();
	}

	/**
	 * The train corpus in the directory if any.
	 */
	public static File train(final File dir) {
		for (File corpus : dir.listFiles()) {
			String name = corpus.getName();
			if (name.matches(".*train.conllu$")) {
				return corpus;
			}
		}
		return null;
	}

	/**
	 * The test corpus in the directory if any.
	 */
	public static File test(final File dir) {
		for (File corpus : dir.listFiles()) {
			String name = corpus.getName();
			if (name.matches(".*test.conllu$")) {
				return corpus;
			}
		}
		return null;
	}

	/**
	 * All training files.
	 * @return
	 */
	public static String[] trainFiles() {
		ArrayList<String> files = new ArrayList<>();
		for (File dir : dirs()) {
			File file = train(dir);
			if (file != null)
				files.add(file.getPath());
		}
		return files.toArray(new String[files.size()]);
	}

	public static String trainFile(String lang) {
		return corpus(lang) +
				lang + "_" + subname(lang).toLowerCase() + "-ud-train.conllu";
	}
	public static String testFile(String lang) {
		return corpus(lang) +
				lang + "_" + subname(lang).toLowerCase() + "-ud-test.conllu";
	}
	private static String corpus(String lang) {
		return universalDependencies +
				"UD_" + name(lang) + "-" + subname(lang) + "/";
	}
	private static String name(String lang) {
		if (lang.equals("ar"))
			return "Arabic";
		else if (lang.equals("bg"))
			return "Bulgarian";
		else if (lang.equals("ca"))
			return "Catalan";
		else if (lang.equals("cs"))
			return "Czech";
		else if (lang.equals("cu"))
			return "Old_Church_Slavonic";
		else if (lang.equals("da"))
			return "Danish";
		else if (lang.equals("de"))
			return "German";
		else if (lang.equals("el"))
			return "Greek";
		else if (lang.equals("en"))
			return "English";
		else if (lang.equals("es"))
			return "Spanish";
		else if (lang.equals("et"))
			return "Estonian";
		else if (lang.equals("eu"))
			return "Basque";
		else if (lang.equals("fa"))
			return "Persian";
		else if (lang.equals("fi"))
			return "Finnish";
		else if (lang.equals("fr"))
			return "French";
		else if (lang.equals("fro"))
			return "Old_French";
		else if (lang.equals("got"))
			return "Gothic";
		else if (lang.equals("grc"))
			return "Ancient_Greek";
		else if (lang.equals("he"))
			return "Hebrew";
		else if (lang.equals("hi"))
			return "Hindi";
		else if (lang.equals("hr"))
			return "Croatian";
		else if (lang.equals("hu"))
			return "Hungarian";
		else if (lang.equals("id"))
			return "Indonesian";
		else if (lang.equals("it"))
			return "Italian";
		else if (lang.equals("ja"))
			return "Japanese";
		else if (lang.equals("ko"))
			return "Korean";
		else if (lang.equals("la"))
			return "Latin";
		else if (lang.equals("lv"))
			return "Latvian";
		else if (lang.equals("nl"))
			return "Dutch";
		else if (lang.equals("no"))
			return "Norwegian";
		else if (lang.equals("pl"))
			return "Polish";
		else if (lang.equals("pt"))
			return "Portuguese";
		else if (lang.equals("ro"))
			return "Romanian";
		else if (lang.equals("ru"))
			return "Russian";
		else if (lang.equals("sk"))
			return "Slovak";
		else if (lang.equals("sl"))
			return "Slovenian";
		else if (lang.equals("sv"))
			return "Swedish";
		else if (lang.equals("tr"))
			return "Turkish";
		else if (lang.equals("ur"))
			return "Urdu";
		else
			throw new IllegalArgumentException("Wrong language");
	}
	private static String subname(String lang) {
		if (lang.equals("ar"))
			return "NYUAD";
		else if (lang.equals("bg"))
			return "BTB";
		else if (lang.equals("ca"))
			return "AnCora";
		else if (lang.equals("cs"))
			return "CAC";
		else if (lang.equals("cu"))
			return "PROIEL";
		else if (lang.equals("da"))
			return "DDT";
		else if (lang.equals("de"))
			return "GSD";
		else if (lang.equals("en"))
			return "EWT";
		else if (lang.equals("el"))
			return "GDT";
		else if (lang.equals("es"))
			return "AnCora";
		else if (lang.equals("et"))
			return "EDT";
		else if (lang.equals("eu"))
			return "BDT";
		else if (lang.equals("fa"))
			return "Seraji";
		else if (lang.equals("fi"))
			return "FTB";
		else if (lang.equals("fr"))
			return "GSD";
		else if (lang.equals("fro"))
			return "SRCMF";
		else if (lang.equals("got"))
			return "PROIEL";
		else if (lang.equals("grc"))
			return "PROIEL";
		else if (lang.equals("he"))
			return "HTB";
		else if (lang.equals("hi"))
			return "HDTB";
		else if (lang.equals("hr"))
			return "SET";
		else if (lang.equals("hu"))
			return "Szeged";
		else if (lang.equals("id"))
			return "GSD";
		else if (lang.equals("it"))
			return "ISDT";
		else if (lang.equals("ja"))
			return "BCCWJ";
		else if (lang.equals("ko"))
			return "Kaist";
		else if (lang.equals("la"))
			return "PROIEL";
		else if (lang.equals("lv"))
			return "LVTB";
		else if (lang.equals("nl"))
			return "Alpino";
		else if (lang.equals("no"))
			return "Bokmaal";
		else if (lang.equals("pl"))
			return "LFG";
		else if (lang.equals("pt"))
			return "GSD";
		else if (lang.equals("ro"))
			return "RRT";
		else if (lang.equals("ru"))
			return "SynTagRus";
		else if (lang.equals("sk"))
			return "SNK";
		else if (lang.equals("sl"))
			return "SSJ";
		else if (lang.equals("sv"))
			return "Talbanken";
		else if (lang.equals("tr"))
			return "IMST";
		else if (lang.equals("ur"))
			return "UDTB";
		else
			throw new IllegalArgumentException("Wrong language");
	}

	public static String langOfCorpus(String path) {
		return "none";
	}

	/**
	 * An arbitrary selection of languages in UD2.2.
	 */
	public static String[] someLanguages() {
		return new String[] {"bg", "cs", "de", "en",
				"fa", "fi", "fr", "grc", "hi", "hr", "id", "it", "ja",
				"nl", "no", "pl", "pt", "ro", "sk", "sl", "sv"};
	}
	/**
	 * Languages with corpora in UD2.2 with at least 6000 training sentences.
	 * @return
	 */
	public static String[] languages6000() {
		return new String[] {
			"ar", "bg", "ca", "cs", "de", "en", "es", "et",
				"fi", "fr",
				"grc",
				"hi", "hr", "it", "ja", "ko", "la", "nl", "no",
				"pl", "pt", "ro", "ru",
				"sk", "sl"
		};
	}
	/**
	 * Languages with corpora in UD2.2 with at least 8000 training sentences.
	 * @return
	 */
	public static String[] languages8000() {
		return new String[] {
			"ar", "bg", "ca", "cs", "de", "en", "es", "et",
				"fi", "fr",
				"grc",
				"hi", "it", "ja", "ko", "la", "nl", "no",
				"pl", "pt", "ro", "ru",
				"sk"
		};
	}

	/**
	 * Languages with corpora in UD2.2 with at least 9000 training sentences.
	 * @return
	 */
	public static String[] languages9000() {
		return new String[]{
			"ar", "ca", "cs", "de", "en", "es", "et",
				"fi", "fr", "fro", "grc", "hi", "it", "ja", "ko", "la",
				"nl", "no", "pl", "pt", "ru"
		};
	}

}
