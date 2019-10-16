package Lasse_Herskind.GeneralizedSchnorr;

import Lasse_Herskind.SingleKnowledgeProof.KnowledgeProof;
import edu.stanford.cs.crypto.efficientct.DoubleGeneratorParams;
import edu.stanford.cs.crypto.efficientct.GeneratorParams;
import edu.stanford.cs.crypto.efficientct.VerificationFailedException;
import edu.stanford.cs.crypto.efficientct.Verifier;
import edu.stanford.cs.crypto.efficientct.circuit.groups.GroupElement;
import edu.stanford.cs.crypto.efficientct.util.ProofUtils;

import java.math.BigInteger;

public class GeneralizedSchnorrProofVerifier <T extends GroupElement<T>> implements Verifier<DoubleGeneratorParams<T>, T , GeneralizedSchnorrProof<T>> {
    @Override
    public void verify(DoubleGeneratorParams<T> params, T commitment, GeneralizedSchnorrProof<T> proof) throws VerificationFailedException {
        BigInteger challenge = ProofUtils.computeChallenge(params.getGroup().groupOrder(), commitment, proof.getT());
        T verificationPoint = proof.getA().multiply(challenge).add(proof.getT());

        if(!verificationPoint.equals(proof.getS())){
            throw new VerificationFailedException();
        }
    }
}
