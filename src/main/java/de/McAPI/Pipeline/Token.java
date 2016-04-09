package de.McAPI.Pipeline;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by Yonas on 09.04.2016.
 */
public class Token {

    private final static Random pseudoRandom = new Random();
    private final static SecureRandom secureRandom = new SecureRandom();

    private Token() {}

    /**
     * This methods creates a new secure token.
     * @return
     */
    public static String generateToken() {
        return new BigInteger(1028, Token.secureRandom).toString(32);
    }

    /**
     * This method generates a new pseudo token.
     * @return
     */
    public static String pseudo() {
        return new BigInteger(32, Token.pseudoRandom).toString(32);
    }

    /**
     * Generates a secure signature.
     * @return
     */
    public static String generateSignature() {
        return new BigInteger(2048, Token.secureRandom).toString(32);
    }

}
