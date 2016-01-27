import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class Bank {
  private int port;
  private ServerSocket listeningSocket; // gniazdo no nasluchu
  private DataInputStream dis; // strumien wejscia
  private DataOutputStream dos; // strumien wyjscia

  private ArrayList<Banknote> banknotesList; // lista banknotow

  private KeyPairGenerator keyPairGenerator; // generator kluczy
  private KeyPair keyPair; // wygenerowana para kluczy
  private Key publicKey; // wygenerowany klucz publiczny
  private Key privateKey; // wygenerowany klucz prywatny
  private Cipher cipher; // klasa szyfrujaca

  private int hiddenBanknote; // nr banknotu do podpisania

  private void generateKeys() {
    try {
      // stworz generator kluczy i wygeneruj parę kluczy
      keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024);
      keyPair = keyPairGenerator.generateKeyPair();

      // wyciagnij klucze z pary kluczy
      publicKey = keyPair.getPublic();
      privateKey = keyPair.getPrivate();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private byte[] generateHash(byte left, byte right, byte[] middle) {
    byte[] hash = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] temp = new byte[2 + middle.length];
      temp[0] = left;
      temp[1] = right;
      for(int j = 0; j < middle.length; j++)
        temp[2 + j] = middle[j];

      // wygeneruj hash
      md.update(temp);
      hash = md.digest();
    } catch(Exception e) {
      e.printStackTrace();
    }

    return hash;
  }

  private boolean compare(byte[] _array1, byte[] _array2) {
    if(Arrays.equals(_array1, _array2))
      return true;
    return false;
  }

  private int[] getIdentificationNumbers(String file, int size) {
    int[] identificationNumbers = new int[size];

    try {
      // stworz bufor
      BufferedReader buffer = new BufferedReader(new FileReader(file));
      String line;
      int id = 0, power = 0, currentLine = 0;
      while((line = buffer.readLine()) != null) {
        for(int i = line.length() - 1; i >= 0; i--)
          id += (line.charAt(i) - 48) * (int)Math.pow(2, power++);
        identificationNumbers[currentLine++] = id;
        id = 0;
        power = 0;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    return identificationNumbers;
  }

  private boolean checkBanknotes() {
    System.out.println("[Bank] Sprawdzam banknoty.");
    // wez wartosc z pierwszego banknotu
    int banknotesValue = banknotesList.get(0).getValue();
    // i porownaj z pozostalymi
    for(Banknote greenback : banknotesList) {
      // jesli wartosci sa rozne, to zwroc false
      if(greenback.getValue() != banknotesValue)
        return false;
    }

    // dla kazdego z banknotow
    for(int i = 0; i < banknotesList.size(); i++) {
      Banknote firstGreenback = banknotesList.get(i);

      for(int j = i + 1; j < banknotesList.size(); j++) {
        Banknote secondGreenback = banknotesList.get(j);

        // porownaj nr identyfikacyjne i jesli sie powtarza, to zwroc false
        if(firstGreenback.getBanknoteNumber() == secondGreenback.getBanknoteNumber())
          return false;
      }

      if(i != hiddenBanknote) {
        // sprawdz zobowiazanie bitowe
        byte[] hashSKeys = firstGreenback.getHashSKeys();
        byte[] hashBKeys = firstGreenback.getHashBKeys();
        byte[][] leftXorByteArray = firstGreenback.getIdentificationLeftXorByteArray();
        byte[][] leftHashes = firstGreenback.getIdentificationLeftHashes();

        for(int j = 0; j < leftHashes.length; j++) {
          if(compare(generateHash(hashSKeys[j], hashBKeys[j], leftXorByteArray[j]), leftHashes[j])) {
            return false;
          }
        }

        byte[] hashTKeys = firstGreenback.getHashTKeys();
        byte[] hashCKeys = firstGreenback.getHashCKeys();
        byte[][] rightXorByteArray = firstGreenback.getIdentificationRightXorByteArray();
        byte[][] rightHashes = firstGreenback.getIdentificationRightHashes();

        for(int j = 0; j < rightHashes.length; j++) {
          if(compare(generateHash(hashTKeys[j], hashCKeys[j], rightXorByteArray[j]), rightHashes[j])) {
            return false;
          }
        }

        // sprawdz ciagi identyfikujace osobe
        int[] banknoteIdentificationNumbers = new int[firstGreenback.getIdentificationLeftXor().length];
        for(int j = 0; j < banknoteIdentificationNumbers.length; j++) {
          banknoteIdentificationNumbers[j] = firstGreenback.getIdentificationLeftXor()[j] ^ firstGreenback.getIdentificationRightXor()[j];
        }

        int[] personIdentificationNumbers = getIdentificationNumbers("../Alice.ids", banknoteIdentificationNumbers.length);

        if(!Arrays.equals(banknoteIdentificationNumbers, personIdentificationNumbers)) {
          return false;
        }
      }
    }

    // jesli wszystkie testy przeszly zwroc true
    return true;
  }

  private void receiveHashes() {
    for(int i = 0; i < banknotesList.size(); i++) {
      // z wyjatkiem podpisywanego
      if(i != hiddenBanknote) {
        // stworz banknot pomocniczy
        Banknote greenback = banknotesList.get(i);

        // stworz zmienna i tablice pomocnicza
        int length;
        byte[] temp = null;

        try {
          // odbierz hashe S
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          // wpisz je do banknotu
          greenback.setHashSKeys(temp);

          // odbierz hashe B
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          // wpisz je do banknotu
          greenback.setHashBKeys(temp);

          // wyslij hashe T
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          // wpisz je do banknotu
          greenback.setHashTKeys(temp);

          // wyslij hashe C
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          // wpisz je do banknotu
          greenback.setHashCKeys(temp);
        } catch(Exception e) {
          e.printStackTrace();
        }

        // podmien banknot w liscie na ten z dodanymi hashami
        banknotesList.set(i, greenback);
      }
    }
  }

  private byte[] show(byte[] codedMessage) {
    // tablica z wynikiem
    byte[] message = null;

    // odszyfruj
    try {
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, privateKey);

      message = cipher.doFinal(codedMessage);
    } catch(Exception e) {
      e.printStackTrace();
    }

    // zwroc
    return message;
  }

  private byte[] hide(byte[] message) {
    // tablica z wynikiem
    byte[] codedMessage = null;

    // odszyfruj
    try {
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, privateKey);

      codedMessage = cipher.doFinal(message);
    } catch(Exception e) {
      e.printStackTrace();
    }

    // zwroc
    return codedMessage;
  }

  private void sendBanknote() {
    System.out.println("[Bank] Podpisuje i wysylam banknot.");
    try {
      // dla podpisywanego banknotu
      Banknote greenback = banknotesList.get(hiddenBanknote);
      // stworz tablice pomocnicza i wpisz do niej wartosc banknotu, a nastepnie
      // zaszyfruj kluczem prywatnym banku
      byte[] temp = hide(greenback.getValueByteArray());

      // wyslij
      dos.writeInt(temp.length);
      dos.write(temp, 0, temp.length);

      // wpisz do tablicy zaszyfrowany nr identyfikacyjny banknotu
      temp = hide(greenback.getBanknoteNumberByteArray());
      dos.writeInt(temp.length);
      dos.write(temp, 0, temp.length);

      // wez wszystkie lewe hashe, ukryj i wyslij
      dos.writeInt(greenback.getIdentificationLeftHashes().length);
      for(byte[] hash : greenback.getIdentificationLeftHashes()) {
        temp = hide(hash);
        dos.writeInt(temp.length);
        dos.write(temp, 0, temp.length);
      }

      // wez wszystkie prawie hashe, ukryj i wyslij
      for(byte[] hash : greenback.getIdentificationRightHashes()) {
        temp = hide(hash);
        dos.writeInt(temp.length);
        dos.write(temp, 0, temp.length);
      }

      // wez wszystkie lewe czesci xora, ukryj i wyslij
      for(byte[] xor : greenback.getIdentificationLeftXorByteArray()) {
        temp = hide(xor);
        dos.writeInt(temp.length);
        dos.write(temp, 0, temp.length);
      }

      // wez wszystkie prawe czesci xora, ukryj i wyslij
      for(byte[] xor : greenback.getIdentificationRightXorByteArray()) {
        temp = hide(xor);
        dos.writeInt(temp.length);
        dos.write(temp, 0, temp.length);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    System.out.println("[Bank] Wyslano.");
  }

  private int byteToInt(byte[] array) {
    // zamien tablice byte na int i zwroc
    return java.nio.ByteBuffer.wrap(array).getInt();
  }

  private void receiveBanknotes() {
    System.out.println("[Bank] Odbieram banknoty.");
    try {
      // odczytaj ilosc banknotow, ktore ma Alice
      int banknotesListSize = dis.readInt();
      // stworz liste banknotow dla Alice
      banknotesList = new ArrayList<Banknote>(banknotesListSize);

      for(int banknoteCounter = 0; banknoteCounter < banknotesListSize; banknoteCounter++) {
        // stworz banknot
        Banknote greenback = new Banknote();

        // odkryj wartosc banknotu
        int length = dis.readInt();
        byte[] temp = new byte[length];
        dis.read(temp, 0, length);
        greenback.setValue(byteToInt(show(temp)));

        // odkryj nr banknotu
        length = dis.readInt();
        temp = new byte[length];
        dis.read(temp, 0, length);
        greenback.setBanknoteNumber(byteToInt(show(temp)));

        // odkryj lewe hashe
        greenback.setNumberOfHashes(dis.readInt());
        byte[][]temp2 = new byte[greenback.getNumberOfHashes()][];
        for(int i = 0; i < greenback.getNumberOfHashes(); i++) {
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          temp2[i] = show(temp);
        }
        greenback.setIdentificationLeftHashes(temp2);

        // odkryj prawe hashe
        for(int i = 0; i < greenback.getNumberOfHashes(); i++) {
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          temp2[i] = show(temp);
        }
        greenback.setIdentificationRightHashes(temp2);

        // odkryj lewe XORy
        for(int i = 0; i < greenback.getNumberOfHashes(); i++) {
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          temp2[i] = show(temp);
        }
        int[] xor = new int[temp2.length];
        for(int i = 0; i < xor.length; i++) {
          xor[i] = byteToInt(temp2[i]);
        }
        greenback.setIdentificationLeftXor(xor);

        // odkryj prawe XORy
        for(int i = 0; i < greenback.getNumberOfHashes(); i++) {
          length = dis.readInt();
          temp = new byte[length];
          dis.read(temp, 0, length);
          temp2[i] = show(temp);
        }
        xor = new int[temp2.length];
        for(int i = 0; i < xor.length; i++) {
          xor[i] = byteToInt(temp2[i]);
        }
        greenback.setIdentificationRightXor(xor);

        banknotesList.add(greenback);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    System.out.println("[Bank] Odebrano.");
  }

  private void signBanknote() {
    // odbierz wszystkie banknoty
    receiveBanknotes();

    // wybierz, ktory banknot podpisac
    Random randomGenerator = new Random();
    int hiddenBanknote = randomGenerator.nextInt(banknotesList.size());

    // odeslij wylosowany banknot
    try {
      dos.writeInt(hiddenBanknote);
    } catch(Exception e) {
      e.printStackTrace();
    }

    // odbierz hashe
    receiveHashes();

    // sprawdz banknoty
    if(!checkBanknotes()) {
      // jesli cos nie tak, to zwroc komunikat i przerwij dzialanie
      System.out.println("[Bank] Alice probowala oszukac. Przerywam dzialanie.");
    } else {
      sendBanknote();
    }
  }

  private void checkSignature() {

  }

  public void closeServer() {
    try {
      listeningSocket.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void startListening() {
    try {
      // stworz gniazdo dla jednego klienta i zaakceptuj polaczenie
      Socket socket = listeningSocket.accept();
      // utworz strumienie wejsca i wyjscia
      dis = new DataInputStream(socket.getInputStream());
      dos = new DataOutputStream(socket.getOutputStream());
      // dopoki polaczenie aktywne przyjmuj zadania
      Boolean isConnectionActive = new Boolean("true");
      while(isConnectionActive.booleanValue()) {
        switch(dis.readUTF().toLowerCase()) {
          case "sendpublickey":
            System.out.println("[Bank] Wysylam klucz publiczny.");
            String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            dos.writeUTF(publicKeyString);
            System.out.println("[Bank] Wyslano.");
            break;
          case "signbanknote":
            System.out.println("[Bank] Otrzymano zadanie podpisania banknotu.");
            signBanknote();
            break;
          case "checksiganture":
            checkSignature();
            break;
          case "closeconnection":
            isConnectionActive = new Boolean("false");
            break;
          default:
            System.out.println("Blad zadania.");
        }
      }
      // zamknij dataStreamy
      dis.close();
      dos.close();
      // zamknij gniazdo
      socket.close();

    } /*catch(EOFException exEOF) {
      System.out.println("Strumien wejscia jest krotszy niz 4B.");
    }*/ catch(IOException exIO) {
      System.out.println("Blad strumienia.");
    } /*catch(InterruptedException exIn) {
      System.out.println("Watek zostal przerwany.");
    }*/
  }

  public Bank(int _port) {
    try {
      port = _port;
      // utworz gniazdo nasluchu i powiaz je z portem (bind)
      listeningSocket= new ServerSocket(port);
      // stworz liste banknotow
      banknotesList = new ArrayList<Banknote>();
      generateKeys();
    } catch(IOException ex) {
      System.out.println("Blad tworzenia gniazda do nasluchu.");
    }
  }

  public static void main(String[] args) {
    // args[0] - nr portu
    if (args.length < 1) {
      System.out.println("Uzycie: Bank nr_portu");
      return;
    }
    try {
      while(true) {
        // utworz Bank
        Bank bank = new Bank(Integer.parseInt(args[0]));
        System.out.println("[Bank] Czekam na polaczenie...");
        //przelacz Bank na nasluchiwanie

        bank.startListening();
        bank.closeServer();
      }
    } catch (NumberFormatException exNF) {
      System.out.println("Zly format portu.");
    }
    System.out.println("Bank zakonczyl dzialanie.");
  }
}
