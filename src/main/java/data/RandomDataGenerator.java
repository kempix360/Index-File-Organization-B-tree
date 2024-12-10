package data;

import memory.BlockOfMemory;
import memory.Record;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RandomDataGenerator implements DataGenerator {

    @Override
    public void generateData(String directory, int n, UniqueKeyGenerator generator) throws IOException {
        Random rand = new Random();
        int blockNumber = 0;
        int recordCount = 0;
        int blockSize = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;

        while (recordCount < n) {
            String blockFilename = directory + "\\block_" + blockNumber + ".txt";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(blockFilename))) {
                for (int i = 0; i < blockSize && recordCount < n; i++) {
                    int first = rand.nextInt(100) + 1;
                    int second = rand.nextInt(100) + 1;
                    int third = rand.nextInt(100) + 1;
                    int key = generator.generateUniqueKey();

                    writer.write(first + " " + second + " " + third + " " + key);
                    writer.newLine();
                    recordCount++;
                }
            }

            blockNumber++;
        }
    }
}
