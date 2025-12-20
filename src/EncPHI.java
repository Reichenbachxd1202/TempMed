public class EncPHI {
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

        Element g1 = G1.newRandomElement().getImmutable();
        Element g2 = G2.newRandomElement().getImmutable();

        SecureRandom random = new SecureRandom();

        for (int np = 10; np <= 100; np += 10) {
            for (int d = 3; d <= 15; d++) {
                System.out.println("========== np = " + np + ", d = " + d + " ==========");

                Element Msg = GT.newRandomElement().getImmutable();
                Element alpha = Zp.newRandomElement().getImmutable();
                Element v = Zp.newRandomElement().getImmutable();
                Element r = Zp.newRandomElement().getImmutable();
                Element A = pairing.pairing(g1, g2).powZn(alpha).getImmutable();

                List<Element> betas = new ArrayList<>();
                for (int i = 0; i <= d; i++) betas.add(G1.newRandomElement().getImmutable());

                int[] bitString = new int[d + 1];
                for (int i = 1; i <= d; i++) {
                    bitString[i] = random.nextBoolean() ? 1 : 0;
                }

                List<Element> deltas = new ArrayList<>();
                for (int i = 0; i < np; i++) deltas.add(Zp.newRandomElement().getImmutable());

                long[] omega = new long[np];
                Element[] r_i = new Element[np];
                Element[] vpk1 = new Element[np];
                List<Element> C3List = new ArrayList<>();

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

                double startTime = System.nanoTime();

                Element C0 = Msg.duplicate().mul(A.powZn(v)).getImmutable();
                Element C1 = g2.powZn(v).getImmutable();
                Element[] C2 = new Element[np];
                for (int i = 0; i < np; i++) {
                    C2[i] = vpk1[i].powZn(v).getImmutable();
                }
                for (int i = 1; i <= d; i++) {
                    Element prod = betas.get(0).duplicate();
                    for (int k = 1; k <= i; k++) {
                        if (bitString[k] == 1) {
                            prod = prod.mul(betas.get(k));
                        }
                    }
                    Element C3 = prod.powZn(v).getImmutable();
                    C3List.add(C3);
                }

                double endTime = System.nanoTime();
                double durationMs = (endTime - startTime) / 1_000_000.0;
                System.out.printf("EncPHI Computation Time (np=%d, d=%d): %.3f ms%n", np, d, durationMs);

                long c0Size = C0.toBytes().length;
                long c1Size = C1.toBytes().length;
                long c2TotalSize = 0;
                for (int i = 0; i < np; i++) c2TotalSize += C2[i].toBytes().length;
                long c3TotalSize = 0;
                for (Element c3 : C3List) c3TotalSize += c3.toBytes().length;

                System.out.printf("C0 Storage: %.3f KBytes%n", c0Size / 1024.0);
                System.out.printf("C1 Storage: %.3f KBytes%n", c1Size / 1024.0);
                System.out.printf("Total C2 Storage (all %d): %.3f KBytes%n", np, c2TotalSize / 1024.0);
                System.out.printf("Total C3 Storage (all %d): %.3f KBytes%n", C3List.size(), c3TotalSize / 1024.0);

                long totalBytes = c0Size + c1Size + c2TotalSize + c3TotalSize;
                System.out.printf("Total CT Storage (np=%d, d=%d): %.3f KB%n%n", np, d, totalBytes / 1024.0);
            }
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
