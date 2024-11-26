package data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class KeyboardDataGenerator implements DataGenerator {
    @Override
    public void generateData(String filename, int n) throws IOException {
        Scanner scanner = new Scanner(System.in);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.print("Enter 3 dimensions separated by spaces, each set in separate lines:");
            for (int i = 0; i < n; i++) {
                int number = scanner.nextInt();
                writer.write(Integer.toString(number));
                number = scanner.nextInt();
                writer.write(" " + number);
                number = scanner.nextInt();
                writer.write(" " + number);
                writer.newLine();
            }
        }
    }
}
