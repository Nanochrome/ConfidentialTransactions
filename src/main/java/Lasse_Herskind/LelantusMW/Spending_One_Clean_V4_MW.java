package Lasse_Herskind.LelantusMW;

import Lasse_Herskind.GeneralizedSchnorr.GeneralizedSchnorrProof;
import Lasse_Herskind.GeneralizedSchnorr.GeneralizedSchnorrProofProver;
import Lasse_Herskind.GeneralizedSchnorr.GeneralizedSchnorrProofVerifier;
import Lasse_Herskind.LelantusConstants;
import Lasse_Herskind.LelantusUtils;
import Lasse_Herskind.OneOutOfManyMW.OneOutOfManyProofMW;
import Lasse_Herskind.OneOutOfManyMW.OneOutOfManyProofProverMW;
import Lasse_Herskind.OneOutOfManyMW.OneOutOfManyProofVerifierMW;
import Lasse_Herskind.OneOutOfManyMW.OneOutOfManyWitnessMW;
import edu.stanford.cs.crypto.efficientct.DoubleGeneratorParams;
import edu.stanford.cs.crypto.efficientct.VerificationFailedException;
import edu.stanford.cs.crypto.efficientct.circuit.groups.BN128Group;
import edu.stanford.cs.crypto.efficientct.circuit.groups.GroupElement;
import edu.stanford.cs.crypto.efficientct.commitments.DoubleBlindedPedersenCommitment;
import edu.stanford.cs.crypto.efficientct.util.ProofUtils;

import java.math.BigInteger;

public class Spending_One_Clean_V4_MW {

    private static BN128Group curve;
    private static DoubleGeneratorParams params;

    private static void init(int size) {
        curve = new BN128Group();
        params = DoubleGeneratorParams.generateParams(size, curve);
    }

    public static void main(String[] args) throws VerificationFailedException {
        /**
         * In this one we try do something else.
         * Instead of extracting some value V, we will extract the a commitment output_coin
         * Which is a normal Pedersen commitment, i.e., gV+hR.
         */

        GeneralizedSchnorrProofProver generalizedSchnorrProofProver = new GeneralizedSchnorrProofProver();
        GeneralizedSchnorrProofVerifier generalizedSchnorrProofVerifier = new GeneralizedSchnorrProofVerifier();

        long start_time = System.currentTimeMillis();

        // First, we need to make an anonymity set, let us use n = 2 and m = 2 for N = 4
        int n = LelantusConstants.n;
        int m = LelantusConstants.m;
        int N = (int) Math.pow(n, m);
        System.out.println("n: " + n + ", m: " + m + ", N: " + N);
        init(N);

        // We Mint the transaction
        BigInteger V = BigInteger.valueOf(25);
        BigInteger q = ProofUtils.randomNumber();
        GroupElement Q = params.getBase().g.multiply(q);
        BigInteger S = ProofUtils.hash(Q.toString());
        BigInteger R = ProofUtils.randomNumber();

        DoubleBlindedPedersenCommitment shielded_coin = LelantusUtils.getDBPedersen(params, S, V, R);
        System.out.println("shielded_coin: \t\t\t\t" + shielded_coin.getCommitment().stringRepresentation());

        DoubleBlindedPedersenCommitment output_coin = LelantusUtils.getDBPedersen(params, BigInteger.ZERO, V, ProofUtils.randomNumber());
        System.out.println("coin_extracted: \t" + output_coin.getCommitment().stringRepresentation());

        System.out.println("--- spending ---");

        System.out.println("Reveal S: " + S);

        DoubleBlindedPedersenCommitment serialNumber = LelantusUtils.getDBPedersen(params, S, BigInteger.ZERO, BigInteger.ZERO).add(output_coin);
        System.out.println("Serial Number Point: " + serialNumber.getCommitment().stringRepresentation());

        // TODO: Implement the 1-out-of-N proofs below!

        GroupElement[] CMList = new GroupElement[N];
        for (int i = 0; i < N - 1; i++) {
            CMList[i] = LelantusUtils.getDBPedersen(params, BigInteger.valueOf(i + 1)).getCommitment();
        }
        CMList[N - 1] = shielded_coin.getCommitment();
        for (int i = 0; i < N; i++) {
            CMList[i] = CMList[i].subtract(serialNumber.getCommitment());
        }

        OneOutOfManyProofProverMW oneOutOfManyProofProverMW = new OneOutOfManyProofProverMW();
        OneOutOfManyProofVerifierMW oneOutOfManyProofVerifierMW = new OneOutOfManyProofVerifierMW();

        OneOutOfManyWitnessMW oneOutOfManyWitnessMW = new OneOutOfManyWitnessMW(N-1, shielded_coin, output_coin);
        OneOutOfManyProofMW oneOutOfManyProofMW = oneOutOfManyProofProverMW.generateProof(params, CMList, oneOutOfManyWitnessMW);
        oneOutOfManyProofVerifierMW.verify(params, CMList, oneOutOfManyProofMW);

        BigInteger private_A = LelantusUtils.getAPrivate(params, oneOutOfManyProofMW, oneOutOfManyWitnessMW);
        GroupElement public_A = LelantusUtils.getAPublic(params, oneOutOfManyProofMW);

        if (!public_A.equals(LelantusUtils.getDBPedersen(params, BigInteger.ZERO, BigInteger.ZERO, private_A).getCommitment())) {
            throw new VerificationFailedException();
        }

        // TODO: What can we split for the excess to be two-fold? Instead of just k*G.
        // Maybe this is not really useful anyway? I.e., the proof clearly contains the output anyways :/
        // Forgot that serialNumber is = g^s*h^v*j^r, so we simply made some odd offsets.
        DoubleBlindedPedersenCommitment publishedSource = output_coin.sub(LelantusUtils.getDBPedersen(params, q, BigInteger.ZERO, private_A));

        // Everybody can calculate the published source
        GroupElement testAIsUsed = serialNumber.getCommitment().subtract(public_A.add(Q));
        System.out.println(testAIsUsed.equals(publishedSource.getCommitment()));

        // The input is then
        System.out.println("Source: " + publishedSource);

        // The output is then
        System.out.println("Output: " + output_coin);

        // The excess
        DoubleBlindedPedersenCommitment excess = output_coin.sub(publishedSource);
        System.out.println("Excess: " + excess);
        System.out.println("Excess should be: " + public_A.add(Q));

        // Then we just prove that we know these values! Huge

        // This is not good enough, we actually need to proof knowledge of a and q seperately. :/ Just more signatures, but not as neat! ØV
        GeneralizedSchnorrProof generalizedSchnorrProof = generalizedSchnorrProofProver.generateProof(params, excess.getCommitment(), excess);
        generalizedSchnorrProofVerifier.verify(params, excess.getCommitment(), generalizedSchnorrProof);

        System.out.println("It works");

        System.out.println(System.currentTimeMillis() - start_time);
    }



}
