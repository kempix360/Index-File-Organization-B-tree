import java.io.File;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;
import data.*;
import database.DatabaseManager;
import memory.DiskFile;

public class Main {
    public static void main(String[] args) {
        String dataDirectory = "src\\disk_files\\data_files";
        String BTreeDirectory = "src\\disk_files\\Btree_files";
        clearDirectory(dataDirectory);
        clearDirectory(BTreeDirectory);
        UniqueKeyGenerator uniqueKeyGenerator = new UniqueKeyGenerator();

        try {
            generateDataToFile(dataDirectory, uniqueKeyGenerator);

            DatabaseManager manager = new DatabaseManager(dataDirectory, BTreeDirectory);
            manager.loadRecordsAndSerializeIndex();

            System.out.println("\nDatabase is ready. Enter commands (type 'help' for a list of commands):");
            CommandProcessor commandProcessor = new CommandProcessor(manager);
            commandProcessor.run();

        } catch (IOException e) {
            System.out.println("An error occurred while generating data or creating the database.");
        }
    }

    public static void clearDirectory(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public static void generateDataToFile(String inputFile, UniqueKeyGenerator generator) throws IOException {
        Scanner scanner = new Scanner(System.in);
        int option;

        System.out.println("Choose option:");
        System.out.println("1. Generate random data");
        System.out.println("2. Insert data from keyboard");
        System.out.println("3. Load data from a text file");

        while (true) {
            try {
                option = scanner.nextInt();
                if (option < 1 || option > 3) {
                    System.out.println("Invalid option. Please choose between 1 and 3.");
                } else {
                    break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
            }
        }

        int n;
        while (true) {
            try {
                System.out.print(option == 3
                        ? "How much data do you want to load? (type 0 to load all data): "
                        : "How much data do you want to generate? ");
                n = scanner.nextInt();
                if (n < 0) {
                    System.out.println("Please enter a positive number.");
                } else {
                    break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.next();
            }
        }

        if (option == 1) {
            DataGenerator randomGenerator = new RandomDataGenerator();
            randomGenerator.generateData(inputFile, n, generator);
        } else if (option == 2) {
            DataGenerator keyboardGenerator = new KeyboardDataGenerator();
            keyboardGenerator.generateData(inputFile, n, generator);
        } else {
            String testFile;
            System.out.print("Provide the name of the file: ");
            scanner.nextLine(); // Consume leftover newline
            while (true) {
                testFile = scanner.nextLine().trim();
                if (testFile.isEmpty()) {
                    System.out.println("File name cannot be empty. Please provide a valid name.");
                } else {
                    break;
                }
            }
            DataGenerator fileGenerator = new FileDataGenerator(testFile);
            fileGenerator.generateData(inputFile, n, generator);
        }
    }
}
