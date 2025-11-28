package com.acc.service;

import com.acc.entity.Invoice;
import com.itextpdf.text.DocumentException;
import java.io.IOException;

public interface InvoiceService {
    Invoice generateInvoice(String orderCode);
    byte[] generatePdfInvoice(Invoice invoice) throws DocumentException, IOException;
	Invoice generateInvoiceByCustomerCode(String customerCode);
}