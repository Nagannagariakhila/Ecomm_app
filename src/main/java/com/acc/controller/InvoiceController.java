package com.acc.controller;

import com.acc.entity.Invoice;
import com.acc.service.InvoiceService;
import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/generate/{orderCode}")
    public ResponseEntity<Invoice> generateInvoiceForOrder(@PathVariable String orderCode) {
        Invoice invoice = invoiceService.generateInvoice(orderCode);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/generate/pdf/{orderCode}")
    public ResponseEntity<byte[]> generatePdfInvoiceForOrder(@PathVariable String orderCode) throws DocumentException, IOException {
        Invoice invoice = invoiceService.generateInvoice(orderCode);
        byte[] pdfBytes = invoiceService.generatePdfInvoice(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "invoice_" + invoice.getInvoiceNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
    
    @GetMapping("/generate/pdf/customer/{customerCode}")
    public ResponseEntity<byte[]> generatePdfInvoiceForCustomer(@PathVariable String customerCode) throws DocumentException, IOException {
        // Assume you have a method in InvoiceService to generate an invoice based on customer code
        Invoice invoice = invoiceService.generateInvoiceByCustomerCode(customerCode);
        byte[] pdfBytes = invoiceService.generatePdfInvoice(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "invoice_" + invoice.getInvoiceNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
    
}