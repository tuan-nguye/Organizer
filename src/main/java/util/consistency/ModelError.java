package util.consistency;

public enum ModelError {
    // file errors
    FILE_IN_WRONG_FOLDER,

    // folder errors
    INVALID_FOLDER_NAME,
    INVALID_FOLDER_STRUCTURE,
    FOLDER_ABOVE_THRESHOLD,
    FILES_IN_NON_LEAF
}