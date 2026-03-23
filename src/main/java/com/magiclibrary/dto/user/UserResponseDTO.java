package com.magiclibrary.dto.user;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idUser;
    private String civility;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String avatar;

    private Integer idRole;
    private String roleLabel;

    private Boolean activeUser;
    private Boolean subscriptionUser;
    private Boolean depositUser;
    private Boolean emailVerified;
    private Boolean ffapMember;

    private String ffapNumber;
    private String bio;
    private String notes;

    private LocalDateTime signupDateUser;
    private LocalDate associationJoinDateUser;
    private LocalDateTime lastLoginUser;
    private LocalDateTime updatedAtUser;

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getCivility() {
        return civility;
    }

    public void setCivility(String civility) {
        this.civility = civility;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getIdRole() {
        return idRole;
    }

    public void setIdRole(Integer idRole) {
        this.idRole = idRole;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public Boolean getActiveUser() {
        return activeUser;
    }

    public void setActiveUser(Boolean activeUser) {
        this.activeUser = activeUser;
    }

    public Boolean getSubscriptionUser() {
        return subscriptionUser;
    }

    public void setSubscriptionUser(Boolean subscriptionUser) {
        this.subscriptionUser = subscriptionUser;
    }

    public Boolean getDepositUser() {
        return depositUser;
    }

    public void setDepositUser(Boolean depositUser) {
        this.depositUser = depositUser;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getFfapMember() {
        return ffapMember;
    }

    public void setFfapMember(Boolean ffapMember) {
        this.ffapMember = ffapMember;
    }

    public String getFfapNumber() {
        return ffapNumber;
    }

    public void setFfapNumber(String ffapNumber) {
        this.ffapNumber = ffapNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getSignupDateUser() {
        return signupDateUser;
    }

    public void setSignupDateUser(LocalDateTime signupDateUser) {
        this.signupDateUser = signupDateUser;
    }

    public LocalDate getAssociationJoinDateUser() {
        return associationJoinDateUser;
    }

    public void setAssociationJoinDateUser(LocalDate associationJoinDateUser) {
        this.associationJoinDateUser = associationJoinDateUser;
    }

    public LocalDateTime getLastLoginUser() {
        return lastLoginUser;
    }

    public void setLastLoginUser(LocalDateTime lastLoginUser) {
        this.lastLoginUser = lastLoginUser;
    }

    public LocalDateTime getUpdatedAtUser() {
        return updatedAtUser;
    }

    public void setUpdatedAtUser(LocalDateTime updatedAtUser) {
        this.updatedAtUser = updatedAtUser;
    }
}