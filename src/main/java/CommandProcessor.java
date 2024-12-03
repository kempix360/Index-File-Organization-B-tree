import memory.Record;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class CommandProcessor {
    public void run(DatabaseManager manager) {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("> ");
            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("exit")) {
                System.out.println("Exiting program. Goodbye!");
                break;
            } else {
                handleCommand(manager, command);
            }
        }
    }

    public static void handleSearchCommand(DatabaseManager manager, String command) {
        try {
            String[] parts = command.split("\\s+");
            if (parts.length != 2) {
                System.out.println("Invalid command format. Use: search k");
                return;
            }

            int key = Integer.parseInt(parts[1]);
            manager.search(key);

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Use: search k");
        } catch (Exception e) {
            System.out.println("Error during search: " + e.getMessage());
        }
    }

    public static void processCommandsFromFile(DatabaseManager manager, String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String command;
            while ((command = reader.readLine()) != null) {
                command = command.trim();
                if (!command.isEmpty()) {
                    handleCommand(manager, command);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading commands from file: " + e.getMessage());
        }
    }

    public static void handleCommand(DatabaseManager manager, String command) {
        if (command.startsWith("search ")) {
            handleSearchCommand(manager, command);
        }
        else if (command.startsWith("file_commands ")) {
            String filePath = command.substring("file_commands ".length()).trim();
            processCommandsFromFile(manager, filePath);
        }
        else if (command.equalsIgnoreCase("help")) {
            displayHelp();
        }
        else {
            System.out.println("Unknown command. Type 'help' for a list of available commands.");
        }
    }

    public static void displayHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  search k             - Search for a record with key k.");
        System.out.println("  file_commands path   - Execute commands from a file of a given path.");
        System.out.println("  help                 - Display this help message.");
        System.out.println("  exit                 - Exit the program.");
    }
}
