import java.io.IOException;

import memory.*;


public class DatabaseManager {
    DiskFile dataFile;
    DiskFile indexFile;
    BlockOfMemory dataBlock;
    BlockOfMemory indexBlock;
    RAM ram;
    BTree bTree;

    public DatabaseManager(DiskFile _dataFile) throws IOException {
        this.dataFile = _dataFile;
        this.indexFile = new DiskFile("src\\disk_files\\index_page.in");
        ram = new RAM();
        bTree = new BTree(3);
    }

    public void loadRecordsAndSerializeIndex() throws IOException {
        BlockOfMemory block;

        int location = 0;
        int blockNumber = 0;
        while ((block = ram.loadToBufferFromData(dataFile, blockNumber)) != null) {
            int index = 0;

            while (index < block.getSize()) {
                Record record = ram.readRecordFromBlock(block);
                if (record.getFirst() != -1) {
                    int key = record.getKey();
                    bTree.insert(key, location);
                    location++;
                }
                block.setIndex(index + Record.RECORD_SIZE);
                index = block.getIndex();
            }
            blockNumber++;
        }

        bTree.serialize(indexFile);
        System.out.println("End of serialization.");
    }

    public Record search(int key) {
        Integer lineNumber = bTree.search(key);

        if (lineNumber == null) {
            System.out.println("Record not found.");
            return new Record(-1, -1, -1, -1);
        }

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;

        int blockNumber = lineNumber / b;

        BlockOfMemory block = ram.loadToBufferFromData(dataFile, blockNumber);

        int index = (lineNumber % b) * Record.RECORD_SIZE;
        block.setIndex(index);

        Record record = ram.readRecordFromBlock(block);
        System.out.println("Record found at line " + lineNumber + ": " + record.toString());
        return record;
    }


}
