package data;

import java.io.File;
import java.io.IOException;

public interface DataGenerator {
    void generateData(String directory, int n, UniqueKeyGenerator generator) throws IOException;

    static void createFile(String name) {
        File file = new File(name);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("An error occurred while creating the file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
