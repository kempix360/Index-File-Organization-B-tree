package data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class RandomDataGenerator implements DataGenerator {
    @Override
    public void generateData(String filename, int n, UniqueKeyGenerator generator) throws IOException {
        Random rand = new Random();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < n; i++) {
                int first = rand.nextInt(100) + 1;
                int second = rand.nextInt(100) + 1;
                int third = rand.nextInt(100) + 1;
                int key = generator.generateUniqueKey();

                writer.write(first + " " + second + " " + third + " " + key);
                writer.newLine();
            }
        }
    }
}
