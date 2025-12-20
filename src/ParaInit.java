public class ParaInit {
    static Pairing pairing;
    static Field<Element> G0, G1, GT, Zp;

    static class TreeNode {
        String code;
        TreeNode left, right;
        TreeNode(String code) { this.code = code; }
    }

    static List<String> nodeCodes = new ArrayList<>();

    public static TreeNode buildTree(String code, int depth, int maxDepth) {
        TreeNode node = new TreeNode(code);
        if (depth < maxDepth) {
            node.left = buildTree(code + "0", depth + 1, maxDepth);
            node.right = buildTree(code + "1", depth + 1, maxDepth);
        }
        if (depth == maxDepth) nodeCodes.add(code);
        return node;
    }

    public static Element hashFromStringToG0(String str) {
        byte[] bytes = str.getBytes();
        return G0.newElement().setFromHash(bytes, 0, bytes.length).getImmutable();
    }

    public static void main(String[] args) {
        pairing = PairingFactory.getPairing("d224.properties");
        G0 = pairing.getG1();
        G1 = pairing.getG2();
        GT = pairing.getGT();
        Zp = pairing.getZr();

        for (int d = 5; d <= 15; d++) {
            for (int uni = 10; uni <= 100; uni += 10) {
                nodeCodes.clear();
                TreeNode root = buildTree("", 0, d);
                int leafCount = nodeCodes.size();

                Element g1 = G0.newRandomElement().getImmutable();
                Element g2 = G1.newRandomElement().getImmutable();

                double totalDurationMs = 0.0;
                long totalParsBytes = 0;
                long totalVpkBytes = 0;
                long totalVskBytes = 0;
                double num = 100;

                for (int repeat = 0; repeat < num; repeat++) {
                    long startTime = System.nanoTime();

                    Element alpha = Zp.newRandomElement().getImmutable();
                    Element mu = Zp.newRandomElement().getImmutable();
                    Element r = Zp.newRandomElement().getImmutable();
                    Element delta = Zp.newRandomElement().getImmutable();

                    List<Element> deltaList = new ArrayList<>(uni);
                    List<Long> omegaList = new ArrayList<>(uni);
                    Random rnd = new Random();
                    for (int i = 0; i < uni; i++) {
                        deltaList.add(Zp.newRandomElement().getImmutable());
                        omegaList.add(rnd.nextLong());
                    }

                    Element beta = G0.newRandomElement().getImmutable();
                    List<Element> betaList = new ArrayList<>();
                    for (int j = 0; j < d; j++) {
                        betaList.add(G0.newRandomElement().getImmutable());
                    }

                    Element A = pairing.pairing(g1, g2).powZn(alpha).getImmutable();
                    Element B = g2.powZn(mu).getImmutable();
                    Element C = g2.powZn(delta).getImmutable();

                    List<Element> vpk1List = new ArrayList<>(uni);
                    List<Element> vpk2List = new ArrayList<>(uni);
                    for (int i = 0; i < uni; i++) {
                        String omegaStr = Long.toString(omegaList.get(i));
                        Element H_omega = hashFromStringToG0(omegaStr);

                        Element exp1 = r.duplicate().mul(deltaList.get(i)).getImmutable();
                        Element vpk1 = H_omega.powZn(exp1).getImmutable();
                        Element vpk2 = H_omega.powZn(delta).getImmutable();

                        vpk1List.add(vpk1);
                        vpk2List.add(vpk2);
                    }

                    long endTime = System.nanoTime();
                    totalDurationMs += (endTime - startTime) / 1_000_000.0;

                    long betaBytes = 0;
                    for (Element b : betaList) betaBytes += b.toBytes().length;

                    long parsBytes = 0;
                    parsBytes += g1.toBytes().length;
                    parsBytes += g2.toBytes().length;
                    parsBytes += beta.toBytes().length;
                    for (Element b : betaList) parsBytes += b.toBytes().length;
                    parsBytes += A.toBytes().length;
                    parsBytes += B.toBytes().length;
                    parsBytes += C.toBytes().length;
                    parsBytes += betaBytes;

                    long vpkBytes = 0;
                    for (int i = 0; i < uni; i++) {
                        vpkBytes += vpk1List.get(i).toBytes().length;
                        vpkBytes += vpk2List.get(i).toBytes().length;
                    }

                    long vskBytes = 0;
                    vskBytes += alpha.toBytes().length;
                    vskBytes += mu.toBytes().length;
                    vskBytes += r.toBytes().length;
                    vskBytes += delta.toBytes().length;
                    for (Element di : deltaList) vskBytes += di.toBytes().length;

                    totalParsBytes += parsBytes;
                    totalVpkBytes += vpkBytes;
                    totalVskBytes += vskBytes;
                }

                double avgDurationMs = totalDurationMs / num;
                double avgParsKB = (double) totalParsBytes / num / 1024.0;
                double avgVpkKB = (double) totalVpkBytes / num / 1024.0;
                double avgVskKB = (double) totalVskBytes / num / 1024.0;

                System.out.printf("ParaInit computation time: %.3f ms%n", avgDurationMs);
                System.out.printf("Pars storage: %.3f KB%n", avgParsKB);
                System.out.printf("Vpk storage: %.3f KB%n", avgVpkKB);
                System.out.printf("Vsk storage: %.3f KB%n", avgVskKB);
                System.out.printf("Universe: %d, Tree depth: %d, Leaf count: %d%n", uni, d, leafCount);
                System.out.println("-----------------------------------------------");
            }
        }
    }
}
