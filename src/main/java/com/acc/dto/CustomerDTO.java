package com.acc.dto;

public class CustomerDTO {

    private String customerCode;
    private UserDTO userDetails;
    private ProfileDTO profileDetails;

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public UserDTO getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDTO userDetails) {
        this.userDetails = userDetails;
    }

    public ProfileDTO getProfileDetails() {
        return profileDetails;
    }

    public void setProfileDetails(ProfileDTO profileDetails) {
        this.profileDetails = profileDetails;
    }
}
