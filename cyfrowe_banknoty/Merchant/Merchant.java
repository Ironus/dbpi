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

  private int hiddenBanknote; // nr banknotu do podpisania

  private Banknote banknote; // niepodpisany banknot

  private void receiveHashes() {
    try {
      // odbierz hashe S
      length = dis.readInt();
      temp = new byte[length];
      dis.read(temp, 0, length);
      // wpisz je do banknotu
      banknote.setHashSKeys(temp);

      // odbierz hashe B
      length = dis.readInt();
      temp = new byte[length];
      dis.read(temp, 0, length);
      // wpisz je do banknotu
      greenback.setHashBKeys(temp);

      // odbierz hashe T
      length = dis.readInt();
      temp = new byte[length];
      dis.read(temp, 0, length);
      // wpisz je do banknotu
      greenback.setHashTKeys(temp);

      // odbierz hashe C
      length = dis.readInt();
      temp = new byte[length];
      dis.read(temp, 0, length);
      // wpisz je do banknotu
      greenback.setHashCKeys(temp);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void receiveBanknote() {
    System.out.println("[Sklep] Odbieram banknot.");
    try {
      // stworz banknot
      banknote = new Banknote();

      // odbierz wartosc banknotu
      banknote.setValue(dis.readInt());

      // odbierz nr banknotu
      banknote.setBanknoteNumber(dis.readInt());

      // odbierz lewe hashe
      banknote.setNumberOfHashes(dis.readInt());
      byte[][]temp2 = new byte[banknote.getNumberOfHashes()][];
      for(int i = 0; i < banknote.getNumberOfHashes(); i++) {
        length = dis.readInt();
        temp = new byte[length];
        dis.read(temp, 0, length);
        temp2[i] = show(temp);
      }
        banknote.setIdentificationLeftHashes(temp2);

      // odkryj prawe hashe
      for(int i = 0; i < banknote.getNumberOfHashes(); i++) {
        length = dis.readInt();
        temp = new byte[length];
        dis.read(temp, 0, length);
        temp2[i] = show(temp);
      }
      banknote.setIdentificationRightHashes(temp2);
    } catch(Exception e) {
      e.printStackTrace();
    }

    System.out.println("[Bank] Odebrano.");
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
      // odbierz banknot i jego podpis
      receiveBanknote();
      receiveSignedBanknote();
      // zamknij dataStreamy
      dis.close();
      dos.close();
      // zamknij gniazdo
      socket.close();
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
