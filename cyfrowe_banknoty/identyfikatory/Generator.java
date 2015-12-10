import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Generator {
  private int[][] identificationNumbers; // tablica z ciagami ID

  public Generator() {
    identificationNumbers = new int[100][31];
  }

  public void generateIDs() {
    // wygeneruj 100 losowych 32bitowych ciagow
    Random randomGenerator = new Random();
    for(int i = 0; i < identificationNumbers.length; i++) {
      for(int j = 0; j < identificationNumbers[i].length; j++)
        identificationNumbers[i][j] = randomGenerator.nextInt(2);
    }
  }

  public void writeToFile() {
    // zapisz ciagi w pliku binarnym
    try {
      // stworz plik
      File file = new File("../Alice.ids");
      // stworz bufor zapisu do pliku
      FileWriter fileWriter = new FileWriter(file);
      for(int i = 0; i < identificationNumbers.length; i++) {
        for(int j = 0; j < identificationNumbers[i].length; j++)
          fileWriter.write(Integer.toString(identificationNumbers[i][j]));
			  fileWriter.write("\n");
      }
			fileWriter.flush();
			fileWriter.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    // stworz generator
    Generator generator = new Generator();

    // wygeneruj identyfikatory
    generator.generateIDs();
    // zapisz identyfikatory do pliku
    generator.writeToFile();
  }
}
