package data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import memory.BlockOfMemory;
import memory.Record;

public class KeyboardDataGenerator implements DataGenerator {
    @Override
    public void generateData(String directory, int n, UniqueKeyGenerator generator) throws IOException {
        Scanner scanner = new Scanner(System.in);
        int recordCount = 0;
        int blockNumber = 0;
        int blockSize = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;

        System.out.println("Enter 3 dimensions separated by spaces (e.g., `x y z`), one set per line:");

        while (recordCount < n || n == 0) {
            String blockFilename = directory + "\\block_" + blockNumber + ".txt";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(blockFilename))) {
                int blockRecordCount = 0;

                while ((n == 0 || recordCount < n) && blockRecordCount < blockSize) {
                    System.out.print("Record " + (recordCount + 1) + ": ");
                    try {
                        int first = scanner.nextInt();
                        int second = scanner.nextInt();
                        int third = scanner.nextInt();
                        int key = generator.generateUniqueKey();

                        writer.write(first + " " + second + " " + third + " " + key);
                        writer.newLine();

                        blockRecordCount++;
                        recordCount++;
                    } catch (Exception e) {
                        System.out.println("Invalid input. Please enter three integers separated by spaces.");
                        scanner.nextLine(); // Clear invalid input
                    }
                }

                if (blockRecordCount == 0) {
                    break; // Exit if no records written in this block
                }
            }

            System.out.println("Block " + blockNumber + " written to " + blockFilename);
            blockNumber++;
        }
    }
}