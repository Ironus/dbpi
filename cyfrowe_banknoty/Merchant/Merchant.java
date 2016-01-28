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
  private InetAddress addrBank; // adres Banku
  private int portBank; // port Banku
  private ServerSocket listeningSocket; // gniazdo no nasluchu
  private Socket socketAlice; // socket do polaczenia z Alice
  private Socket socketBank; // socket do polaczenia z Bankiem
  private DataInputStream disAlice; // strumien wejscia Alice
  private DataOutputStream dosAlice; // strumien wyjscia Alice
  private DataInputStream disBank; // strumien wejscia Banku
  private DataOutputStream dosBank; // strumien wyjscia Banku

  private Banknote banknote; // niepodpisany banknot
  private SignedBanknote signedBanknote; // podpisany banknot

  private boolean compare(byte[] _array1, byte[] _array2) {
    if(Arrays.equals(_array1, _array2))
      return true;
    return false;
  }

  private byte[] generateHash(byte left, byte right, byte[] middle) {
    byte[] hash = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] temp = new byte[2 + middle.length];
      temp[0] = left;
      temp[1] = right;
      for(int j = 0; j < middle.length; j++)
        temp[2 + j] = middle[j];

      // wygeneruj hash
      md.update(temp);
      hash = md.digest();
    } catch(Exception e) {
      e.printStackTrace();
    }

    return hash;
  }

  private boolean storeBanknote() {
    try {
      sendSignedBanknote();
      if(disBank.readInt() == 1) {
        return true;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private void sendBanknote() {
    try {
      // wyslij do wartosc banknotu
      dosBank.writeInt(banknote.getValue());

      // wyslij nr identyfikacyjny banknotu
      dosBank.writeInt(banknote.getBanknoteNumber());

      // wyslij lewe hashe
      dosBank.writeInt(banknote.getIdentificationLeftHashes().length);
      for(byte[] hash : banknote.getIdentificationLeftHashes()) {
        dosBank.writeInt(hash.length);
        dosBank.write(hash, 0, hash.length);
      }

      // wyslij prawie hashe
      for(byte[] hash : banknote.getIdentificationRightHashes()) {
        dosBank.writeInt(hash.length);
        dosBank.write(hash, 0, hash.length);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private boolean sendSignature() {
    try {
      // wyslij wartosc
      byte[] temp = signedBanknote.getValue();
      dosBank.writeInt(temp.length);
      dosBank.write(temp, 0, temp.length);

      // wyslij nr identyfikacyjny banknotu
      temp = signedBanknote.getBanknoteNumber();
      dosBank.writeInt(temp.length);
      dosBank.write(temp, 0, temp.length);

      // wez wszystkie lewe hashe, ukryj i wyslij
      dosBank.writeInt(signedBanknote.getIdentificationLeftHashes().length);
      for(byte[] hash : signedBanknote.getIdentificationLeftHashes()) {
        temp = hash;
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);
      }

      // wez wszystkie prawie hashe, ukryj i wyslij
      for(byte[] hash : signedBanknote.getIdentificationRightHashes()) {
        temp = hash;
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);
      }

      sendBanknote();
      // jesli Bank potwierdzi, zwroc true
      if(disBank.readInt() == 1) {
        return true;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private void sendSignedBanknote() {
    try {
      // wyslij wartosc
      byte[] temp = signedBanknote.getValue();
      dosBank.writeInt(temp.length);
      dosBank.write(temp, 0, temp.length);

      // wyslij nr identyfikacyjny banknotu
      temp = signedBanknote.getBanknoteNumber();
      dosBank.writeInt(temp.length);
      dosBank.write(temp, 0, temp.length);

      // wez wszystkie lewe hashe, ukryj i wyslij
      dosBank.writeInt(signedBanknote.getIdentificationLeftHashes().length);
      for(byte[] hash : signedBanknote.getIdentificationLeftHashes()) {
        temp = hash;
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);
      }

      // wez wszystkie prawie hashe, ukryj i wyslij
      for(byte[] hash : signedBanknote.getIdentificationRightHashes()) {
        temp = hash;
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);
      }

      sendBanknote();
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

      // odbierz prawe hashe
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
      socketAlice = listeningSocket.accept();
      // utworz strumienie wejsca i wyjscia
      disAlice = new DataInputStream(socketAlice.getInputStream());
      dosAlice = new DataOutputStream(socketAlice.getOutputStream());
      // odbierz banknot i jego podpis
      receiveBanknote();
      receiveSignedBanknote();

      // utworz gniazdo dla Banku i podlacz je do addr:port
      socketBank = new Socket(addrBank, portBank);
      // pobierz strumienie i zbuduj na nich lepsze strumienie
      dosBank = new DataOutputStream(socketBank.getOutputStream());
      disBank = new DataInputStream(socketBank.getInputStream());

      // wyslij sygnature
      System.out.println("[Sklep] Sprawdzam podpis.");
      dosBank.writeUTF("checksignature");
      if(!sendSignature()) {
        System.out.println("[Sklep] Proba oszustwa.");
        dosAlice.writeInt(0);
        disAlice.close();
        dosAlice.close();
        socketAlice.close();
      } else {
        System.out.println("[Sklep] Zgadza sie.");
        dosAlice.writeInt(1);
        // stworz generator liczb pseudolosowych
        Random randomGenerator = new Random();
        int number;
        boolean hashIsFine = true;

        System.out.println("[Sklep] Rozpoczynam wysylanie zadan xor.");
        for(int i = 0; i < banknote.getNumberOfHashes(); i++) {
          // wylosuj 0 = lewo 1 = prawo
          number = randomGenerator.nextInt(2);
          // przekaz los Alice
          dosAlice.writeInt(number);

          // odbierz xora
          int length = disAlice.readInt();
          int[] temp = new int[length];
          for(int j = 0; j < length; j++) {
            temp[j] = disAlice.readInt();
          }

          // odbierz hash key'e
          length = disAlice.readInt();
          byte[] temp2 = new byte[length];
          disAlice.read(temp2, 0, length);

          length = disAlice.readInt();
          byte[] temp3 = new byte[length];
          disAlice.read(temp3, 0, length);

          if(number == 1) {
            // wstaw prawego xora i hash keye do banknotu
            banknote.setIdentificationRightXor(temp);
            banknote.setHashTKeys(temp2);
            banknote.setHashCKeys(temp3);

            if(compare(generateHash(temp2[0], temp3[0], banknote.getIdentificationRightXorByteArray()[i]), signedBanknote.getIdentificationRightHashes()[i])) {
              hashIsFine = false;
            }
          } else {
            // wstaw lewego xora
            banknote.setIdentificationLeftXor(temp);
            banknote.setHashSKeys(temp2);
            banknote.setHashBKeys(temp3);

            if(compare(generateHash(temp2[0], temp3[0], banknote.getIdentificationLeftXorByteArray()[i]), signedBanknote.getIdentificationLeftHashes()[i])) {
              hashIsFine = false;
            }
          }
        }

        // sprawdz zobowiazanie bitowe
        if(hashIsFine) {
          dosAlice.writeInt(1);
        } else {
          dosAlice.writeInt(0);
        }
        // zdeponuj banknot
        dosBank.writeUTF("storebanknote");
        System.out.println("[Sklep] Wysylam banknot do depozytu.");
        if(storeBanknote()) {
          System.out.println("[Sklep] Banknot zdeponowano.");
        } else {
          System.out.println("[Sklep] Nastapila proba oszustwa.");
        }
      }
      dosBank.writeUTF("closeconnection");

      // zamknij dataStreamy Alice i Banku
      disAlice.close();
      dosAlice.close();
      socketAlice.close();
      disBank.close();
      dosBank.close();
      socketBank.close();
    } catch(IOException exIO) {
      System.out.println("Blad strumienia.");
    }
  }

  public Merchant(int _port, InetAddress _addrBank, int _portBank) {
    try {
      port = _port;
      addrBank = _addrBank;
      portBank = _portBank;
      // utworz gniazdo nasluchu i powiaz je z portem (bind)
      listeningSocket= new ServerSocket(port);
    } catch(IOException ex) {
      System.out.println("Blad tworzenia gniazda do nasluchu.");
    }
  }

  public static void main(String[] args) {
    // args[0] - nr portu
    if (args.length < 3) {
      System.out.println("Uzycie: Merchant nr_portu adres_banku nr_portu");
      return;
    }
    try {
      // utworz Sklep
      Merchant merchant = new Merchant(Integer.parseInt(args[0]), InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
      System.out.println("[Sklep] Czekam na polaczenie...");
      //przelacz sklep na nasluchiwanie

      merchant.startListening();
      merchant.closeServer();
    } catch (NumberFormatException exNF) {
      System.out.println("Zly format portu.");
    } catch (UnknownHostException exUH) {
      System.out.println("Blad wprowadzonego adresu hosta.");
    }
    System.out.println("Sklep zakonczyl dzialanie.");
  }
}
