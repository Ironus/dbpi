import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.security.MessageDigest;

public class Bob {
  private InetAddress addr; // adres Alice
  private int port; // port
  private Socket socket; // socket do polaczenia z Alice
  private DataOutputStream dos; // strumien wyjscia
  private  DataInputStream dis; // strumien wejscia

  private byte[] r1_1; // R1 do hashowania przeslane jako pierwsze
  private byte[] r1_2; // R1 do hashowania przeslane jako drugie
  private byte[] r2; // R2 do hashowania
  private byte[] r1r2b; // polaczone R1, R2 i decyzja
  private byte[] cAlice; // szyfrogram od Alice
  private byte[] cBob; // szyfrogram Boba
  private byte[] key; // klucz do szyfrogramu
  private int decision; // decyzja Alice

  private MessageDigest md;

  private void hashC() {
    try {
      // utworz funkcje hashujaca
      md = MessageDigest.getInstance("SHA-256");
      // wprowadz parametry do funkcji hashujacej
      md.update(r1r2b);
      // wpisz wynik hashowania do zmiennej encrypted
      cBob = md.digest();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private boolean compare(byte[] _array1, byte[] _array2) {
    if(Arrays.equals(_array1, _array2))
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

      // odbierz szyfrogram i R1 od Alice
      int r1Size = dis.readInt();
      r1_1 = new byte[r1Size];
      dis.read(r1_1, 0, r1Size);
      int cSize = dis.readInt();
      cAlice = new byte[cSize];
      dis.read(cAlice, 0, cSize);

      // odbierz R1, R2 i decyzje od Alice
      r1Size = dis.readInt();
      r1_2 = new byte[r1Size];
      dis.read(r1_2, 0, r1Size);
      int r2Size = dis.readInt();
      r2 = new byte[r2Size];
      dis.read(r2, 0, r2Size);
      decision = dis.readInt();

      // polacz R1, R2 i decyzje
      r1r2b = new byte[64];
      for(int i = 0; i < 64; i++) {
        if(i < 31)
          r1r2b[i] = r1_2[i];
        else if(i == 63)
          r1r2b[i] = (decision == 1 ? (byte)1 : (byte)2);
        else
          r1r2b[i] = r2[i - 31];
      }

      // zahashuj R1, R2 i decyzje
      hashC();

      if(compare(r1_1, r1_2)) {
        if (compare(cAlice, cBob)) {
           System.out.println("Wszystko sie zgadza.");
         } else {
           System.out.println("Alice oszukala. Szyfrogramy nie zgadzaja sie.");
         }
      } else {
        System.out.println("Alice oszukala. Przeslane R1 nie zgadzaja sie.");
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
