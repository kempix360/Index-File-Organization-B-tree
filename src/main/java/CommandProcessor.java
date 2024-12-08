import database.DatabaseManager;
import memory.Record;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class CommandProcessor {
    private final DatabaseManager manager;

    public CommandProcessor(DatabaseManager manager) {
        this.manager = manager;
    }

    public void run() {
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

    public void handleSearchCommand(String command) {
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

    public void handleInsertCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 5) {
            System.out.println("Invalid command format. Use: insert r1 r2 r3 k");
            return;
        }

        try {
            int r1 = Integer.parseInt(parts[1]);
            int r2 = Integer.parseInt(parts[2]);
            int r3 = Integer.parseInt(parts[3]);
            int key = Integer.parseInt(parts[4]);
            manager.insert(new Record(r1, r2, r3, key));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        }
    }

    public void handleUpdateCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 5) {
            System.out.println("Invalid command format. Use: update k r1 r2 r3");
            return;
        }

        try {
            int key = Integer.parseInt(parts[1]);
            int r1 = Integer.parseInt(parts[2]);
            int r2 = Integer.parseInt(parts[3]);
            int r3 = Integer.parseInt(parts[4]);
            manager.updateRecord(key, new Record(r1, r2, r3, key));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        }
    }

    public void handleDeleteCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Invalid command format. Use: delete k");
            return;
        }
        try {
            int key = Integer.parseInt(parts[1]);
            manager.delete(key);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        }
    }

    public void handlePrintDataCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Invalid command format. Use: print num");
            return;
        }
        try {
            int blockNumber = Integer.parseInt(parts[1]);
            manager.printDataBlock(blockNumber);
        } catch (NumberFormatException e) {
            System.out.println("Invalid block number.");
        }
    }

    public void processCommandsFromFile(String filename) {
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

    public void handleCommand(DatabaseManager manager, String command) {
        if (command.startsWith("search")) {
            handleSearchCommand(command);
        }
        else if (command.startsWith("insert")) {
            handleInsertCommand(command);
        }
        else if (command.startsWith("update")) {
            handleUpdateCommand(command);
        }
        else if (command.startsWith("delete")) {
            handleDeleteCommand(command);
        }
        else if (command.startsWith("printData")) {
            handlePrintDataCommand(command);
        }
        else if (command.startsWith("printBTree")) {
            manager.printBTree();
        }
        else if (command.startsWith("fileCommands")) {
            String filePath = command.substring("fileCommands ".length()).trim();
            processCommandsFromFile(filePath);
        }
        else if (command.equalsIgnoreCase("help")) {
            displayHelp();
        }
        else {
            System.out.println("Unknown command. Type 'help' for a list of available commands.");
        }
    }

    public void displayHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  search k             - Search for a record of a key k.");
        System.out.println("  insert r1 r2 r3 k    - Insert a record with values r1, r2, r3 and key k.");
        System.out.println("  update k r1 r2 r3    - Update the record of a key k to values r1, r2 and r3.");
        System.out.println("  delete k             - Delete record of a key k.");
        System.out.println("  printData num        - Print block of data with number num.");
        System.out.println("  printBTree           - Print the B-Tree structure.");
        System.out.println("  fileCommands path    - Execute commands from a file of a given path.");
        System.out.println("  help                 - Display this help message.");
        System.out.println("  exit                 - Exit the program.");
    }
}
