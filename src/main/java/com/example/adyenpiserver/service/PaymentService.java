package com.example.adyenpiserver.service;

import java.math.BigDecimal;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Service;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.nexo.AmountsReq;
import com.adyen.model.nexo.MessageCategoryType;
import com.adyen.model.nexo.MessageClassType;
import com.adyen.model.nexo.MessageHeader;
import com.adyen.model.nexo.MessageType;
import com.adyen.model.nexo.PaymentTransaction;
import com.adyen.model.nexo.SaleData;
import com.adyen.model.nexo.SaleToPOIRequest;
import com.adyen.model.nexo.SaleToPOIResponse;
import com.adyen.model.nexo.TransactionIdentification;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.service.TerminalCloudAPI;
import com.example.adyenpiserver.dto.PaymentRequestDTO;
import com.example.adyenpiserver.dto.PaymentResponseDTO;

@Service
public class PaymentService {

    // For simplicity, hardcode your API key, environment, device name, etc.
    // In production, store these in environment variables or application.properties.
    private static final String API_KEY = "YOUR_API_KEY";
    private static final Environment ENV = Environment.TEST; // or LIVE
    private static final String DEVICE_NAME = "V400m-123456789"; // Terminal name
    private static final String SALE_ID = "POS-SystemID12345";
    private static final String SERVICE_ID = "123456789"; // unique ID per transaction, or generate dynamically

    private final TerminalCloudAPI terminalCloudApi;

    public PaymentService() {
        // Step 2: Initialize the client
        Client client = new Client(API_KEY, ENV);
        // Step 3: Initialize the TerminalCloudAPI
        this.terminalCloudApi = new TerminalCloudAPI(client);
    }

    /**
     * Initiates a PaymentRequest on the Adyen Terminal and returns the response.
     */
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) throws Exception {
        // Build the TerminalAPIRequest
        TerminalAPIRequest terminalAPIRequest = buildPaymentRequest(requestDTO);

        // Step 5: Make the request (synchronous)
        TerminalAPIResponse terminalAPIResponse = terminalCloudApi.sync(terminalAPIRequest);

        // Convert TerminalAPIResponse to your custom response
        return mapToPaymentResponseDTO(terminalAPIResponse);
    }

    /**
     * Builds a TerminalAPIRequest for a Payment.
     */
    private TerminalAPIRequest buildPaymentRequest(PaymentRequestDTO requestDTO) throws Exception {
        // Create root request
        TerminalAPIRequest terminalAPIRequest = new TerminalAPIRequest();

        // Prepare the SaleToPOIRequest
        SaleToPOIRequest saleToPOIRequest = new SaleToPOIRequest();
        saleToPOIRequest.setMessageHeader(buildMessageHeader(MessageCategoryType.PAYMENT, SERVICE_ID));

        // Build PaymentRequest
        com.adyen.model.nexo.PaymentRequest paymentRequest = new com.adyen.model.nexo.PaymentRequest();

        // SaleData
        SaleData saleData = new SaleData();
        TransactionIdentification transactionIdentification = new TransactionIdentification();
        transactionIdentification.setTransactionID("TXN-" + System.currentTimeMillis()); // unique transaction ID
        XMLGregorianCalendar timestamp = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        transactionIdentification.setTimeStamp(timestamp);
        saleData.setSaleTransactionID(transactionIdentification);

        // Optionally set SaleToAcquirerData, ApplicationInfo, etc.
        // ...

        paymentRequest.setSaleData(saleData);

        // PaymentTransaction
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency(requestDTO.currency()); 
        amountsReq.setRequestedAmount(BigDecimal.valueOf(requestDTO.amount())); 
        paymentTransaction.setAmountsReq(amountsReq);

        paymentRequest.setPaymentTransaction(paymentTransaction);
        saleToPOIRequest.setPaymentRequest(paymentRequest);

        // Attach everything
        terminalAPIRequest.setSaleToPOIRequest(saleToPOIRequest);
        return terminalAPIRequest;
    }

    /**
     * Builds a basic MessageHeader for Payment/Abort/Status requests.
     */
    private MessageHeader buildMessageHeader(MessageCategoryType messageCategory, String serviceID) {
        MessageHeader header = new MessageHeader();
        header.setMessageClass(MessageClassType.SERVICE);
        header.setMessageCategory(messageCategory);
        header.setMessageType(MessageType.REQUEST);
        header.setProtocolVersion("3.0");
        header.setServiceID(serviceID);
        header.setSaleID(SALE_ID);
        header.setPOIID(DEVICE_NAME);
        return header;
    }

    /**
     * Convert the TerminalAPIResponse into your custom PaymentResponseDTO.
     */
    private PaymentResponseDTO mapToPaymentResponseDTO(TerminalAPIResponse response) {
        // Extract relevant info from SaleToPOIResponse
        SaleToPOIResponse saleToPOIResponse = response.getSaleToPOIResponse();
        if (saleToPOIResponse == null) {
            return new PaymentResponseDTO("ERROR", "No response from terminal.");
        }

        MessageHeader header = saleToPOIResponse.getMessageHeader();
        if (header == null) {
            return new PaymentResponseDTO("ERROR", "No message header in response.");
        }

        // Check PaymentResponse
        com.adyen.model.nexo.PaymentResponse paymentResponse = saleToPOIResponse.getPaymentResponse();
        if (paymentResponse != null) {
            // Possibly check Result, pspReference, etc.
            return new PaymentResponseDTO("SUCCESS", "Payment initiated successfully!");
        }

        return new PaymentResponseDTO("UNKNOWN", "Unknown response from terminal.");
    }
}
