package memory;

import java.io.*;
import java.util.Scanner;

public class DiskFile {
    private final String filename;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    private Scanner scanner;
    private int runCount = 0;

    public DiskFile(String filename) throws IOException {
        this.filename = filename;
        this.fileInputStream = null;
        this.fileOutputStream = null;
        this.scanner = null;
    }

    public String getFilename() {
        return filename;
    }

    public FileInputStream getFileInputStream() {
        return fileInputStream;
    }

    public void resetFileInputStream() throws IOException {
        if (this.fileInputStream != null) { this.fileInputStream.close(); }
        this.fileInputStream = new FileInputStream(filename);
    }

    public FileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void resetFileOutputStream() throws IOException {
        if (this.fileOutputStream != null) { this.fileOutputStream.close(); }
        this.fileOutputStream = new FileOutputStream(filename);
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void resetScanner() {
        if (this.scanner != null) this.scanner.close();
        this.scanner = new Scanner(fileInputStream);
    }

    public int getRunCount() {
        return this.runCount;
    }

    public void incrementRunCount() {
        this.runCount++;
    }

    public void decrementRunCount() {
        this.runCount--;
    }

}
