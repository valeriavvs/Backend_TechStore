package com.upc.backend_techstore.excepciones;

public class InsufficientStockException extends RuntimeException {
    private String productName;
    private Integer requestedQuantity;
    private Integer availableQuantity;

    public InsufficientStockException(String productName, Integer requestedQuantity, Integer availableQuantity) {
        super("Stock insuficiente para el producto: " + productName + 
                ". Stock disponible: " + availableQuantity + ", Solicitado: " + requestedQuantity);
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public InsufficientStockException(String message) {
        super(message);
    }

    public String getProductName() {
        return productName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}
