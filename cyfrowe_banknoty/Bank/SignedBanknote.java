import java.io.Serializable;
import java.util.Random;
import java.security.MessageDigest;

public class SignedBanknote implements Serializable {
  private byte[] value; // kwota na banknocie
  private byte[] banknoteNumber; // nr identyfikacyjny banknotu
  private byte[][] identificationLeftXor; // niejawna lewa strona nr ID Alice
  private byte[][] identificationRightXor; // niejawna prawa strona nr ID Alice
  private byte[][] identificationLeftHashes; // zahaszowana lewa strona nr ID Alice
  private byte[][] identificationRightHashes; // zahaszowana prawa strona nr ID Alice

  public SignedBanknote() {

  }

  public byte[] getValue() {
    return value;
  }

  public byte[] getBanknoteNumber() {
    return banknoteNumber;
  }

  public byte[][] getIdentificationLeftXor() {
    return identificationLeftXor;
  }

  public byte[][] getIdentificationRightXor() {
    return identificationRightXor;
  }

  public byte[][] getIdentificationLeftHashes() {
    return identificationLeftHashes;
  }

  public byte[][] getIdentificationRightHashes() {
    return identificationRightHashes;
  }

  public void setValue(byte[] _value) {
    value = _value;
  }

  public void setBanknoteNumber(byte[] _banknoteNumber) {
    banknoteNumber = _banknoteNumber;
  }

  public void setIdentificationLeftHashes(byte[][] _identificationLeftHashes) {
    identificationLeftHashes = _identificationLeftHashes;
  }

  public void setIdentificationRightHashes(byte[][] _identificationRightHashes) {
    identificationRightHashes = _identificationRightHashes;
  }
}
