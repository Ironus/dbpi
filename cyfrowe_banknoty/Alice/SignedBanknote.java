import java.io.Serializable;
import java.util.Random;
import java.security.MessageDigest;

public class SignedBanknote implements Serializable {
  private int numberOfHashes;
  private byte[] value; // kwota na banknocie
  private byte[] banknoteNumber; // nr identyfikacyjny banknotu
  private byte[][] identificationLeftXor; // niejawna lewa strona nr ID Alice
  private byte[][] identificationRightXor; // niejawna prawa strona nr ID Alice
  private byte[][] identificationLeftHashes; // zahaszowana lewa strona nr ID Alice
  private byte[][] identificationRightHashes; // zahaszowana prawa strona nr ID Alice

  public SignedBanknote() {

  }

  public int getNumberOfHashes() {
    return numberOfHashes;
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

  public void setNumberOfHashes(int _numberOfHashes) {
    numberOfHashes = _numberOfHashes;
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

  public void setIdentificationLeftXor(byte[][] _identificationLeftXor) {
    identificationLeftXor = _identificationLeftXor;
  }

  public void setIdentificationRightXor(byte[][] _identificationRightXor) {
    identificationRightXor = _identificationRightXor;
  }
}
