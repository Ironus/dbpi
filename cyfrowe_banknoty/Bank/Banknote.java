import java.io.Serializable;
import java.util.Random;
import java.security.MessageDigest;

public class Banknote implements Serializable {
  private int value; // kwota na banknocie
  private int numberOfHashes; // ilosc zahaszowanych ciagow
  private int banknoteNumber; // nr identyfikacyjny banknotu
  private int[] identificationLeftXor; // niejawna lewa strona nr ID Alice
  private int[] identificationRightXor; // niejawna prawa strona nr ID Alice
  private byte[][] identificationLeftHashes; // zahaszowana lewa strona nr ID Alice
  private byte[][] identificationRightHashes; // zahaszowana prawa strona nr ID Alice
  private byte[] hashSKeys; // klucze S do hashowania lewego Xora
  private byte[] hashBKeys; // klucze B do hashowania lewego Xora
  private byte[] hashTKeys; // klucze T do hashowania prawego Xora
  private byte[] hashCKeys; // klucze C do hashowania prawego Xora

  private void generateBanknoteNumber() {
    Random randomGenerator = new Random();
    banknoteNumber = randomGenerator.nextInt((int)Math.pow(2, 31) - 1);
  }

  private void generateXor(int[] _identificationNumbers) {
    identificationLeftXor = new int[_identificationNumbers.length];
    identificationRightXor = new int[_identificationNumbers.length];

    Random randomGenerator = new Random();
    // dla kazdego ciagu identyfikujacego Alice wylosuj lewa strone xora i oblicz prawa
    for(int i = 0; i < _identificationNumbers.length; i++) {
      identificationLeftXor[i] = randomGenerator.nextInt((int)Math.pow(2,31) - 1);
      identificationRightXor[i] = _identificationNumbers[i] ^ identificationLeftXor[i];
    }
  }

  private byte generateHashKey() {
    // stworz generator
    Random randomGenerator = new Random();
    // stworz zmienna pomocnicza
    byte[] temp = new byte[1];

    // wygeneruj losowy byte
    randomGenerator.nextBytes(temp);

    // zwroc losowy byte
    return temp[0];
  }

  private void generateHashes() {
    // utworz tablice do przetrzymywania Hashy
    identificationLeftHashes = new byte[identificationLeftXor.length][];
    identificationRightHashes = new byte[identificationRightXor.length][];
    // utworz tablice do przetrzymywania kluczy hashowania
    hashSKeys = new byte[identificationLeftXor.length];
    hashBKeys = new byte[identificationLeftXor.length];
    hashTKeys = new byte[identificationRightXor.length];
    hashCKeys = new byte[identificationRightXor.length];

    try {
      // dla kazdego Xora wygeneruj hash
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for(int i = 0; i < identificationLeftHashes.length; i++) {
        // wygeneruj klucze hashowania dla lewej strony
        hashSKeys[i] = generateHashKey();
        hashBKeys[i] = generateHashKey();

        // stworz zmienne pomocnicze
        byte[] xorTemp = new byte[4];
        xorTemp[0] = (byte)(identificationLeftXor[i] >> 24);
        xorTemp[1] = (byte)(identificationLeftXor[i] >> 16);
        xorTemp[2] = (byte)(identificationLeftXor[i] >> 8);
        xorTemp[3] = (byte)(identificationLeftXor[i] >> 0);

        byte[] temp = new byte[2 + xorTemp.length];
        temp[0] = hashSKeys[i];
        temp[1] = hashBKeys[i];
        for(int j = 0; j < xorTemp.length; j++)
          temp[2 + j] = xorTemp[j];

        // wygeneruj hash
        md.update(temp);
        identificationLeftHashes[i] = md.digest();

        //wygeneruj klucze hashowania dla prawej strony
        hashTKeys[i] = generateHashKey();
        hashCKeys[i] = generateHashKey();

        // nadpisz zmienne pomocnicze
        xorTemp = new byte[4];
        xorTemp[0] = (byte)(identificationRightXor[i] >> 24);
        xorTemp[1] = (byte)(identificationRightXor[i] >> 16);
        xorTemp[2] = (byte)(identificationRightXor[i] >> 8);
        xorTemp[3] = (byte)(identificationRightXor[i] >> 0);

        temp = new byte[2 + xorTemp.length];
        temp[0] = hashSKeys[i];
        temp[1] = hashBKeys[i];
        for(int j = 0; j < xorTemp.length; j++)
          temp[2 + j] = xorTemp[j];

        // wygeneruj hash
        md.update(temp);
        identificationRightHashes[i] = md.digest();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public Banknote() {

  }

  public Banknote(int _value) {
    value = _value;

    // wygeneruj identyfikator banknotu
    generateBanknoteNumber();
  }

  public Banknote(int _value, int[] _identificationNumbers) {
    value = _value;

    // wygeneruj identyfikator banknotu
    generateBanknoteNumber();
    generateXor(_identificationNumbers);
    generateHashes();
  }

  public int getBanknoteNumber() {
    return banknoteNumber;
  }

  public byte[][] getIdentificationLeftHashes() {
    return identificationLeftHashes;
  }

  public byte[][] getIdentificationRightHashes() {
    return identificationRightHashes;
  }

  public byte[] getHashSKeys() {
    return hashSKeys;
  }

  public byte[] getHashBKeys() {
    return hashBKeys;
  }

  public byte[] getHashTKeys() {
    return hashTKeys;
  }

  public byte[] getHashCKeys() {
    return hashCKeys;
  }

  public void setValue(int _value) {
    value = _value;
  }

  public void setNumberOfHashes(int _numberOfHashes) {
    numberOfHashes = _numberOfHashes;
  }

  public void setBanknoteNumber(int _banknoteNumber) {
    banknoteNumber = _banknoteNumber;
  }

  public void setIdentificationLeftHashes(byte[][] _identificationLeftHashes) {
    identificationLeftHashes = _identificationLeftHashes;
  }

  public void setIdentificationRightHashes(byte[][] _identificationRightHashes) {
    identificationRightHashes = _identificationRightHashes;
  }

  public void setHashSKeys(byte[] _hashSKeys) {
    hashSKeys = _hashSKeys;
  }

  public void setHashBKeys(byte[] _hashBKeys) {
    hashSKeys = _hashBKeys;
  }

  public void setHashTKeys(byte[] _hashTKeys) {
    hashSKeys = _hashTKeys;
  }

  public void setHashCKeys(byte[] _hashCKeys) {
    hashSKeys = _hashCKeys;
  }
}
