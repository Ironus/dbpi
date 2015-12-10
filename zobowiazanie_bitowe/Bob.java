import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.security.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Bob {
  private InetAddress addr; // adres Alice
  private int port; // port
  private Socket socket; // socket do polaczenia z Alice
  private DataOutputStream dos; // strumien wyjscia
  private  DataInputStream dis; // strumien wejscia

  private byte[] r; // R do zobowiazania bitowego
  private byte[] cAlice; // szyfrogram od Alice
  private byte[] cBob; // szyfrogram Boba
  private byte[] key; // klucz do szyfrogramu
  private int decision; // decyzja Alice

  private SecretKeyFactory keyFactory; // generator klucza z tablicy byte
  private SecretKey desKey; // wygenerowany klucz
  private Cipher cipher; // klasa szyfrujaca

  private void prepareR() {
    // przygotuj R
    r = new byte[8];

    // stworz rng i wylosuj R
    Random randomGenerator = new Random();
    randomGenerator.nextBytes(r);
  }

  private void cipherCode() {
    try {
      // wygeneruj klucz prywatny
      keyFactory = SecretKeyFactory.getInstance("DES");
      desKey = keyFactory.generateSecret(new DESKeySpec(key));

      // stworz klase szyfrujaca i ustal parametry szyfrowania
      cipher = Cipher.getInstance("DES/ECB/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, desKey);

      cBob = cipher.doFinal(r);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private boolean compare() {
    if(Arrays.equals(cAlice, cBob))
      return true;
    return false;
  }

  public Bob(InetAddress _addr, int _port) {
    try {
      addr = _addr;
      port = _port;
      // utworz gniazdo i podlacz je do addr:port
      socket = new Socket(addr, port);

      // pobierz strumienie i zbuduj na nich lepsze strumienie
      dos = new DataOutputStream(socket.getOutputStream());
      dis = new DataInputStream(socket.getInputStream());

      // przygotuj r dla Alice
      prepareR();

      // wyslij wartosc R do Alice
      dos.writeInt(r.length - 1);
      dos.write(r, 0, r.length - 1);

      // odbierz szyfrogram od Alice
      int cSize = dis.readInt();
      cAlice = new byte[cSize];
      dis.read(cAlice, 0, cSize);

      // odbierz klucz i decyzje od Alice
      int keySize = dis.readInt();
      key = new byte[keySize];
      dis.read(key, 0, keySize);
      decision = dis.readInt();

      // dopisz decyzje Alice do r
      r[r.length - 1] = (decision == 1 ? (byte)1 : (byte)2);

      // zaszyfruj r z decyzja za pomoca klucza od Alice
      cipherCode();

      if(compare()) {
        System.out.println("Alice nie oszukala. Szyfrogramy sa takie same. Jej decyzja to " + decision);
      } else {
        System.out.println("Alice oszukala. Szyfrogramy nie zgadzaja sie.");
      }

      // zamkniecie strumieni
      dis.close();
      dos.close();

      // zamkniecie socketu
      socket.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Uzycie: Bob adres_hosta nr_portu");
    }
    try {
      // ustal adres serwera i stworz Boba
      Bob bob = new Bob(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Bob zakonczyl dzialanie");
  }
}
