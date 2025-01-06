package com.example.adyenpiserver.service;

import java.math.BigDecimal;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.applicationinfo.ApplicationInfo;
import com.adyen.model.applicationinfo.CommonField;
import com.adyen.model.nexo.AmountsReq;
import com.adyen.model.nexo.MessageCategoryType;
import com.adyen.model.nexo.MessageClassType;
import com.adyen.model.nexo.MessageHeader;
import com.adyen.model.nexo.MessageType;
import com.adyen.model.nexo.PaymentRequest;
import com.adyen.model.nexo.PaymentResponse;
import com.adyen.model.nexo.PaymentTransaction;
import com.adyen.model.nexo.SaleData;
import com.adyen.model.nexo.SaleToPOIRequest;
import com.adyen.model.nexo.SaleToPOIResponse;
import com.adyen.model.nexo.TransactionIdentification;
import com.adyen.model.terminal.SaleToAcquirerData;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.service.TerminalCloudAPI;
import com.example.adyenpiserver.dto.PaymentRequestDTO;
import com.example.adyenpiserver.dto.PaymentResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PaymentService {

    // Hardcoded for demonstration; replace with environment variables in production.
    private static final String API_KEY = "";

    private static final Environment ENV = Environment.TEST; 
    private static final String DEVICE_NAME = "P630-452323439"; 
    private static final String SALE_ID = "Raspberry-pi-server";

    private final TerminalCloudAPI terminalCloudApi;
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService() {
        // Step 1: Initialize the Adyen client
        Client client = new Client(API_KEY, ENV);

        // Step 2: Initialize the TerminalCloudAPI
        this.terminalCloudApi = new TerminalCloudAPI(client);
    }

    /**
     * Processes a payment using the Adyen Terminal API (Cloud).
     */
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) throws Exception {
        // 1. Generate a unique service ID for each transaction
        String serviceId = "SVC-" + System.currentTimeMillis();

        // 2. Build TerminalAPIRequest (aligned with Adyen’s example)
        TerminalAPIRequest terminalAPIRequest = buildPaymentRequest(requestDTO, serviceId);

        // 3. Log the request in JSON
        ObjectMapper objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String requestJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(terminalAPIRequest);
        logger.info("TerminalAPIRequest being sent:\n{}", requestJson);

        // 4. Execute the synchronous call to Adyen
        TerminalAPIResponse terminalAPIResponse;
        try {
            logger.info("Sending payment request to terminal: {}", DEVICE_NAME);
            terminalAPIResponse = terminalCloudApi.sync(terminalAPIRequest);
            logger.info("Received response from terminal: {}", terminalAPIResponse);
        } catch (Exception e) {
            logger.error("Error occurred while processing payment", e);
            return new PaymentResponseDTO("ERROR", "Failed to call Adyen: " + e.getMessage());
        }

        // 5. Convert TerminalAPIResponse to custom PaymentResponseDTO
        return mapToPaymentResponseDTO(terminalAPIResponse);
    }

    /**
     * Builds a TerminalAPIRequest object for PAYMENT, closely following the Adyen example.
     */
    private TerminalAPIRequest buildPaymentRequest(PaymentRequestDTO requestDTO, String serviceId) throws Exception {
        // If requestDTO doesn't provide a currency or amount, use defaults:
        String currency = requestDTO.currency() == null ? "EUR" : requestDTO.currency();
        double amount   = requestDTO.amount() == 0.0 ? 10.0 : requestDTO.amount(); // default to 10 if 0.0

        // Step 4.1: Create root request
        TerminalAPIRequest terminalAPIRequest = new TerminalAPIRequest();
        SaleToPOIRequest saleToPOIRequest = new SaleToPOIRequest();

        // Step 4.2: MessageHeader
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageClass(MessageClassType.SERVICE);
        messageHeader.setMessageCategory(MessageCategoryType.PAYMENT);
        messageHeader.setMessageType(MessageType.REQUEST);
        messageHeader.setProtocolVersion("3.0");
        messageHeader.setServiceID(serviceId);
        messageHeader.setSaleID(SALE_ID);
        messageHeader.setPOIID(DEVICE_NAME);     
        saleToPOIRequest.setMessageHeader(messageHeader);

        // Step 4.3: PaymentRequest
        PaymentRequest paymentRequest = new PaymentRequest();

        // Create SaleData
        SaleData saleData = new SaleData();
        TransactionIdentification transactionIdentification = new TransactionIdentification();
        transactionIdentification.setTransactionID("TXN-" + System.currentTimeMillis()); // or "001" per example
        XMLGregorianCalendar timestamp = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        transactionIdentification.setTimeStamp(timestamp);
        saleData.setSaleTransactionID(transactionIdentification);

        // Add SaleToAcquirerData + ApplicationInfo (like the example)
        SaleToAcquirerData saleToAcquirerData = new SaleToAcquirerData();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        CommonField merchantApplication = new CommonField();
        merchantApplication.setVersion("1");
        merchantApplication.setName("Test");
        applicationInfo.setMerchantApplication(merchantApplication);
        saleToAcquirerData.setApplicationInfo(applicationInfo);
        saleData.setSaleToAcquirerData(saleToAcquirerData);

        // Optionally set other fields: merchantAccount, store, recurring, etc.
        // saleToAcquirerData.setMerchantAccount("YOUR_MERCHANT_ACCOUNT");

        // Attach the SaleToAcquirerData to the saleData
        saleData.setSaleToAcquirerData(saleToAcquirerData);

        // Step 4.4: PaymentTransaction with amounts
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency(currency);
        amountsReq.setRequestedAmount(BigDecimal.valueOf(amount));
        paymentTransaction.setAmountsReq(amountsReq);

        paymentRequest.setSaleData(saleData);
        paymentRequest.setPaymentTransaction(paymentTransaction);

        // Attach PaymentRequest
        saleToPOIRequest.setPaymentRequest(paymentRequest);

        // Step 4.5: Attach the SaleToPOIRequest to the root
        terminalAPIRequest.setSaleToPOIRequest(saleToPOIRequest);

        return terminalAPIRequest;
    }

    /**
     * Converts the TerminalAPIResponse into PaymentResponseDTO to return to the client.
     */
    private PaymentResponseDTO mapToPaymentResponseDTO(TerminalAPIResponse response) {
        SaleToPOIResponse saleToPOIResponse = response.getSaleToPOIResponse();
        if (saleToPOIResponse == null) {
            return new PaymentResponseDTO("ERROR", "No response from terminal.");
        }
        MessageHeader header = saleToPOIResponse.getMessageHeader();
        if (header == null) {
            return new PaymentResponseDTO("ERROR", "No message header in response.");
        }

        // Check if we got a PaymentResponse
        PaymentResponse paymentResponse = saleToPOIResponse.getPaymentResponse();
        if (paymentResponse != null) {
            // Optionally check paymentResponse.getPOIData(), getSaleData(), etc.
            return new PaymentResponseDTO("SUCCESS", "Payment initiated successfully!");
        }

        return new PaymentResponseDTO("UNKNOWN", "Unknown response from terminal.");
    }
}
