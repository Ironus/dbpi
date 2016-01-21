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

  private byte[] cipherCode(byte[] message) {
    byte[] codedMessage = null;
    try {
      // stworz klase szyfrujaca i ustal parametry szyfrowania
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);

      codedMessage = cipher.doFinal(message);
    } catch(Exception e) {
      e.printStackTrace();
    }

    return codedMessage;
  }

  private byte[] intToByte(int value) {
    return java.nio.ByteBuffer.allocate(4).putInt(value).array();
  }

  private byte[] prepareMessage() {
    // przygotuj wiadomosc
    int value = 100;

    byte[] message = intToByte(value);

    return cipherCode(message);
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
      byte[] codedMessage = prepareMessage();

      // wyslij szyfrogram do Alice
      dos.writeInt(codedMessage.length);
      dos.writeInt(new Integer(57));
      dos.write(codedMessage, 0, codedMessage.length);

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
