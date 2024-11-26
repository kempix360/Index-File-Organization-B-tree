package data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class RandomDataGenerator implements DataGenerator {
    @Override
    public void generateData(String filename, int n) throws IOException {
        Random rand = new Random();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < n; i++) {
                int number = rand.nextInt(100) + 1;
                writer.write(Integer.toString(number));
                number = rand.nextInt(100) + 1;
                writer.write(" " + number);
                number = rand.nextInt(100) + 1;
                writer.write(" " + number);
                writer.newLine();
            }
        }
    }
}
