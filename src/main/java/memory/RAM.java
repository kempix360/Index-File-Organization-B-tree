package memory;

import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class RAM {

    private int totalReadOperations;
    private int totalWriteOperations;

    public RAM() {
        totalReadOperations = 0;
        totalWriteOperations = 0;
    }

    public BlockOfMemory loadToBufferFromData(DiskFile file, int blockNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.getFilename()))) {
            int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
            int recordsToSkip = blockNumber * b;
            int currentLine = 0;

            String line;
            while (currentLine < recordsToSkip && reader.readLine() != null) {
                currentLine++;
            }

            byte[] buffer = new byte[BlockOfMemory.BUFFER_SIZE];
            int bufferIndex = 0;

            while ((line = reader.readLine()) != null && bufferIndex < BlockOfMemory.BUFFER_SIZE) {
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    try {
                        int number = Integer.parseInt(token);

                        buffer[bufferIndex] = (byte) (number >> 24);
                        buffer[bufferIndex + 1] = (byte) (number >> 16);
                        buffer[bufferIndex + 2] = (byte) (number >> 8);
                        buffer[bufferIndex + 3] = (byte) number;
                        bufferIndex += 4;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (bufferIndex >= BlockOfMemory.BUFFER_SIZE) {
                        break;
                    }
                }
            }

            if (bufferIndex > 0) {
                totalReadOperations++;
                return new BlockOfMemory(buffer, bufferIndex);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public PageBtreeFile loadNodeFromTree(DiskFile file, int pageNumber) {
        String filename = file.getFilename();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = null;
            int currentLine = 0;

            // Read the specific line
            while ((line = reader.readLine()) != null) {
                if (currentLine == pageNumber) {
                    break;
                }
                currentLine++;
            }

            if (line == null) {
                throw new IOException("Line " + pageNumber + " not found in file.");
            }

            return deserializeNode(line);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static PageBtreeFile deserializeNode(String line) {
        String[] parts = line.split("\\|");

        // Parse required fields
        int pageID = Integer.parseInt(parts[0].split("=")[1].trim());
        int parentPageID = Integer.parseInt(parts[1].split("=")[1].trim());
        int numKeys = Integer.parseInt(parts[2].split("=")[1].trim());
        int numChildren = Integer.parseInt(parts[3].split("=")[1].trim());

        // Parse keys
        List<Integer> keys = new ArrayList<>();
        if (!parts[4].split("=")[1].trim().equals("[]")) {
            for (String key : parts[4].split("=")[1].trim().replace("[", "").replace("]", "").split(", ")) {
                keys.add(Integer.parseInt(key));
            }
        }

        // Parse addresses
        List<Integer> addresses = new ArrayList<>();
        if (!parts[5].split("=")[1].trim().equals("[]")) {
            for (String address : parts[5].split("=")[1].trim().replace("[", "").replace("]", "").split(", ")) {
                addresses.add(Integer.parseInt(address));
            }
        }

        // Parse children
        List<Integer> children = new ArrayList<>();
        if (!parts[6].split("=")[1].trim().equals("[]")) {
            for (String child : parts[6].split("=")[1].trim().replace("[", "").replace("]", "").split(", ")) {
                children.add(Integer.parseInt(child));
            }
        }

        return new PageBtreeFile(pageID, parentPageID, keys, addresses, children);
    }


    public void writeToDataFile(DiskFile file, BlockOfMemory _blockOfMemory) {
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
            return new Record(-1, -1, -1, -1);
        }

        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int index = blockOfMemory.getIndex();
        int recordSize = Record.RECORD_SIZE;

        int[] record_values = new int[4];
        int values_index = 0;

        if (index < 0 || (index + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return new Record(-1, -1, -1, -1);
        }

        for (int i = index; i < index + recordSize; i += 4) {

            if (i + 4 < size) {
                int number = ((data[i] & 0xFF) << 24) |
                        ((data[i + 1] & 0xFF) << 16) |
                        ((data[i + 2] & 0xFF) << 8) |
                        (data[i + 3] & 0xFF);
                record_values[values_index] = number;
                values_index++;
            }
        }

        return new Record(record_values[0], record_values[1], record_values[2], record_values[3]);
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