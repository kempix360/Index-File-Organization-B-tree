package database;

public enum ColorCode {
    RESET("\u001B[0m"),
    GREEN("\u001B[32m"),
    RED("\u001B[31m"),
    YELLOW("\u001B[33m"),
    CYAN("\u001B[36m");

    private final String code;

    ColorCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
