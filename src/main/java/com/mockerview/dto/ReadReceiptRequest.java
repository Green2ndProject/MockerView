package com.mockerview.dto;

public class ReadReceiptRequest {
    
    private String partnerUsername;

    public ReadReceiptRequest() {}

    public String getPartnerUsername() {
        return partnerUsername;
    }
    
    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }
}
