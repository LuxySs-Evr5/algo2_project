package projetalgo;

public class TimeConversion {

    public static int toSeconds(String time) {
        String[] parts = time.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format, expected HH:MM:SS");
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }

    public static String fromSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        totalSeconds %= 3600; 
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
