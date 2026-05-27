package com.smartinventory.model;

public enum Category {
    DAIRY("Dairy Products"),
    ELECTRONICS("Electronics"),
    CLOTHING("Clothing"),
    CANNED_GOODS("Canned Goods"),
    FROZEN("Frozen Foods"),
    BEVERAGES("Beverages");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
