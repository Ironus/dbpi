import java.io.*;
import java.net.*;

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
  private Banknote[] banknotes; // tablica banknotow
  private Banknote banknote; // banknot wybrany przez Bank

  private int[] identificationNumbers;

  private void sendBanknotes() {
    for(Banknote greenback : banknotes) {
      dos.writeInt(greenback.getValue());
    }
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

      // wyslij zadanie podpisania banknotow
      dosBank.writeUTF("signbanknote");

      // wyslij banknoty
      sendBanknotes();

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
