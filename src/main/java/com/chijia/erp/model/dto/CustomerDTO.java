package com.chijia.erp.model.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private Long id;
    private String customerCode;
    private String shortName;
    private String fullName;
    private String contactPerson;
    private String phone;
    private String mobile;
    private String fax;
    private String taxId;
    private String email;
    private String address;
    private String deliveryAddress;
    private Integer checkoutDay;
    private String invoiceType;
    private String invoiceTitle;
    private String remark;
    private boolean status;
}