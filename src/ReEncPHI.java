public class ReEncPHI {
    static Pairing pairing;
    static Field<Element> G1;
    static Field<Element> G2;
    static Field<Element> GT;
    static Field<Element> Zp;

    static SecureRandom secureRandom = new SecureRandom();

    public static void main(String[] args) throws Exception {
        pairing = PairingFactory.getPairing("g149.properties");
        G1 = pairing.getG1();
        G2 = pairing.getG2();
        GT = pairing.getGT();
        Zp = pairing.getZr();

        int num = 100;

        for (int np = 10; np <= 100; np += 10) {
            System.out.println("========== np = " + np + " ==========");

            Element v = Zp.newRandomElement().getImmutable();
            Element r = Zp.newRandomElement().getImmutable();

            List<Element> deltas = new ArrayList<>();
            for (int i = 0; i < np; i++) deltas.add(Zp.newRandomElement().getImmutable());

            List<Element> tldeltas = new ArrayList<>();
            for (int i = 0; i < np; i++) tldeltas.add(Zp.newRandomElement().getImmutable());

            long[] omega = new long[np];
            Element[] r_i = new Element[np];
            Element[] vpk1 = new Element[np];

            for (int i = 0; i < np; i++) {
                long w = secureRandom.nextLong();
                omega[i] = w;

                BigInteger omegaBI = BigInteger.valueOf(w);
                Element omegaZ = Zp.newElement().set(omegaBI).getImmutable();

                r_i[i] = r.duplicate().mul(omegaZ).getImmutable();
                byte[] omegaBytes = longToBytes(w);
                Element H_omega = hashFromBytesToG1(omegaBytes);
                vpk1[i] = H_omega.powZn(r_i[i]).getImmutable();
            }

            Element[] C2 = new Element[np];
            for (int i = 0; i < np; i++) {
                C2[i] = vpk1[i].powZn(v).getImmutable();
            }

            List<Element> ratios = new ArrayList<>();
            for (int i = 0; i < np; i++) {
                Element ratio = deltas.get(i).duplicate().div(tldeltas.get(i)).getImmutable();
                ratios.add(ratio);
            }

            double totalTimeMs = 0;
            long totalStorage = 0;

            for (int t = 0; t < num; t++) {
                Element[] tlC2 = new Element[np];
                double startTime = System.nanoTime();

                for (int i = 0; i < np; i++) {
                    tlC2[i] = C2[i].powZn(ratios.get(i)).getImmutable();
                }

                double endTime = System.nanoTime();
                totalTimeMs += (endTime - startTime) / 1_000_000.0;

                long c2TotalSize = 0;
                for (int i = 0; i < np; i++) c2TotalSize += tlC2[i].toBytes().length;
                totalStorage += c2TotalSize;
            }

            System.out.printf("Average ReEncPHI Computation Time: %.3f ms%n",
                    totalTimeMs / num);
            System.out.printf("Average tlC2 Storage: %.3f KBytes%n%n",
                    totalStorage / num / 1024.0);
        }
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static Element hashFromBytesToG1(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return pairing.getG1().newElement().setFromHash(hash, 0, hash.length).getImmutable();
    }
}
