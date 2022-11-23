/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase.heads;

import standrews.constbase.*;

import java.util.stream.Stream;

public class NegraHeadFinder extends HeadFinder {

    protected int getHeadIndex(ConstInternal node) {
        if (nLabel(node, "HD") == 1) {
            return firstWithLabel(node, "HD");
        } else if (nLabel(node, "HD") > 1) {
            return firstWithLabel(node, "HD");
        } else if (node.getCat().equals("VP")) {
            return 0;
        } else if (node.getCat().equals("S")) {
            return 0;
        } else if (node.getCat().equals("AP")) {
            return 0;
        } else if (node.getCat().equals("AVP")) {
            return 0;
        } else if (Stream.of("CPP", "CVP", "CNP", "CAP", "CAVP",
                        "CO", "CAC", "CVZ", "CCP").
                anyMatch(s -> s.equals(node.getCat()))) {
            if (nLabel(node, "CJ") > 0)
                return firstWithLabel(node, "CJ");
            return 0;
        } else if (node.getCat().equals("CVP")) {
            if (nLabel(node, "CJ") > 0)
                return firstWithLabel(node, "CJ");
            return 0;
        } else if (node.getCat().equals("NP")) {
            String[] heads1 = new String[]{"NN", "NE", "MPN", "CNP", "NP", "NM"};
            String[] heads2 = new String[]{"PPER", "PIS", "CARD", "AP",
                    "PRELS", "PIAT", "PDS", "PIDAT", "FM", "PPOSAT", "TRUNC"};
            String[] heads3 = new String[]{"ADJA", "MTA", "CAP", "PRF", "ADV", "XY"};
            String[] heads4 = new String[]{"ART", "PP", "KOKOM"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            if (nCat(node, heads2) > 0)
                return firstWithCat(node, heads2);
            if (nCat(node, heads3) > 0)
                return firstWithCat(node, heads3);
            if (nCat(node, heads4) > 0)
                return firstWithCat(node, heads4);
            return 0;
        } else if (node.getCat().equals("PP")) {
            String[] heads1 = new String[]{"APPR", "APPRART", "PROAV", "APPO", "CAC",
                    "KOUS", "KOUI", "PTKA", "ISU", "MPN"};
            String[] heads2 = new String[]{"CPP", "FM", "ADV", "ADJD", "ADJA", "KOKOM"};
            String[] heads3 = new String[]{"AP", "CARD"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            if (nCat(node, heads2) > 0)
                return firstWithCat(node, heads2);
            if (nCat(node, heads3) > 0)
                return firstWithCat(node, heads3);
            return 0;
        } else if (node.getCat().equals("MPN")) {
            String[] heads1 = new String[]{"MPN", "FM", "NE"};
            String[] heads2 = new String[]{"NN", "CARD", "XY"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            if (nCat(node, heads2) > 0)
                return firstWithCat(node, heads2);
            return 0;
        } else if (node.getCat().equals("CS")) {
            String[] heads1 = new String[]{"S", "CS"};
            String[] heads2 = new String[]{"VVFIN"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            if (nCat(node, heads2) > 0)
                return firstWithCat(node, heads2);
            return 0;
        } else if (node.getCat().equals("NM")) {
            String[] heads1 = new String[]{"CARD", "NN"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            return 0;
        } else if (node.getCat().equals("MTA")) {
            String[] heads1 = new String[]{"NE", "NN"};
            String[] heads2 = new String[]{"TRUNC"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            if (nCat(node, heads2) > 0)
                return firstWithCat(node, heads2);
            return 0;
        } else if (node.getCat().equals("DL")) {
            if (nLabel(node, "DH") > 0)
                return firstWithLabel(node, "DH");
            return 0;
        } else if (node.getCat().equals("ISU")) {
            String[] heads1 = new String[]{"NN"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            return 0;
        } else if (node.getCat().equals("QL")) {
            String[] heads1 = new String[]{"CARD"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            return 0;
        } else if (node.getCat().equals("CH")) {
            String[] heads1 = new String[]{"NE"};
            if (nCat(node, heads1) > 0)
                return firstWithCat(node, heads1);
            return 0;
        } else if (node.getCat().equals("--")) {
            return 0;
        } else {
            System.err.println(node.getCat());
            ConstLeaf[] leaves = node.getLeaves();
            ConstNode[] children = node.getChildren();
            printLeaves(leaves);
            for (ConstNode child : children) {
                System.err.print("   " + child.getCat() + " " +
                        child.getLabel() + " ");
                ConstLeaf[] childLeaves = child.getLeaves();
                printLeaves(childLeaves);
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        ConstTreebank bank = new NegraTreebank("/home/mjn/Data/Negra/negra-attach.export");
        NegraHeadFinder finder = new NegraHeadFinder();
        finder.makeHeadedTreebank(bank);
    }
}
