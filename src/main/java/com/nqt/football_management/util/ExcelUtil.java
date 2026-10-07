package com.nqt.football_management.util;


import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.mapper.ExcelRowMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class ExcelUtil {
    private static final Set<String> EXCEL_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/octet-stream"
    );

    private ExcelUtil() {
    }

    public static String getCellValue(
            Row row,
            DataFormatter formatter,
            Map<String, Integer> headerMap,
            String headerName
    ) {
        Integer columnIndex = headerMap.get(normalizeHeader(headerName));
        if (columnIndex == null) {
            return "";
        }

        return getCellValue(row.getCell(columnIndex), formatter);
    }

    public static String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    public static String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("Excel file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw invalidFile("Excel filename is required");
        }

        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        if (!normalizedFilename.endsWith(".xlsx") && !normalizedFilename.endsWith(".xls")) {
            throw invalidFile("Only .xlsx and .xls files are supported");
        }

        String contentType = file.getContentType();
        if (contentType != null && !EXCEL_CONTENT_TYPES.contains(contentType)) {
            throw invalidFile("Invalid Excel content type");
        }
    }

    public static <T> List<T> readExcel(MultipartFile file, ExcelRowMapper<T> mapper) {
        return readExcel(file, List.of(), mapper);
    }

    public static <T> List<T> readExcel(
            MultipartFile file,
            Collection<String> requiredHeaders,
            ExcelRowMapper<T> mapper
    ) {
        return readExcel(file, 1, requiredHeaders, mapper);
    }

    public static <T> List<T> readExcel(
            MultipartFile file,
            int sheetNumber,
            Collection<String> requiredHeaders,
            ExcelRowMapper<T> mapper
    ) {
        validateExcelFile(file);
        if (sheetNumber < 1) {
            throw invalidFile("sheetNumber must be at least 1");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw invalidFile("Excel file does not contain a sheet");
            }

            if (sheetNumber > workbook.getNumberOfSheets()) {
                throw invalidFile("sheetNumber exceeds workbook sheet count: " + workbook.getNumberOfSheets());
            }
            Sheet sheet = workbook.getSheetAt(sheetNumber - 1);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT) {
                @Override
                public String formatCellValue(Cell cell) {
                    return super.formatCellValue(cell, evaluator);
                }
            };
            Map<String, Integer> headerMap = getHeaderIndexMap(sheet, formatter);
            validateRequiredHeaders(headerMap, requiredHeaders);

            List<T> result = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                T item = mapper.map(row, formatter, headerMap);
                if (item != null) {
                    result.add(item);
                }
            }
            return result;
        } catch (GlobalException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalidFile("Cannot read Excel file");
        }
    }

    private static Map<String, Integer> getHeaderIndexMap(Sheet sheet, DataFormatter formatter) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw invalidFile("Excel header row is required");
        }

        Map<String, Integer> headerMap = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String headerName = getCellValue(cell, formatter);
            String normalizedHeader = normalizeHeader(headerName);
            if (normalizedHeader.isBlank()) {
                continue;
            }
            if (headerMap.putIfAbsent(normalizedHeader, cell.getColumnIndex()) != null) {
                throw invalidFile("Duplicate Excel header: " + headerName);
            }
        }
        return headerMap;
    }

    private static void validateRequiredHeaders(
            Map<String, Integer> headerMap,
            Collection<String> requiredHeaders
    ) {
        List<String> missingHeaders = requiredHeaders.stream()
                .filter(header -> !headerMap.containsKey(normalizeHeader(header)))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw invalidFile("Missing Excel headers: " + String.join(", ", missingHeaders));
        }
    }

    private static String normalizeHeader(String header) {
        return header == null
                ? ""
                : header.replace("\uFEFF", "")
                        .trim()
                        .replaceAll("[\\s_-]+", "")
                        .toLowerCase(Locale.ROOT);
    }

    private static GlobalException invalidFile(String message) {
        return new GlobalException(ErrorCode.DATA_INVALID, message);
    }
}
