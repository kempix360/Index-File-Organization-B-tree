import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;
import data.*;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

    }


    public static void generateDataToFile(String inputFile) throws IOException {

        Scanner scanner = new Scanner(System.in);
        int option;
        System.out.println("Choose option:");
        System.out.println("1. Generate random data");
        System.out.println("2. Insert data from keyboard");
        System.out.println("3. Load data from a text file");
        while (true) {
            try {
                option = scanner.nextInt(); // Try reading the input
                if (option < 1 || option > 3) {  // Ensure the option is valid
                    System.out.println("Invalid option. Please choose between 1 and 3.");
                } else {
                    break;  // Exit loop if valid option
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();  // Clear the invalid input from scanner buffer
            }
        }
        int n;
        while (true) {
            try {
                if (option == 3) {
                    System.out.print("How much data do you want to load? (type 0 if you want to load all data) ");
                }
                else System.out.print("How much data do you want to generate? ");
                n = scanner.nextInt();
                if (n < 0) {
                    System.out.println("Please enter a positive number.");
                } else {
                    break;  // Valid number, break out of the loop
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.next();  // Clear invalid input
            }
        }


        if (option == 1) {
            DataGenerator randomGenerator = new RandomDataGenerator();
            randomGenerator.generateData(inputFile, n);

        } else if (option == 2) {
            DataGenerator randomGenerator = new KeyboardDataGenerator();
            randomGenerator.generateData(inputFile, n);

        } else {
            String testFile;
            System.out.print("Provide the name of the file: ");
            while (true) {
                testFile = scanner.next();
                if (testFile.trim().isEmpty()) {
                    System.out.println("File name cannot be empty. Please provide a valid name.");
                } else {
                    break;  // Valid input, break out of the loop
                }
            }
            DataGenerator randomGenerator = new FileDataGenerator(testFile);
            randomGenerator.generateData(inputFile, n);
        }
    }
}


