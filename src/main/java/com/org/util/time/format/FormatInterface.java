package com.org.util.time.format;

import com.drew.imaging.ImageProcessingException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public interface FormatInterface {
    public LocalDateTime readDate(File file) throws ImageProcessingException, IOException;
}
