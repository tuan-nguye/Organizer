package com.org.util.consistency;

/**
 * Errors or inconsistencies that folders in file graph models can show.
 */
public enum ModelError {
    // the folder name is invalid and doesn't follow the standard
    INVALID_FOLDER_NAME,
    // the folder structure is invalid
    INVALID_FOLDER_STRUCTURE,
    // the folder contains more files than allowed by the threshold
    FOLDER_ABOVE_THRESHOLD,
    // files should only be saved in leaf folders
    FILES_IN_NON_LEAF,
    // the files in this folder have deviating dates
    FOLDER_CONTAINS_INCONSISTENT_DATES,
    // the folder doesn't contain enough files to have subfolders and can therefore be reduced
    CAN_BE_REDUCED,
    // the error folder that every repository must have, is missing
    ERROR_FOLDER_MISSING
}
