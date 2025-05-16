package projetalgo;

import org.jline.reader.LineReader;

public class InteractiveConsole {
    private static LineReader reader;

    public static void init(LineReader r) {
        reader = r;
    }

    public static String ask(String message) {
        return reader.readLine(message).strip();
    }
}
