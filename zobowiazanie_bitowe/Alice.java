import java.io.*;
import java.net.*;
import java.util.Random;
import java.security.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.apache.commons.codec.binary.Base64;

public class Alice {

  private ServerSocket listeningSocket; // gniazdo do nasluchu
  private static int port; // nr portu
  private DataInputStream dis; // strumien wejscia
  private DataOutputStream dos; // strumien wyjscia

  byte[] r; // R do zobowiazania bitowego
  byte[] encrypted; // szyfrogram

  private KeyGenerator keygenerator;  // generator kluczy
  private SecretKey desKey; // wygenerowany klucz
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

  private void cipherCode() {
    try {
      // stworz generator kluczy i wygeneruj klucz prywatny
      keygenerator = KeyGenerator.getInstance("DES");
      desKey = keygenerator.generateKey();

      // stworz klase szyfrujaca i ustal parametry szyfrowania
      cipher = Cipher.getInstance("DES/ECB/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, desKey);

      encrypted = cipher.doFinal(r);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void startListening() {
    Socket socket = null; // gniazdo dla jednego klienta

    try {
      // czekaj na polaczenie
      socket = listeningSocket.accept();

      // pobierz strumien wejscia i wyjscia, nadbuduj na nich "lepsze" strumienie
      dis = new DataInputStream(socket.getInputStream());
      dos = new DataOutputStream(socket.getOutputStream());

      // pobierz wartosc R ze strumienia
      int rSize = dis.readInt();
      r = new byte[rSize + 1];
      dis.read(r, 0, rSize);

      // podejmij decyzje
      Random randomGenerator = new Random();
      int decision = randomGenerator.nextInt(2);
      System.out.println("Moja decyzja to " + decision);

      r[rSize] = (decision == 1 ? (byte)1 : (byte)2);

      // zaszyfruj wiadomosc
      cipherCode();

      // wyslij szyfrogram do Boba
      dos.writeInt(encrypted.length);
      dos.write(encrypted, 0, encrypted.length);

      // odczekaj jakis czas
      Thread.sleep(2000);

      // wyslij klucz i decyzje
      dos.writeInt(desKey.getEncoded().length);
      dos.write(desKey.getEncoded(), 0, desKey.getEncoded().length);
      dos.writeInt(decision);

      // zamknij strumienie
      dis.close();
      dos.close();
      // zamknij gniazdo
      socket.close();
      // zamknij gniazdo nasluchujace
      listeningSocket.close();
      } catch(EOFException exEOF) {
        System.out.println("Strumien wejscia jest krotszy niz 4B.");
      } catch(IOException exIO) {
        System.out.println("Blad strumienia.");
      } catch(InterruptedException exIn) {
        System.out.println("Watek zostal przerwany.");
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
