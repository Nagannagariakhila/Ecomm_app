package com.acc.serviceImpl;

import com.acc.entity.Address;
import com.acc.entity.Customer;
import com.acc.entity.Invoice;
import com.acc.entity.InvoiceItem;
import com.acc.entity.Order;
import com.acc.entity.OrderItem;
import com.acc.entity.Profile;
import com.acc.repository.OrderRepository;
import com.acc.service.InvoiceService;
import com.acc.exception.ResourceNotFoundException;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Invoice generateInvoice(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

        return buildInvoiceFromOrder(order);
    }

    private Invoice buildInvoiceFromOrder(Order order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + order.getOrderCode());
        invoice.setInvoiceDate(new Date());

        Customer customer = order.getCustomer();
        Profile profile = null;
        if (customer != null) {
            profile = customer.getProfile();
        }

        Address shippingAddress = order.getShippingAddress();

        if (profile != null) {
            invoice.setCustomerName(profile.getFirstName() + " " + profile.getLastName());
            invoice.setCustomerEmail(profile.getEmail());
        } else {
            invoice.setCustomerName("N/A");
            invoice.setCustomerEmail("N/A");
        }

        if (shippingAddress != null) {
            invoice.setCustomerAddress(
                shippingAddress.getStreet() + ", " +
                shippingAddress.getCity() + ", " +
                shippingAddress.getState() + " " +
                shippingAddress.getZipCode()
            );
        } else {
            invoice.setCustomerAddress("N/A");
        }

        // Corrected line to use the 'status' field from the Order entity
        invoice.setPaymentStatus(order.getStatus());

        List<InvoiceItem> invoiceItems = order.getOrderItems().stream()
                .map(this::mapToInvoiceItem)
                .collect(Collectors.toList());

        invoice.setItems(invoiceItems);

        double totalAmount = invoiceItems.stream()
                .mapToDouble(InvoiceItem::getSubtotal)
                .sum();
        invoice.setTotalAmount(totalAmount);

        return invoice;
    }

    private InvoiceItem mapToInvoiceItem(OrderItem orderItem) {
        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setProductName(orderItem.getProduct().getName());
        invoiceItem.setQuantity(orderItem.getQuantity());
        invoiceItem.setUnitPrice(orderItem.getPrice().doubleValue());

        BigDecimal price = orderItem.getPrice();
        BigDecimal finalPrice = price;
        Double discountPercent = orderItem.getProduct().getDiscountPercentage();

        if (discountPercent != null && discountPercent > 0.0) {
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discountPercent))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            finalPrice = price.subtract(discountAmount);
        }

        invoiceItem.setSubtotal(finalPrice.doubleValue() * invoiceItem.getQuantity());

        return invoiceItem;
    }

    @Override
    public byte[] generatePdfInvoice(Invoice invoice) throws DocumentException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, byteArrayOutputStream);
        document.open();

        BaseFont baseFont = BaseFont.createFont("c:\\windows\\fonts\\arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font rupeeFont = new Font(baseFont, 12, Font.NORMAL, BaseColor.BLACK);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
        Font headerFooterFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);
        Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLUE);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        // Header
        Paragraph header = new Paragraph("Your E-Commerce Store", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        // Add a line separator
        document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------"));
        document.add(new Paragraph(" "));

        Paragraph invoiceTitle = new Paragraph("Invoice", titleFont);
        invoiceTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(invoiceTitle);
        document.add(new Paragraph(" "));

        // Invoice Details Section
        Paragraph details = new Paragraph();
        details.add(new Phrase("Invoice Number: " + invoice.getInvoiceNumber() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 12)));
        details.add(new Phrase("Invoice Date: " + invoice.getInvoiceDate() + "\n\n", FontFactory.getFont(FontFactory.HELVETICA, 12)));
        document.add(details);

        // Customer Details Section
        Paragraph customerDetails = new Paragraph();
        customerDetails.add(new Phrase("Customer Name: " + invoice.getCustomerName() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 12)));
        customerDetails.add(new Phrase("Customer Email: " + invoice.getCustomerEmail() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 12)));
        customerDetails.add(new Phrase("Address: " + invoice.getCustomerAddress() + "\n", FontFactory.getFont(FontFactory.HELVETICA, 12)));
        document.add(customerDetails);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        addTableHeader(table);

        for (InvoiceItem item : invoice.getItems()) {
            double discountedAmount = item.getSubtotal() / item.getQuantity();

            table.addCell(new Phrase(item.getProductName(), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            table.addCell(new Phrase(String.valueOf(item.getQuantity()), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            table.addCell(new Phrase("₹" + String.format("%.2f", item.getUnitPrice()), rupeeFont));
            table.addCell(new Phrase("₹" + String.format("%.2f", discountedAmount), rupeeFont));
            table.addCell(new Phrase("₹" + String.format("%.2f", item.getSubtotal()), rupeeFont));
        }

        document.add(table);

        Paragraph total = new Paragraph(new Phrase("Total Amount: ", totalFont));
        total.add(new Phrase("₹" + String.format("%.2f", invoice.getTotalAmount()), rupeeFont));
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);
        document.add(new Paragraph(" "));

        // Payment Status added here, below the total amount
        Paragraph paymentStatus = new Paragraph("Payment Status: " + invoice.getPaymentStatus(), statusFont);
        paymentStatus.setAlignment(Element.ALIGN_RIGHT);
        document.add(paymentStatus);

        // Footer
        document.add(new Paragraph(" "));
        document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------"));
        Paragraph footer = new Paragraph("Thank you for your business! For any queries, please contact: support@yourecommercestore.com", headerFooterFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = {"Product", "Quantity", "Price", "Discounted Price", "Subtotal"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

	@Override
	public Invoice generateInvoiceByCustomerCode(String customerCode) {
		// TODO Auto-generated method stub
		return null;
	}}
