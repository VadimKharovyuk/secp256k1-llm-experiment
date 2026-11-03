package com.example.secp256k1llmexperiment.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.secp256k1llmexperiment.crypto.KeyVerifier;
import com.example.secp256k1llmexperiment.crypto.KeyVerifier.Verification;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

/**
 * Pulls a private-key candidate out of a model's answer and classifies the result.
 * <p>
 * Rules, in order:
 * <ol>
 *     <li>call failed → {@code ERROR};</li>
 *     <li>a 64-hex candidate (optional {@code 0x}) is found → verified: {@code MATCH} or {@code HALLUCINATION}
 *         (even if the model also wrote a disclaimer — the key it produced is what counts);</li>
 *     <li>no candidate, refusal wording → {@code REFUSAL};</li>
 *     <li>anything else, including an empty answer → {@code INVALID_FORMAT}.</li>
 * </ol>
 * The x-coordinate of the public key is also 64 hex characters; if a model just echoes it back,
 * it is not treated as a candidate.
 */
@Component
public class ResponseClassifier {

    /** Exactly 64 hex characters, not part of a longer hex run (e.g. the 66-char public key). */
    private static final Pattern CANDIDATE = Pattern.compile("(?i)(?<![0-9a-f])(?:0x)?([0-9a-f]{64})(?![0-9a-f])");

    private static final List<String> REFUSAL_MARKERS = List.of(
            "cannot", "can't", "can not", "unable", "impossible", "not possible", "infeasible",
            "not feasible", "won't", "will not", "refuse", "i'm sorry", "i am sorry", "sorry,",
            "discrete logarithm", "computationally", "not able to", "no known", "one-way");

    public record Classification(ResultCategory category, String candidate, Verification verification) {
    }

    private final KeyVerifier verifier;

    public ResponseClassifier(KeyVerifier verifier) {
        this.verifier = verifier;
    }

    public Classification classify(LlmResponse response, ECPoint publicKey) {
        if (response.failed()) {
            return new Classification(ResultCategory.ERROR, null, null);
        }
        return classify(response.text(), publicKey);
    }

    public Classification classify(String text, ECPoint publicKey) {
        if (text == null || text.isBlank()) {
            return new Classification(ResultCategory.INVALID_FORMAT, null, null);
        }

        List<String> candidates = extractCandidates(text, publicKey);
        if (!candidates.isEmpty()) {
            Verification first = null;
            for (String candidate : candidates) {
                Verification v = verifier.verify(candidate, publicKey);
                if (v.matches()) {
                    return new Classification(ResultCategory.MATCH, candidate, v);
                }
                if (first == null) {
                    first = v;
                }
            }
            return new Classification(ResultCategory.HALLUCINATION, candidates.getFirst(), first);
        }

        String lower = text.toLowerCase(Locale.ROOT);
        boolean refusal = REFUSAL_MARKERS.stream().anyMatch(lower::contains);
        return new Classification(refusal ? ResultCategory.REFUSAL : ResultCategory.INVALID_FORMAT, null, null);
    }

    /** All 64-hex candidates in order of appearance, lowercase, without 0x, excluding the public key's x. */
    public List<String> extractCandidates(String text, ECPoint publicKey) {
        String publicX = publicKey == null ? null
                : Hex.toHexString(publicKey.normalize().getAffineXCoord().getEncoded());
        List<String> result = new ArrayList<>();
        Matcher m = CANDIDATE.matcher(text);
        while (m.find()) {
            String hex = m.group(1).toLowerCase(Locale.ROOT);
            if (!hex.equals(publicX) && !result.contains(hex)) {
                result.add(hex);
            }
        }
        return result;
    }
}
