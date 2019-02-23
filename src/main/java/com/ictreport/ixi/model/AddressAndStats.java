package com.ictreport.ixi.model;

public class AddressAndStats {
    private Address address;
    private Integer allTx;
    private Integer newTx;
    private Integer ignoredTx;
    private Integer invalidTx;
    private Integer requestedTx;

    public AddressAndStats(Address address) {
        this.address = address;
        this.allTx = null;
        this.newTx = null;
        this.ignoredTx = null;
        this.invalidTx = null;
        this.requestedTx = null;
    }

    public AddressAndStats(Address address, int allTx, int newTx, int ignoredTx, int invalidTx, int requestedTx) {
        this.address = address;
        this.allTx = allTx;
        this.newTx = newTx;
        this.ignoredTx = ignoredTx;
        this.invalidTx = invalidTx;
        this.requestedTx = requestedTx;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Integer getAllTx() {
        return allTx;
    }

    public void setAllTx(Integer allTx) {
        this.allTx = allTx;
    }

    public Integer getNewTx() {
        return newTx;
    }

    public void setNewTx(Integer newTx) {
        this.newTx = newTx;
    }

    public Integer getIgnoredTx() {
        return ignoredTx;
    }

    public void setIgnoredTx(Integer ignoredTx) {
        this.ignoredTx = ignoredTx;
    }

    public Integer getInvalidTx() {
        return invalidTx;
    }

    public void setInvalidTx(Integer invalidTx) {
        this.invalidTx = invalidTx;
    }

    public Integer getRequestedTx() {
        return requestedTx;
    }

    public void setRequestedTx(Integer requestedTx) {
        this.requestedTx = requestedTx;
    }
}