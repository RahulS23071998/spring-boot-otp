package com.starter.springboot.service.impl;

import com.starter.springboot.constants.AdminConstants;
import com.starter.springboot.dto.BulkUserImportResponse;
import com.starter.springboot.dto.BulkUserImportResult;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.service.IBulkUserImportService;
import com.starter.springboot.service.IUserService;
import jakarta.persistence.EntityExistsException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BulkUserImportService implements IBulkUserImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUserImportService.class);

    private final IUserService userService;

    public BulkUserImportService(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public BulkUserImportResponse importUsersFromFile(MultipartFile file) {
        List<BulkUserImportResult> results = new ArrayList<>();
        
        try {
            String filename = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
            
            if (filename.endsWith(AdminConstants.CSV_FILE_EXTENSION)) {
                results = importFromCsv(file);
            } else if (filename.endsWith(AdminConstants.XLSX_FILE_EXTENSION)) {
                results = importFromExcel(file);
            } else {
                throw new IllegalArgumentException(AdminConstants.UNSUPPORTED_FILE_FORMAT_MESSAGE);
            }

            int successCount = (int) results.stream().filter(BulkUserImportResult::isSuccess).count();
            int failureCount = results.size() - successCount;

            return new BulkUserImportResponse(results.size(), successCount, failureCount, results);

        } catch (Exception e) {
            LOGGER.error(AdminConstants.LOG_ERROR_IMPORTING_USERS, e.getMessage(), e);
            BulkUserImportResult errorResult = new BulkUserImportResult(0, "", false, 
                AdminConstants.FILE_PROCESSING_ERROR_MESSAGE + e.getMessage());
            return new BulkUserImportResponse(0, 0, 1, List.of(errorResult));
        }
    }

    private List<BulkUserImportResult> importFromCsv(MultipartFile file) throws IOException {
        List<BulkUserImportResult> results = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser =
                     CSVFormat.DEFAULT.builder()
                             .setHeader(AdminConstants.CSV_HEADERS)
                             .setIgnoreEmptyLines(true)
                             .setSkipHeaderRecord(true)
                             .get()
                             .parse(reader)) {

            int rowNumber = 1;
            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    BulkUserImportResult result = processUserRow(rowNumber, record.toMap());
                    results.add(result);
                } catch (Exception e) {
                    LOGGER.warn(AdminConstants.LOG_ERROR_PROCESSING_CSV_ROW, rowNumber, e.getMessage());
                    results.add(new BulkUserImportResult(rowNumber, "", false, e.getMessage()));
                }
            }
        }
        
        return results;
    }

    private List<BulkUserImportResult> importFromExcel(MultipartFile file) throws IOException {
        List<BulkUserImportResult> results = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int rowNumber = 0;
            for (Row row : sheet) {
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }
                
                rowNumber++;
                try {
                    Map<String, String> rowData = extractExcelRowData(row);
                    BulkUserImportResult result = processUserRow(rowNumber, rowData);
                    results.add(result);
                } catch (Exception e) {
                    LOGGER.warn(AdminConstants.LOG_ERROR_PROCESSING_EXCEL_ROW, rowNumber, e.getMessage());
                    results.add(new BulkUserImportResult(rowNumber, "", false, e.getMessage()));
                }
            }
        }
        
        return results;
    }

    private Map<String, String> extractExcelRowData(Row row) {
        return Map.of(
            "username", getCellValueAsString(row, 0),
            "password", getCellValueAsString(row, 1),
            "email", getCellValueAsString(row, 2),
            "firstName", getCellValueAsString(row, 3),
            "lastName", getCellValueAsString(row, 4),
            "otpRequired", getCellValueAsString(row, 5)
        );
    }

    private String getCellValueAsString(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return "";
        }
        return row.getCell(cellIndex).toString().trim();
    }

    private BulkUserImportResult processUserRow(int rowNumber, Map<String, String> rowData) {
        String username = rowData.getOrDefault("username", "").trim();
        String password = rowData.getOrDefault("password", "").trim();
        String email = rowData.getOrDefault("email", "").trim();
        String firstName = rowData.getOrDefault("firstName", "").trim();
        String lastName = rowData.getOrDefault("lastName", "").trim();
        String otpRequiredStr = rowData.getOrDefault("otpRequired", AdminConstants.OTP_FLAG_DEFAULT_FALSE).trim().toLowerCase();
        
        // Validation
        if (username.isEmpty()) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.USERNAME_REQUIRED_MESSAGE);
        }
        if (password.isEmpty()) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        if (email.isEmpty()) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.EMAIL_REQUIRED_MESSAGE);
        }
        if (firstName.isEmpty()) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.FIRST_NAME_REQUIRED_MESSAGE);
        }
        if (lastName.isEmpty()) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.LAST_NAME_REQUIRED_MESSAGE);
        }
        
        boolean otpRequired = AdminConstants.OTP_FLAG_TRUE.equals(otpRequiredStr) || 
                              AdminConstants.OTP_FLAG_YES.equals(otpRequiredStr) || 
                              AdminConstants.OTP_FLAG_ONE.equals(otpRequiredStr);
        
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setIsOtpRequired(otpRequired);
            user.setStatus(UserStatus.ACTIVE);
            user.setEnabled(true);
            
            User createdUser = userService.createUser(user);
            return new BulkUserImportResult(rowNumber, username, true, AdminConstants.USER_CREATED_SUCCESS_MESSAGE, createdUser.getId());
            
        } catch (EntityExistsException e) {
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.USER_ALREADY_EXISTS_ADMIN_MESSAGE);
        } catch (Exception e) {
            LOGGER.error(AdminConstants.LOG_ERROR_CREATING_USER, username, e.getMessage());
            return new BulkUserImportResult(rowNumber, username, false, AdminConstants.USER_CREATION_FAILED_MESSAGE + e.getMessage());
        }
    }
}
