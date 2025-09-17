package com.acc.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class PaymentQrCodeService {

    public byte[] generateQrCode(String data, int width, int height) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    public byte[] generateUpiQrCode(String receiverName, String upiId, double amount, String orderId, int width, int height) throws WriterException, IOException {
        String upiUrl = String.format(
            "upi://pay?pa=%s&pn=%s&tr=%s&am=%s&cu=INR",
            upiId,
            URLEncoder.encode(receiverName, StandardCharsets.UTF_8.toString()),
            URLEncoder.encode(orderId, StandardCharsets.UTF_8.toString()),
            String.valueOf(amount)
        );
        return generateQrCode(upiUrl, width, height);
    }
}