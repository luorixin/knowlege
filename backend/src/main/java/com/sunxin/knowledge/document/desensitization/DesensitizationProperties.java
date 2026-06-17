package com.sunxin.knowledge.document.desensitization;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.desensitization")
public class DesensitizationProperties {

    private boolean enabled = true;
    private Rule phone = new Rule(true);
    private Rule email = new Rule(true);
    private Rule idCard = new Rule(true);
    private Rule amount = new Rule(true);
    private Rule customerContact = new Rule(true);
    private Rule customerName = new Rule(true);
    private List<String> customerNames = new ArrayList<>(List.of("平安银行"));
    private String customerNameReplacement = "某金融客户";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Rule getPhone() {
        return phone;
    }

    public void setPhone(Rule phone) {
        this.phone = phone;
    }

    public Rule getEmail() {
        return email;
    }

    public void setEmail(Rule email) {
        this.email = email;
    }

    public Rule getIdCard() {
        return idCard;
    }

    public void setIdCard(Rule idCard) {
        this.idCard = idCard;
    }

    public Rule getAmount() {
        return amount;
    }

    public void setAmount(Rule amount) {
        this.amount = amount;
    }

    public Rule getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(Rule customerContact) {
        this.customerContact = customerContact;
    }

    public Rule getCustomerName() {
        return customerName;
    }

    public void setCustomerName(Rule customerName) {
        this.customerName = customerName;
    }

    public List<String> getCustomerNames() {
        return customerNames;
    }

    public void setCustomerNames(List<String> customerNames) {
        this.customerNames = customerNames == null ? new ArrayList<>() : customerNames;
    }

    public String getCustomerNameReplacement() {
        return customerNameReplacement;
    }

    public void setCustomerNameReplacement(String customerNameReplacement) {
        this.customerNameReplacement = customerNameReplacement;
    }

    public static class Rule {
        private boolean enabled = true;

        public Rule() {
        }

        public Rule(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
