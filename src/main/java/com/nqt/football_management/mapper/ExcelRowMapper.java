package com.nqt.football_management.mapper;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

@FunctionalInterface
public interface ExcelRowMapper<T> {
    T map(Row row, DataFormatter formatter, Map<String, Integer> headerMap);
}
