import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class Merchant {
  private int port;
  private ServerSocket listeningSocket; // gniazdo no nasluchu
  private DataInputStream disAlice; // strumien wejscia Alice
  private DataOutputStream dosAlice; // strumien wyjscia Alice

  private int hiddenBanknote; // nr banknotu do podpisania

  private Banknote banknote; // niepodpisany banknot
  private SignedBanknote signedBanknote; // podpisany banknot

  private void receiveHashes() {
    try {
      // odbierz hashe S
      int length = disAlice.readInt();
      byte[] temp = new byte[length];
      disAlice.read(temp, 0, length);
      // wpisz je do banknotu
      banknote.setHashSKeys(temp);

      // odbierz hashe B
      length = disAlice.readInt();
      temp = new byte[length];
      disAlice.read(temp, 0, length);
      // wpisz je do banknotu
      banknote.setHashBKeys(temp);

      // odbierz hashe T
      length = disAlice.readInt();
      temp = new byte[length];
      disAlice.read(temp, 0, length);
      // wpisz je do banknotu
      banknote.setHashTKeys(temp);

      // odbierz hashe C
      length = disAlice.readInt();
      temp = new byte[length];
      disAlice.read(temp, 0, length);
      // wpisz je do banknotu
      banknote.setHashCKeys(temp);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void receiveSignedBanknote() {
    System.out.println("[Sklep] Odbieram sygnature.");

    try {
      // stworz banknot
      signedBanknote = new SignedBanknote();

      // odbierz wartosc banknotu
      int length = disAlice.readInt();
      byte[] temp = new byte[length];
      disAlice.read(temp, 0, length);
      signedBanknote.setValue(temp);

      // odbierz nr banknotu
      length = disAlice.readInt();
      temp = new byte[length];
      disAlice.read(temp, 0, length);
      signedBanknote.setBanknoteNumber(temp);

      // odbierz lewe hashe
      signedBanknote.setNumberOfHashes(disAlice.readInt());
      byte[][]temp2 = new byte[signedBanknote.getNumberOfHashes()][];
      for(int i = 0; i < signedBanknote.getNumberOfHashes(); i++) {
        length = disAlice.readInt();
        temp = new byte[length];
        disAlice.read(temp, 0, length);
        temp2[i] = temp;
      }
      signedBanknote.setIdentificationLeftHashes(temp2);

      // odbierz prawe hashe
      for(int i = 0; i < signedBanknote.getNumberOfHashes(); i++) {
        length = disAlice.readInt();
        temp = new byte[length];
        disAlice.read(temp, 0, length);
        temp2[i] = temp;
      }
      signedBanknote.setIdentificationRightHashes(temp2);
    } catch(Exception e) {
      e.printStackTrace();
    }

    System.out.println("[Sklep] Odebrano.");
  }

  private void receiveBanknote() {
    System.out.println("[Sklep] Odbieram banknot.");
    try {
      // stworz banknot
      banknote = new Banknote();

      // odbierz wartosc banknotu
      banknote.setValue(disAlice.readInt());

      // odbierz nr banknotu
      banknote.setBanknoteNumber(disAlice.readInt());

      // odbierz lewe hashe
      banknote.setNumberOfHashes(disAlice.readInt());
      byte[][]temp2 = new byte[banknote.getNumberOfHashes()][];
      int length;
      byte[] temp;
      for(int i = 0; i < banknote.getNumberOfHashes(); i++) {
        length = disAlice.readInt();
        temp = new byte[length];
        disAlice.read(temp, 0, length);
        temp2[i] = temp;
      }
        banknote.setIdentificationLeftHashes(temp2);

      // odkryj prawe hashe
      for(int i = 0; i < banknote.getNumberOfHashes(); i++) {
        length = disAlice.readInt();
        temp = new byte[length];
        disAlice.read(temp, 0, length);
        temp2[i] = temp;
      }
      banknote.setIdentificationRightHashes(temp2);
    } catch(Exception e) {
      e.printStackTrace();
    }

    System.out.println("[Sklep] Odebrano.");
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
      Socket socketAlice = listeningSocket.accept();
      // utworz strumienie wejsca i wyjscia
      disAlice = new DataInputStream(socketAlice.getInputStream());
      dosAlice = new DataOutputStream(socketAlice.getOutputStream());
      // odbierz banknot i jego podpis
      receiveBanknote();
      receiveSignedBanknote();
      // zamknij dataStreamy
      disAlice.close();
      dosAlice.close();
      // zamknij gniazdo
      socketAlice.close();
    } catch(IOException exIO) {
      System.out.println("Blad strumienia.");
    }
  }

  public Merchant(int _port) {
    try {
      port = _port;
      // utworz gniazdo nasluchu i powiaz je z portem (bind)
      listeningSocket= new ServerSocket(port);
    } catch(IOException ex) {
      System.out.println("Blad tworzenia gniazda do nasluchu.");
    }
  }

  public static void main(String[] args) {
    // args[0] - nr portu
    if (args.length < 1) {
      System.out.println("Uzycie: Merchant nr_portu");
      return;
    }
    try {
      while(true) {
        // utworz Bank
        Merchant merchant = new Merchant(Integer.parseInt(args[0]));
        System.out.println("[Sklep] Czekam na polaczenie...");
        //przelacz Bank na nasluchiwanie

        merchant.startListening();
        merchant.closeServer();
      }
    } catch (NumberFormatException exNF) {
      System.out.println("Zly format portu.");
    }
    System.out.println("Sklep zakonczyl dzialanie.");
  }
}
