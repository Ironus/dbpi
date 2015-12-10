import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class Bob {
  private InetAddress addr; // adres Alice
  private int port; // port
  private Socket socket; // socket do polaczenia z Alice
  private DataOutputStream dos; // strumien wyjscia
  private  DataInputStream dis; // strumien wejscia

  private Key publicKey; // otrzymany klucz publiczny
  X509EncodedKeySpec publicKeySpec; // specyfikacja do generowania klucza
  KeyFactory keyFactory; // generator kluczy
  private Cipher cipher; // klasa szyfrujaca

  private byte[] message; // wiadomosc do wyslania
  private byte[] cBob; // szyfrogram

  private void prepareMessage() {
    // przygotuj wiadomosc
    message = new byte[8];

    // stworz rng i wylosuj wiadomosc
    Random randomGenerator = new Random();
    randomGenerator.nextBytes(message);

    System.out.println("Wiadomosc dla Alice:");
    for(int i = 0; i < message.length; i++)
      System.out.print(message[i]);
    System.out.println();
  }

  private void cipherCode() {
    try {
      // stworz klase szyfrujaca i ustal parametry szyfrowania
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);

      cBob = cipher.doFinal(message);
    } catch(Exception e) {
      e.printStackTrace();
    }
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

      // odbierz klucz publiczny od Alice
      String publicKeyString = dis.readUTF();
      byte[] publicKeyByteArray = Base64.getDecoder().decode(publicKeyString);
      publicKeySpec = new X509EncodedKeySpec(publicKeyByteArray);
      keyFactory = KeyFactory.getInstance("RSA");
      publicKey = keyFactory.generatePublic(publicKeySpec);

      System.out.println("[Bob] Odebralem klucz publiczny od Alice");

      // przygotuj wiadomosc dla Alice
      prepareMessage();

      // przygotuj szyfrogram
      cipherCode();

      // wyslij szyfrogram do Alice
      dos.writeInt(cBob.length);
      dos.write(cBob, 0, cBob.length);

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
