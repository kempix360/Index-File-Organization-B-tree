import java.io.IOException;

import memory.*;


public class FileManager {
    DiskFile dataFile;
    RAM ram;

    public FileManager(String filename) throws IOException {
        dataFile = new DiskFile(filename);
        ram = new RAM();
    }

    public void writeRecord(Record record) {
        BlockOfMemory block = ram.loadToBuffer(dataFile);
        ram.writeRecordToBlock(block, record);
        ram.writeToFile(dataFile, block);
    }

    public Record readRecord(int key) {
        BlockOfMemory block = ram.loadToBuffer(dataFile);
        return ram.readRecordFromBlock(block);
    }
}
