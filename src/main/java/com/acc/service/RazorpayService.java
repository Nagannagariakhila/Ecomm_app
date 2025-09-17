
package com.acc.service;

import java.util.UUID;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

//RazorpayService.java
import com.razorpay.*;

@Service
public class RazorpayService {

 private static final String KEY_ID = "rzp_test_RFNLCCDDkhQcUC"; 
 private static final String KEY_SECRET = "gbYPLOpK4KRQUZoNGHBa5Thc"; 
 public String createOrder(int amountInRupees) throws RazorpayException {
     RazorpayClient razorpayClient = new RazorpayClient(KEY_ID, KEY_SECRET);

     JSONObject orderRequest = new JSONObject();
     orderRequest.put("amount", amountInRupees * 100);
     orderRequest.put("currency", "INR");
     orderRequest.put("receipt", "txn_" + UUID.randomUUID());
     orderRequest.put("payment_capture", 1);

     Order order = razorpayClient.orders.create(orderRequest);
     return order.toString(); 
 }
}
