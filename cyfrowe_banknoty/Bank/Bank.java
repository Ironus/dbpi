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

  private void showBanknotes() {
    
  }

  private void receiveHashes() {
    System.out.println("[Bank] Odbieram dane potrzebne do odkrycia banknotow.");

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

    System.out.println("[Bank] Odebrano.");
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
    System.out.println("[Bank] Wybieram banknot do podpisu.");
    Random randomGenerator = new Random();
    int hiddenBanknote = randomGenerator.nextInt(banknotesList.size());
    System.out.println("[Bank] Wybrano banknot nr " + (hiddenBanknote + 1) + ". Odyslam nr banknotu.");

    // odeslij wylosowany banknot
    try {
      dos.writeInt(hiddenBanknote);
    } catch(Exception e) {
      e.printStackTrace();
    }

    // odbierz hashe
    receiveHashes();

    // odkryj banknoty
    showBanknotes();
  }

  private void checkBanknote() {

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
          case "checkbanknote":
            checkBanknote();
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
      // utworz Alice
      Bank bank = new Bank(Integer.parseInt(args[0]));
      System.out.println("[Bank] Czekam na polaczenie...");
      //przelacz Alice na nasluchiwanie
      bank.startListening();
      bank.closeServer();
    } catch (NumberFormatException exNF) {
      System.out.println("Zly format portu.");
    }
    System.out.println("Bank zakonczyl dzialanie.");
  }
}
