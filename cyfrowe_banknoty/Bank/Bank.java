import java.io.*;
import java.util.*;
import java.net.*;

public class Bank {
  private int port;
  private ServerSocket listeningSocket; // gniazdo no nasluchu
  private DataInputStream dis; // strumien wejscia
  private DataOutputStream dos; // strumien wyjscia

  private ArrayList<Banknote> banknotesList;

  private byte[] show() {
    byte[] message;

    try {
      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, privateKey);

      message = cipher.doFinal(cBob);
    } catch(Exception e) {
      e.printStackTrace();
    }

    return message;
  }

  private int byteToInt(byte[] array) {
    return array[0] << 24 | (array[1] & 0xFF) << 16 | (array[2] & 0xFF) << 8 | (array[3] & 0xFF);
  }

  private void receiveBanknotes() {
    // zczytaj ilosc banknotow, ktore ma Alice
    int banknotesListSize = dis.readInt();
    // stworz liste banknotow dla Alice
    banknotesList = new ArrayList<Banknote>(banknotesListSize);

    for(Banknote greenback : banknotesList) {
      int length;
      byte[] temp;

      // stworz banknot
      greenback = new Banknote();

      // odkryj wartosc banknotu
      length = dis.readInt();
      greenback.setValue(byteToInt(show(dis.read(temp, 0, length))));

      // odkryj nr banknotu
      length = dis.readInt();
      greenback.setBanknoteNumber(byteToInt(show(dis.read(temp, 0, length))));

      // odkryj lewe hashe
      greenback.setNumberOfHashes(dis.readInt());
      byte[][]temp2;
      for(int i = 0; i < greenback.numberOfHashes; i++) {
        length = dis.readInt();
        temp2[i] = show(dis.read(temp, 0, length));
      }
      greenback.setIdentificationLeftHashes(temp2);

      // odkryj prawe hashe
      for(int i = 0; i < greenback.numberOfHashes; i++) {
        length = dis.readInt();
        temp2[i] = show(dis.read(temp, 0, length));
      }
      greenback.setIdentificationRightHashes(temp2);

      banknotesList.add(greenback);
    }

    // to do
    Random randomGenerator = new Random();
  }

  private void signBanknote() {
    receiveBanknotes();
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
          case "signbanknote":
            System.out.println("[Bank] Podpisuje banknot");
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
