public class DecPHI {
    public static void main(String[] args) throws Exception {
        Pairing pairing = PairingFactory.getPairing("d224.properties");

        Field<Element> G1 = pairing.getG1();
        Field<Element> G2 = pairing.getG2();
        Field<Element> GT = pairing.getGT();
        Field<Element> Zp = pairing.getZr();

        Element C0 = GT.newRandomElement().getImmutable();
        Element AR1 = GT.newRandomElement().getImmutable();
        Element AR3 = GT.newRandomElement().getImmutable();

        Element gamma = Zp.newRandomElement().getImmutable();

        int iterations = 10;
        Element M = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();

            Element ratio = AR1.duplicate().div(AR3).getImmutable();
            M = C0.duplicate().mul(ratio.powZn(gamma)).getImmutable();

            long endTime = System.nanoTime();
            double elapsedMs = (endTime - startTime) / 1_000_000.0;

            System.out.printf("Iteration %d: %.3f ms\n", i + 1, elapsedMs);
        }

        int storageBytes = M.toBytes().length;
        System.out.printf("Storage of M: %d Bytes\n", storageBytes);
    }
}
