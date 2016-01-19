import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class Alice {
  private InetAddress addrBank; // adres Banku
  private InetAddress addrShop; // adres Sklepu
  private int portBank; // port Banku
  private int portShop; // port Sklepu
  private Socket socketBank; // socket do polaczenia z Bankiem
  private Socket socketShop; // socket do polaczenia ze Sklepem
  private DataOutputStream dosBank; // strumien wyjscia na Bank
  private DataOutputStream dosShop; // strumien wyjscia na Sklep
  private DataInputStream disBank; // strumien wejscia od Banku
  private DataInputStream disShop; // strumien wejscia od Sklepu

  private int value; // kwota na banknocie
  private int signedBanknote; // indeks w tablicy podpisanego banknotu
  private Banknote[] banknotes; // tablica banknotow
  private Banknote banknote; // banknot wybrany przez Bank

  private Key publicKeyBank;  // klucz publiczny banku
  private Cipher cipher;  //klasa szyfrujaca

  private int[] identificationNumbers; // nr identyfikacyjne Alice

  private void sendHashKeys() {
    // stworz zmienna do przetrzymywania tablicy
    byte[] temp = null;

    try {
      // dla kazdego banknotu
      for(int i = 0; i < banknotes.length; i++) {
        // z wyjatkiem podpisywanego
        if(i != signedBanknote) {
          // wyslij hashe S
          temp = banknotes[i].getHashSKeys();
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);

          // wyslij hashe B
          temp = banknotes[i].getHashBKeys();
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);

          // wyslij hashe T
          temp = banknotes[i].getHashTKeys();
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);

          // wyslij hashe C
          temp = banknotes[i].getHashCKeys();
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void getPublicKey() {
    try {
      System.out.println("[Alice] Odbieram klucz publiczny Banku.");
      String publicKeyString = disBank.readUTF();
      System.out.println("[Alice] Odebrano.");
      byte[] publicKeyByteArray = Base64.getDecoder().decode(publicKeyString);
      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByteArray);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      publicKeyBank = keyFactory.generatePublic(publicKeySpec);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private byte[] hide(byte[] message) {
    byte[] codedMessage = null;
    try {
      // stworz klase szyfrujaca i ustal parametry szyfrowania
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, publicKeyBank);

      codedMessage = cipher.doFinal(message);
    } catch(Exception e) {
      e.printStackTrace();
    }

    return codedMessage;
  }

  private void sendBanknotes() {
    try {
      // wyslij ile jest banknotow
      dosBank.writeInt(banknotes.length);

      // dla kazdego banknotu
      for(Banknote greenback : banknotes) {
      // zakryj go i wyslij do banku
        // stworz tablice pomocnicza i wpisz do niej wartosc banknotu, a nastepnie
        // zaszyfruj kluczem publicznym banku
        byte[] temp = hide(greenback.getValueByteArray());

        // wyslij do banku
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);

        // wpisz do tablicy zaszyfrowany nr identyfikacyjny banknotu
        temp = hide(greenback.getBanknoteNumberByteArray());
        dosBank.writeInt(temp.length);
        dosBank.write(temp, 0, temp.length);

        // wez wszystkie lewe hashe, ukryj i wyslij
        dosBank.writeInt(greenback.getIdentificationLeftHashes().length);
        for(byte[] hash : greenback.getIdentificationLeftHashes()) {
          temp = hide(hash);
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);
        }

        // wez wszystkie prawie hashe, ukryj i wyslij
        for(byte[] hash : greenback.getIdentificationRightHashes()) {
          temp = hide(hash);
          dosBank.writeInt(temp.length);
          dosBank.write(temp, 0, temp.length);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    //}
    System.out.println("[Alice] Wyslano.");
  }

  private void getIdentificationNumbers() {
    try {
      // stworz bufor
      BufferedReader buffer = new BufferedReader(new FileReader("../Alice.ids"));
      String line;
      int id = 0, power = 0, currentLine = 0;
      while((line = buffer.readLine()) != null) {
        for(int i = line.length() - 1; i >= 0; i--)
          id += (line.charAt(i) - 48) * (int)Math.pow(2, power++);
        identificationNumbers[currentLine++] = id;
        id = 0;
        power = 0;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void openConnection(String socketType) {
    try {
      if(socketType == "socketBank") {
        // utworz gniazdo dla Banku i podlacz je do addr:port
        socketBank = new Socket(addrBank, portBank);
        // pobierz strumienie i zbuduj na nich lepsze strumienie
        dosBank = new DataOutputStream(socketBank.getOutputStream());
        disBank = new DataInputStream(socketBank.getInputStream());
      } else {
        // utworz gniazdo dla Banku i podlacz je do addr:port
        socketShop = new Socket(addrShop, portShop);
        dosShop = new DataOutputStream(socketShop.getOutputStream());
        disShop = new DataInputStream(socketShop.getInputStream());
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void closeConnection(String socketType) {
    try {
      // zamknij strumienie wejscia i wyjscia oraz gniazdo
      if(socketType == "socketBank") {
        dosBank.writeUTF("closeconnection");
        dosBank.close();
        disBank.close();
        socketBank.close();
      } else {
        dosShop.close();
        disShop.close();
        socketShop.close();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public Alice(InetAddress _addrBank, int _portBank, InetAddress _addrShop, int _portShop) {
    try {
      addrBank = _addrBank;
      portBank = _portBank;
      addrBank = _addrShop;
      portBank = _portShop;

      // pobierz swoje identyfikatory
      System.out.println("[Alice] Pobieram identyfikatory.");
      identificationNumbers = new int[100];
      getIdentificationNumbers();

      // stworz banknoty
      System.out.println("[Alice] Tworzę banknoty.");
      value = 100;
      banknotes = new Banknote[100];

      for(int i = 0; i < 100; i++)
        banknotes[i] = new Banknote(value, identificationNumbers);

      // polacz z Bankiem
      openConnection("socketBank");

      // odbierz klucz publiczny banku
      dosBank.writeUTF("sendpublickey");
      getPublicKey();

      // wyslij zadanie podpisania banknotow
      dosBank.writeUTF("signbanknote");

      // wyslij banknoty
      System.out.println("[Alice] Wysylam banknoty.");
      sendBanknotes();

      // odbierz nr banknotu, ktory bedzie podpisany
      System.out.println("[Alice] Odbieram nr banknotu, ktory bedzie podpisany.");
      signedBanknote = disBank.readInt();
      System.out.println("[Alice] Odebrano.");

      // wyslij dane potrzebne do odkrycia pozostalych banknotow
      System.out.println("[Alice] Wysylam pozostale dane.");
      sendHashKeys();

      // zamknij polaczenie z Bankiem
      closeConnection("socketBank");

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length < 4) {
      System.out.println("Uzycie: Alice adres_banku nr_portu adres_sklepu nr_portu");
    } else {
      try {
        // ustal adres banku i sklepu i stworz Alice
        Alice alice = new Alice(InetAddress.getByName(args[0]),     Integer.parseInt(args[1]), InetAddress.getByName(args[2]),  Integer.parseInt(args[3]));
      } catch(Exception e) {
        e.printStackTrace();
      }
      System.out.println("Alice zakonczyla dzialanie");
    }
  }
}
