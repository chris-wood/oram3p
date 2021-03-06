package sprout.crypto.oprf;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

import sprout.crypto.CryptoException;
import sprout.crypto.WrongPartyException;
import sprout.crypto.oprf.Message;

// For now we simply use an EC based OPRF. 
// Although, we may want to investigate Elgamal variants in the future. 
public class OPRF {
  // Public keys
  protected ECPoint g, y;
  protected BigInteger n;

  // Private keys
  protected BigInteger k;

  /** 
   * Create a new keyed PRF
   */
  public OPRF() {
    loadSharedKeys();
    k = generateSecretKey();
  }

  /**
   * @param y public key of the secret holder
   */
  public OPRF(ECPoint y) {
    loadSharedKeys();
    this.y = y;
  }

  protected void loadSharedKeys() {
    // TODO: Is this the curve we want to use? 
    X9ECParameters x9 = NISTNamedCurves.getByName("P-224");
    g = x9.getG();
    n = x9.getN();
  }

  public ECPoint getG() {
    return g;
  }

  public BigInteger getN() {
    return n;
  }

  public BigInteger getK() {
    return k;
  }

  public ECPoint getY() {
    if (y == null) {
      y = g.multiply(k);
    }

    return y;
  }

  protected BigInteger generateSecretKey() {
    return randomRange(n);
  }

  /* **********************
   *  Protocol directives *
   ************************/

  /**
   * computes v = H(x)*g^t and w = y^-t
   * 
   * @param msg input x
   * @return Message containing v and w
   * @throws WrongPartyException If you already hold k 
   */
  public Message prepare(String msg) throws CryptoException, WrongPartyException {

    if (k != null) {
      throw new WrongPartyException("Key holder cannot prepare messages");
    }
    
    
    BigInteger t = generateSecretKey();
    
    ECPoint gt = g.multiply(t);
    ECPoint v = hash(msg).add(gt);
    
    ECPoint w = y.multiply(t).negate();
   
    return new Message(v,w);
  }

  /**
   * computes v' = v^k
   * @param msg result of prepare
   * @return v'
   * @throws WrongPartyException If you do not hold k
   */
  // 
  public Message evaluate(Message msg) throws CryptoException, WrongPartyException {
    if (k == null) {
      throw new WrongPartyException("Only the key holder can evaluate");
    }
    return new Message( msg.getV().multiply(k) );
  }

  /** 
   * Receiver computes v' * w
   * @param msg result of evaluate and w from prepare
   * @return H(x)^k
   */
  public Message deblind(Message msg) {
    return new Message( msg.getV().add(msg.getW()) );
  }

  /** 
   * compute v = H(msg)^k
   * @param msg
   * @return v
   * @throws WrongPartyException If you do not hold k.
   */
  public Message evaluate(String msg) throws CryptoException, WrongPartyException {
    if (k == null) {
      throw new WrongPartyException("Only the key holder can evaluate");
    }
    
    return new Message( hash(msg).multiply(k) );
  }

  protected ECPoint hash(String input) throws CryptoException{
    return g.multiply(hash(input.getBytes(), (byte)0).mod(n));
  }

  /**
   * Compute SHA-1
   * 
   * @param message
   * @param selector Key to the hash. Useful for providing multiple unique hash functions. 
   * @return H(selector | message)
   * @throws CryptoException if SHA-1 is not available
   */
  protected BigInteger hash(byte [] message, byte selector) throws CryptoException{

    // input = selector | message
    byte [] input = new byte[message.length + 1];
    System.arraycopy(message, 0, input, 1, message.length);
    input[0] = selector;

    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e1) {
      throw new CryptoException("SHA-1 is not supported");
    }
    digest.reset();

    return new BigInteger(digest.digest(input));
  }

  // TODO: integrate this into the alreday existing random class
  /**
   * Calculate a random number between 0 and range (exclusive)
   * 
   * @param range
   */
  protected BigInteger randomRange(BigInteger range){
    //TODO: Is there anything else we should fall back on here perhaps openssl bn_range
    //         another option is using an AES based key generator (the only algorithim supported by android)

    // TODO: Should we be keeping this rand around? 
    SecureRandom rand = new SecureRandom();
    BigInteger temp = new BigInteger(range.bitLength(), rand);
    while(temp.compareTo(range) >= 0 || temp.equals(BigInteger.ZERO)){
      temp = new BigInteger(range.bitLength(), rand);
    }
    return temp;

  }
}
