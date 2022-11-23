/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class TigerTreebank extends ConstTreebank {
    public TigerTreebank(String id,
                         Set<String> poss, Set<String> cats, Set<String> labels,
                         ConstTree[] trees) {
        super(id, poss, cats, labels, trees);
    }

    public TigerTreebank(final String filename) {
        final List<ConstTree> sentences = new ArrayList<>();
        File xmlFile = new File(filename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            NodeList sentenceList = doc.getElementsByTagName("s");
            for (int i = 0; i < sentenceList.getLength(); i++) {
                sentences.add(readSentence(sentenceList.item(i)));
            }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            logger().warning(e.getMessage());
        }
    }

    private ConstTree readSentence(Node node) {
        Element element = (Element) node;
        String id = element.getAttribute("id");
        ConstTree tree = new ConstTree(id);
        System.out.println(id);
        NodeList graphList = element.getElementsByTagName("graph");
        for (int i = 0; i < graphList.getLength(); i++) {
            // TODO
        }
        return tree;
    }

    public static void convertNegra(final String source, final String target) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            PrintWriter writer = new PrintWriter(target, "UTF-8");
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                String idS = "";
                String root = "";
                ConstTree tree = null;
                String idNt = "";

                public void startElement(String uri,
                                         String localName,
                                         String qName,
                                         Attributes atts) {
                    if (qName.equals("s")) {
                        idS = atts.getValue("id");
                        idS = idS.replaceAll("s", "");
                        tree = new ConstTree(idS);
                    } else if (qName.equals("graph")) {
                        root = atts.getValue("root");
                        root = root.replaceAll("s[0-9]*_", "");
                    } else if (qName.equals("t")) {
                        String idT = atts.getValue("id");
                        idT = idT.replaceAll("s[0-9]*_", "");
                        String form = atts.getValue("word");
                        String pos = atts.getValue("pos");
                        ConstLeaf leaf = tree.addLeaf(form, pos, "--");
                        if (idT.equals(root)) {
                            tree.addRoot(leaf);
                        }
                    } else if (qName.equals("nt")) {
                        idNt = atts.getValue("id");
                        idNt = idNt.replaceAll("s[0-9]*_", "");
                        if (!idNt.equals(root)) {
                            String cat = atts.getValue("cat");
                            tree.addInternal(idNt, cat, "--");
                        }
                    } else if (qName.equals("edge")) {
                        String label = atts.getValue("label");
                        String idref = atts.getValue("idref");
                        idref = idref.replaceAll("s[0-9]*_", "");
                        int idrefNum = Integer.parseInt(idref);
                        if (idNt.equals(root)) {
                            if (idrefNum >= 500) {
                                ConstInternal child = tree.getInternal(idref);
                                child.setLabel(label);
                                tree.addRoot(child);
                            } else {
                                int i = Integer.parseInt(idref) - 1;
                                ConstLeaf child = tree.getLeaf(i);
                                child.setLabel(label);
                                tree.addRoot(child);
                            }
                        } else {
                            if (idrefNum >= 500) {
                                ConstInternal parent = tree.getInternal(idNt);
                                ConstInternal child = tree.getInternal(idref);
                                child.setLabel(label);
                                parent.addChildRight(child);
                            } else {
                                ConstInternal parent = tree.getInternal(idNt);
                                int i = idrefNum - 1;
                                ConstLeaf child = tree.getLeaf(i);
                                child.setLabel(label);
                                parent.addChildRight(child);
                            }
                        }
                    }
                }

                public void endElement(String uri, String localName, String qName) {
                    if (qName.equals("s")) {
                        // System.out.println("multipleroot");
                        writer.println(NegraTreebank.treeToString(tree));
						/*
						writer.println("#BOS " + idS);
						for (ConstLeaf leaf : leaves) {
							writer.println(leaf.getForm() + "\t\t\t" +
									leaf.getCat() + "\t--\t\t" +
									leaf.getLabel() + "\t");
							// parents[leaf.getIndex()] + "\n";
						}
						writer.println("#EOS " + idS);
						leaves.clear();
						*/
                    }

                }
            };
            saxParser.parse(source, handler);
            writer.close();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger().warning(e.getMessage());
        }
    }

    private static Logger logger() {
        final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
        log.setParent(Logger.getGlobal());
        return log;
    }
}
