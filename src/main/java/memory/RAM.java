package memory;

import java.io.*;
import java.util.Scanner;

public class RAM {

    private int totalReadOperations;
    private int totalWriteOperations;

    public RAM() {
        totalReadOperations = 0;
        totalWriteOperations = 0;
    }

    public BlockOfMemory loadToBuffer(DiskFile file) {
        // Initialize fileInputStream and scanner if not already done
        byte[] buffer = new byte[BlockOfMemory.BUFFER_SIZE];
        int bufferIndex = 0;
        Scanner scanner = file.getScanner();

        // Loop through the file reading each integer as a token
        while (scanner.hasNextInt() && bufferIndex < BlockOfMemory.BUFFER_SIZE) {
            int number = scanner.nextInt();

            // Convert the integer to 4 bytes
            buffer[bufferIndex] = (byte) (number >> 24);
            buffer[bufferIndex + 1] = (byte) (number >> 16);
            buffer[bufferIndex + 2] = (byte) (number >> 8);
            buffer[bufferIndex + 3] = (byte) number;
            bufferIndex += 4;
        }

        // If we’ve read data, return a new BlockOfMemory
        if (bufferIndex > 0) {
            totalReadOperations++;
            return new BlockOfMemory(buffer, bufferIndex);
        } else {
            return null; // No more data to read
        }
    }


    public void writeToFile(DiskFile file, BlockOfMemory _blockOfMemory) {
        if (_blockOfMemory == null) {
            return;
        }

        try {
            DataOutputStream outputStream = new DataOutputStream(file.getFileOutputStream());
            byte[] data = _blockOfMemory.getBuffer();
            int size = _blockOfMemory.getSize();
            int index = _blockOfMemory.getIndex();

            for (int i = index; i < size; i += 4) {
                if (i + 3 < size) {
                    int number = ((data[i] & 0xFF) << 24) |
                            ((data[i + 1] & 0xFF) << 16) |
                            ((data[i + 2] & 0xFF) << 8) |
                            (data[i + 3] & 0xFF);
                    outputStream.writeBytes(number + " ");

                    // After every third integer, write a newline
                    if ((i / 4 + 1) % 3 == 0) {
                        outputStream.writeBytes("\n");
                    }

                    data[i] = 0;
                    data[i + 1] = 0;
                    data[i + 2] = 0;
                    data[i + 3] = 0;
                }
            }

            totalWriteOperations++;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Record readRecordFromBlock(BlockOfMemory blockOfMemory) {
        if (blockOfMemory == null) {
            return new Record(-1, -1, -1);
        }

        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int index = blockOfMemory.getIndex();
        int recordSize = Record.RECORD_SIZE;

        int[] record_values = new int[3];
        int values_index = 0;

        if (index < 0 || (index + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return new Record(-1, -1, -1);
        }

        for (int i = index; i < index + recordSize; i += 4) {

            if (i + 3 < size) {
                int number = ((data[i] & 0xFF) << 24) |
                        ((data[i + 1] & 0xFF) << 16) |
                        ((data[i + 2] & 0xFF) << 8) |
                        (data[i + 3] & 0xFF);
                record_values[values_index] = number;
                values_index++;
            }
        }

        return new Record(record_values[0], record_values[1], record_values[2]);
    }

    public void writeRecordToBlock(BlockOfMemory blockOfMemory, Record record) {
        if (blockOfMemory == null) {
            return;
        }

        if (record == null || record.getFirst() == -1 || record.getSecond() == -1 || record.getThird() == -1) {
            return;
        }

        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int recordSize = Record.RECORD_SIZE;

        if ((size + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return;
        }

        int[] record_values = {record.getFirst(), record.getSecond(), record.getThird()};
        int values_index = 0;

        for (int i = size; i < size + recordSize; i += 4) {
            int number = record_values[values_index];
            data[i] = (byte) (number >> 24);
            data[i + 1] = (byte) (number >> 16);
            data[i + 2] = (byte) (number >> 8);
            data[i + 3] = (byte) number;
            values_index++;
        }

        blockOfMemory.setSize(size + recordSize);

    }

    public int getTotalReadOperations() {
        return totalReadOperations;
    }

    public int getTotalWriteOperations() {
        return totalWriteOperations;
    }

}