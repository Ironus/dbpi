import java.io.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class Alice {
  private ServerSocket listeningSocket; // gniazdo do nasluchu
  private static int port; // nr portu
  private DataInputStream dis; // strumien wejscia
  private DataOutputStream dos; // strumien wyjscia

  private KeyPairGenerator keyPairGenerator; // generator kluczy
  private KeyPair keyPair; // wygenerowana para kluczy
  private Key publicKey; // wygenerowany klucz publiczny
  private Key privateKey; // wygenerowany klucz prywatny
  private Cipher cipher; // klasa szyfrujaca

  public Alice(int _port) {
    try {
      port = _port;

      // utworz gniazdo nasluchu i powiaz je z portem (bind)
      listeningSocket= new ServerSocket(port);
    } catch(IOException ex) {
      System.out.println("Blad tworzenia gniazda do nasluchu.");
    }
  }

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

  private byte[] decipherCode(byte[] codedMessage) {
    byte[] message = null;
    try {
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, privateKey);

      message = cipher.doFinal(codedMessage);
    } catch(Exception e) {
      e.printStackTrace();
    }

    return message;
  }

  private int byteToInt(byte[] message) {
    return java.nio.ByteBuffer.wrap(message).getInt();
  }

  private void startListening() {
    Socket socket = null; // gniazdo dla jednego klienta

    try {
      // czekaj na polaczenie
      socket = listeningSocket.accept();

      // pobierz strumien wejscia i wyjscia, nadbuduj na nich "lepsze" strumienie
      dis = new DataInputStream(socket.getInputStream());
      dos = new DataOutputStream(socket.getOutputStream());

      // wygeneruj klucze
      generateKeys();

      // wyslij klucz publiczny do Boba
      String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
      dos.writeUTF(publicKeyString);

      // odbierz szyfrogram od Boba
      int size = dis.readInt();
      int test = dis.readInt();
      System.out.println(size + " " + test);
      byte[] codedMessage = new byte[size];
      dis.read(codedMessage, 0, size);
      System.out.println("[Alice] Odebralam szyfrogram od Boba");

      byte[] message = decipherCode(codedMessage);

      System.out.print("Wiadomosc od Boba to: ");
      System.out.println(byteToInt(message));

      // zamknij strumienie
      dis.close();
      dos.close();
      // zamknij gniazdo
      socket.close();
      // zamknij gniazdo nasluchujace
      listeningSocket.close();
      } catch(EOFException exEOF) {
        System.out.println("Strumien wejscia jest zbyt krotki.");
      } catch(IOException exIO) {
        System.out.println("Blad strumienia.");
      }
    }

  public static void main(String[] args) {
    // args[0] - nr portu
    if (args.length < 1) {
      System.out.println("Uzycie: Alice nr_portu");
      return;
    }
    try {
      // utworz Alice
      Alice alice = new Alice(Integer.parseInt(args[0]));
      System.out.println("[Alice] Czekam na Boba...");
      //przelacz Alice na nasluchiwanie
      alice.startListening();
    } catch (NumberFormatException exNF) {
      System.out.println("Zly format portu.");
    }
    System.out.println("Alice zakonczyla dzialanie.");
  }
}
