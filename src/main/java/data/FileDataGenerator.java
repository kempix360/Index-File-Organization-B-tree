package data;

import memory.BlockOfMemory;
import memory.Record;

import java.io.*;


public class FileDataGenerator implements DataGenerator {
    private final String sourceFile;

    public FileDataGenerator(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public void generateData(String directory, int n, UniqueKeyGenerator generator) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            int recordCount = 0;
            int blockNumber = 0;
            int blockSize = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;

            while (recordCount < n || n == 0) {
                String blockFilename = directory + "\\block_" + blockNumber + ".txt";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(blockFilename))) {
                    int blockRecordCount = 0;

                    while ((line = reader.readLine()) != null && (n == 0 || recordCount < n) && blockRecordCount < blockSize) {
                        writer.write(line);
                        writer.newLine();
                        blockRecordCount++;
                        recordCount++;
                    }

                    if (blockRecordCount == 0) {
                        break; // Exit if no more records to write
                    }
                }

                System.out.println("Block " + blockNumber + " written to " + blockFilename);
                blockNumber++;
            }
        }
    }
}
