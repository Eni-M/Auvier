package com.auvier.infrastructure.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Store a file and return its URL path
     * @param file the uploaded file
     * @param subDirectory optional subdirectory (e.g., "variants", "products")
     * @return the URL path to access the file
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * Delete a file by its URL path
     * @param fileUrl the file URL to delete
     */
    void deleteFile(String fileUrl);
}
