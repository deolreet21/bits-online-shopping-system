// Owner: Aliya | Product Management | Enum for product categories with displayName
package com.shopping.system.entity;

public enum Category {
    ELECTRONICS("Electronics"),
    ELECTRICAL("Electrical"),
    FURNITURE("Furniture"),
    COSMETICS("Cosmetics"),
    TOYS("Toys"),
    BOOKS("Books");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
