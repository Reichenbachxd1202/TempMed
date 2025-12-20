public class PolKGen {
    static Pairing pairing;
    static Field<Element> G1, G2, GT, Zp;

    public static Element hashFromBytesToG0(byte[] bytes) {
        return pairing.getG1().newElement().setFromHash(bytes, 0, bytes.length).getImmutable();
    }

    public static void main(String[] args) throws Exception {
        pairing = PairingFactory.getPairing("g149.properties");
        G1 = pairing.getG1();
        G2 = pairing.getG2();
        GT = pairing.getGT();
        Zp = pairing.getZr();

        Element alpha = Zp.newRandomElement().getImmutable();
        Element mu = Zp.newRandomElement().getImmutable();
        Element r = Zp.newRandomElement().getImmutable();
        Element delta = Zp.newRandomElement().getImmutable();

        int d = 10;
        int uni = 200;

        List<Element> deltaList = new ArrayList<>(uni);
        for (int i = 0; i < uni; i++) {
            deltaList.add(Zp.newRandomElement().getImmutable());
        }

        Random rnd = new Random();
        List<Long> omegaList = new ArrayList<>(uni);
        for (int i = 0; i < uni; i++) {
            omegaList.add(rnd.nextLong());
        }

        List<Element> betaList = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            betaList.add(G1.newRandomElement().getImmutable());
        }

        Element g1 = G1.newRandomElement().getImmutable();
        Element g2 = G2.newRandomElement().getImmutable();

        List<Element> vpk1List = new ArrayList<>(uni);
        List<Element> vpk2List = new ArrayList<>(uni);
        for (int i = 0; i < uni; i++) {
            String omegaStr = Long.toString(omegaList.get(i));
            Element H_omega = hashFromBytesToG0(omegaStr.getBytes());

            Element exp1 = r.duplicate().mul(deltaList.get(i)).getImmutable();
            Element vpk1 = H_omega.powZn(exp1).getImmutable();
            Element vpk2 = H_omega.powZn(delta).getImmutable();

            vpk1List.add(vpk1);
            vpk2List.add(vpk2);
        }

        Element gamma = Zp.newRandomElement().getImmutable();


        for (int l = 10; l <= 100; l += 10) {
            int n = 100;
            Element[][] M = new Element[l][n];
            for (int i = 0; i < l; i++) {
                for (int j = 0; j < n; j++) {
                    M[i][j] = Zp.newRandomElement().getImmutable();
                }
            }

            Element[] vec_v = new Element[n];
            vec_v[0] = (mu.sub(alpha)).div(gamma).getImmutable();
            for (int j = 1; j < n; j++) {
                vec_v[j] = Zp.newRandomElement().getImmutable();
            }

            Element[] vec_sig = new Element[l];
            for (int i = 0; i < l; i++) {
                Element sum = Zp.newZeroElement();
                for (int j = 0; j < n; j++) {
                    sum.add(M[i][j].duplicate().mul(vec_v[j]));
                }
                vec_sig[i] = sum.getImmutable();
            }

            List<Element> pok1List = new ArrayList<>(uni);

            long startTime = System.nanoTime();
            for (int i = 0; i < l; i++) {
                Element exp2 = r.duplicate().mul(delta).getImmutable();
                Element left = g1.powZn(vec_sig[i]);
                Element right = vpk1List.get(i).powZn(exp2);
                Element pok1 = left.mul(right).getImmutable();
                pok1List.add(pok1);
            }

            Element pok2 = g2.powZn(delta).getImmutable();

            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            System.out.printf("l = %d, PolKGen computation time: %.3f ms%n", l, durationMs);

            long totalBytes = 0;
            for (Element pok1 : pok1List) {
                totalBytes += pok1.toBytes().length;
            }
            totalBytes += pok2.toBytes().length;
            System.out.printf("l = %d, Pok storage: %.3f KB%n%n", l, totalBytes / 1024.0);
        }
    }
}
