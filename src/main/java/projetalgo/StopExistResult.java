package projetalgo;

public enum StopExistResult {
        EXISTS, // indicates that the stop exists and returns the name
        NOT_EXISTS, // indicates that the stop does not exist
        SEVERAL_MATCHES // indicates that several stops with the same name have been found
}