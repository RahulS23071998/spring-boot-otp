package com.starter.springboot.service;

import com.starter.springboot.dto.BulkUserImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IBulkUserImportService {
    
    BulkUserImportResponse importUsersFromFile(MultipartFile file);
}
