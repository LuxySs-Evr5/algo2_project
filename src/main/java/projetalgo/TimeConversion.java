package projetalgo;

public class TimeConversion {

    public static int toSeconds(final String time) {
        try {
            String[] parts = time.split(":");
            if (parts.length != 3) {
                return -1;
            }

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);

            return hours * 3600 + minutes * 60 + seconds;

        } catch (NumberFormatException e) {
            System.err.println("Error: " + e.getMessage());
            return -1;
        }
    }

    public static String fromSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        totalSeconds %= 3600; 
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Returns string representation of the duration in minutes and seconds.
     */
    public static String formatDuration(final int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + " min" + (remainingSeconds > 0 ? " " + remainingSeconds + " sec" : "");
        } else {
            return remainingSeconds + " sec";
        }
    }

}
