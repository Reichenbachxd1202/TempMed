public class RanKGen {
    static class TreeNode {
        String code;
        TreeNode left;
        TreeNode right;

        TreeNode(String code) {
            this.code = code;
        }
    }

    public static TreeNode buildTree(String code, int depth, int maxDepth) {
        TreeNode node = new TreeNode(code);
        if (depth < maxDepth) {
            node.left = buildTree(code + "0", depth + 1, maxDepth);
            node.right = buildTree(code + "1", depth + 1, maxDepth);
        }
        return node;
    }

    public static List<TreeNode> cover(TreeNode root, int L, int R, int maxDepth) {
        int offset = 1 << maxDepth;
        int x = L + offset;
        int y = R + offset;
        List<Integer> heapIndices = new ArrayList<>();

        while (x < y) {
            if (x % 2 == 1) {
                heapIndices.add(x);
                x++;
            }
            x /= 2;

            if (y % 2 == 0) {
                heapIndices.add(y);
                y--;
            }
            y /= 2;
        }
        if (x == y) heapIndices.add(x);

        List<String> codes = new ArrayList<>();
        for (int idx : heapIndices) {
            codes.add(heapIndexToCode(idx));
        }
        Collections.sort(codes);

        List<TreeNode> result = new ArrayList<>();
        for (String c : codes) {
            TreeNode n = findNode(root, c);
            if (n != null) result.add(n);
        }
        return result;
    }

    private static String heapIndexToCode(int idx) {
        StringBuilder sb = new StringBuilder();
        while (idx > 1) {
            sb.append((idx % 2 == 0) ? '0' : '1');
            idx /= 2;
        }
        return sb.reverse().toString();
    }

    private static TreeNode findNode(TreeNode root, String code) {
        TreeNode cur = root;
        for (char b : code.toCharArray()) {
            if (cur == null) return null;
            cur = (b == '0') ? cur.left : cur.right;
        }
        return cur;
    }

    static Pairing pairing;
    static Field<Element> G0, G1, GT, Zp;
    static Element g1, g2, g3;
    static List<Element> beta;
    static List<TreeNode> coverNodes;

    public static void main(String[] args) throws Exception {
        pairing = PairingFactory.getPairing("d159.properties");
        G0 = pairing.getG1();
        G1 = pairing.getG2();
        GT = pairing.getGT();
        Zp = pairing.getZr();

        g1 = G0.newRandomElement().getImmutable();
        g2 = G1.newRandomElement().getImmutable();
        g3 = G0.newRandomElement().getImmutable();

        int num = 100;

        for (int maxDepth = 5; maxDepth <= 15; maxDepth += 1) {
            TreeNode root = buildTree("", 0, maxDepth);
            int L = 1, R = (int) Math.pow(2, maxDepth - 1) - 2;

            System.out.printf("\n==== maxDepth = %d, L = %d, R = %d ====\n", maxDepth, L, R);
            coverNodes = cover(root, L, R, maxDepth);

            System.out.println("Cover Codes:");
            for (TreeNode n : coverNodes) {
                if (n.code.length() > maxDepth) continue;
                System.out.println(n.code.replaceFirst("^.", ""));
            }

            beta = new ArrayList<>();
            for (int i = 0; i <= maxDepth; i++) {
                beta.add(G0.newRandomElement().getImmutable());
            }

            runRanKGen(maxDepth, num);
        }
    }

    public static void runRanKGen(int maxDepth, int num) throws Exception {
        double totalDurationCrk = 0;
        double totalDurationFrk = 0;
        long totalCrkBytes = 0;
        long totalFrkBytes = 0;

        for (int t = 0; t < num; t++) {
            Element chi = Zp.newRandomElement().getImmutable();
            Element mu = Zp.newRandomElement().getImmutable();
            Element gamma = Zp.newRandomElement().getImmutable();

            List<Element> Crk1 = new ArrayList<>();
            List<Element> Frk1 = new ArrayList<>();

            long startTime1 = System.nanoTime();
            Element Crkmu = g1.duplicate().powZn(mu.powZn(chi));

            for (TreeNode n : coverNodes) {
                String tau = n.code.replaceFirst("^.", "");
                Element crk = Crkmu.duplicate();
                for (int i = 0; i < tau.length(); i++) {
                    if (tau.charAt(i) == '1') {
                        crk.mul(beta.get(i + 1).powZn(chi));
                    }
                }
                Crk1.add(crk.getImmutable());
            }
            Element Crk2 = g2.duplicate().powZn(chi);
            long endTime1 = System.nanoTime();
            totalDurationCrk += (endTime1 - startTime1) / 1_000_000.0;

            long startTime2 = System.nanoTime();
            Element Frkmu = g1.duplicate().powZn(mu.div(gamma));

            for (TreeNode n : coverNodes) {
                String tau = n.code.replaceFirst("^.", "");
                Element frk = Frkmu.duplicate();
                for (int i = 0; i < tau.length(); i++) {
                    if (tau.charAt(i) == '1') {
                        frk.mul(beta.get(i + 1).powZn(mu));
                    }
                }
                Frk1.add(frk.getImmutable());
            }
            Element Frk2 = g2.duplicate().powZn(mu);
            Element Frk3 = g1.duplicate().powZn(mu).powZn(chi);
            long endTime2 = System.nanoTime();
            totalDurationFrk += (endTime2 - startTime2) / 1_000_000.0;

            for (Element e : Crk1) totalCrkBytes += e.toBytes().length;
            totalCrkBytes += Crk2.toBytes().length;

            for (Element e : Frk1) totalFrkBytes += e.toBytes().length;
            totalFrkBytes += Frk2.toBytes().length;
            totalFrkBytes += Frk3.toBytes().length;
        }

        System.out.printf("Average Crk Computation Time: %.3f ms%n", totalDurationCrk / num);
        System.out.printf("Average Frk Computation Time: %.3f ms%n", totalDurationFrk / num);
        System.out.printf("Average Total Crk Storage: %.3f KB%n", totalCrkBytes / num / 1024.0);
        System.out.printf("Average Total Frk Storage: %.3f KB%n", totalFrkBytes / num / 1024.0);
        System.out.printf("-----------------------------------------------%n");
    }
}
