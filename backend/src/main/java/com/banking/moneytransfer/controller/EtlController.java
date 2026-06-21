package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.model.entity.TransactionLog;
import com.banking.moneytransfer.repository.TransactionLogRepository;
import com.banking.moneytransfer.service.CsvService;
import com.banking.moneytransfer.service.SnowflakeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/etl")
public class EtlController {

    @Autowired
    private TransactionLogRepository repo;

    @Autowired
    private CsvService csvService;

    @Autowired
    private SnowflakeService snowflakeService;

    @PostMapping("/upload")
    public String upload(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) throws Exception {
        log.info("Uploading csv files into the snowflake warehouse");

        List<TransactionLog> data = repo.findByDateRange(start, end);
        File csv = csvService.writeCsv(data);
        snowflakeService.uploadAndCopy(csv);

        return "Upload successful: " + data.size() + " rows";
    }
}