import java.io.*;
import java.net.*;
import java.util.Random;
import java.security.MessageDigest;

public class Alice {

  private ServerSocket listeningSocket; // gniazdo do nasluchu
  private static int port; // nr portu
  private DataInputStream dis; // strumien wejscia
  private DataOutputStream dos; // strumien wyjscia

  private byte[] r1; // R1 do hashowania
  private byte[] r2; // R2 do hashowania
  private byte[] r1r2b; // ciag do hashowania
  private byte[] encrypted; // szyfrogram

  private MessageDigest md;

  public Alice(int _port) {
    try {
      port = _port;

      // utworz gniazdo nasluchu i powiaz je z portem (bind)
      listeningSocket= new ServerSocket(port);
    } catch(IOException ex) {
      System.out.println("Blad tworzenia gniazda do nasluchu.");
    }
  }

  private void hashC() {
    try {
      // utworz funkcje hashujaca
      md = MessageDigest.getInstance("SHA-256");
      //wprowadz parametry do funkcji hashujacej
      md.update(r1r2b);
      // wpisz wynik hashowania do zmiennej encrypted
      encrypted = md.digest();
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

      // podejmij decyzje
      Random randomGenerator = new Random();
      int decision = randomGenerator.nextInt(2);
      System.out.println("Moja decyzja to " + decision);

      // wylosuj R1 i R2;
      r1 = new byte[31];
      r2 = new byte[32];
      randomGenerator.nextBytes(r1);
      randomGenerator.nextBytes(r2);

      // konkatenuj r1, r2 i decyzje
      r1r2b = new byte[64];
      for(int i = 0; i < 64; i++) {
        if(i < 31)
          r1r2b[i] = r1[i];
        else if(i == 63)
          r1r2b[i] = (decision == 1 ? (byte)1 : (byte)2);
        else
          r1r2b[i] = r2[i - 31];
      }

      // zaszyfruj wiadomosc
      hashC();

      // wyslij szyfrogram i R1 do Boba
      dos.writeInt(r1.length);
      dos.write(r1, 0, r1.length);
      dos.writeInt(encrypted.length);
      dos.write(encrypted, 0, encrypted.length);

      // odczekaj jakis czas
      Thread.sleep(2000);

      // wyslij R1, R2 i decyzje
      dos.writeInt(r1.length);
      dos.write(r1, 0, r1.length);
      dos.writeInt(r2.length);
      dos.write(r2, 0, r2.length);
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
