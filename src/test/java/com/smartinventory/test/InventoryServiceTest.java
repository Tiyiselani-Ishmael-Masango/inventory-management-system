package com.smartinventory.test;

import com.smartinventory.config.Constants;
import com.smartinventory.model.Category;
import com.smartinventory.model.Product;
import com.smartinventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.smartinventory.dto.InventoryValueResponse;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product testProduct;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Milk");
        testProduct.setCategory(Category.DAIRY);
        testProduct.setCostPrice(new BigDecimal("2.50"));
        testProduct.setRetailPrice(new BigDecimal("3.50"));
        testProduct.setExpiryDate(LocalDate.now().plusDays(10));
        testProduct.setMaxCapacity(100);
        testProduct.setCurrentQuantity(50);

        testProducts = new ArrayList<>();
        testProducts.add(testProduct);
    }

    @Test
    void testTotalInventoryValuationAccuracyWithBigDecimal() {
        // Given: Multiple products with specific quantities and prices
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Milk");
        product1.setCategory(Category.DAIRY);
        product1.setCostPrice(new BigDecimal("2.50"));
        product1.setCurrentQuantity(10);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Butter");
        product2.setCategory(Category.DAIRY);
        product2.setCostPrice(new BigDecimal("5.75"));
        product2.setCurrentQuantity(20);

        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        when(productRepository.findAll()).thenReturn(products);

        // When: Calculate inventory value
        InventoryValueResponse response = inventoryService.calculateInventoryValue();

        // Then: Verify exact BigDecimal calculations (no rounding errors)
        // Milk: 10 * 2.50 = 25.00
        // Butter: 20 * 5.75 = 115.00
        // Total: 140.00
        assertEquals("140.00", response.totalValue());
        assertEquals("140.00", response.byCategory().get("Dairy Products"));
    }

    @Test
    void testOutboundRejectionWhenInsufficientStock() {
        // Given: A product with limited stock
        testProduct.setCurrentQuantity(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When: Try to remove more units than available
        com.smartinventory.dto.StockMovementRequest request = 
            new com.smartinventory.dto.StockMovementRequest(1L, 10);

        // Then: Should throw InsufficientStockException
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.recordOutboundMovement(request);
        });
    }

    @Test
    void testOutboundAllowedWhenSufficientStock() {
        // Given: A product with adequate stock
        testProduct.setCurrentQuantity(50);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Remove units within available quantity
        com.smartinventory.dto.StockMovementRequest request = 
            new com.smartinventory.dto.StockMovementRequest(1L, 20);

        // Then: Should succeed
        assertDoesNotThrow(() -> {
            inventoryService.recordOutboundMovement(request);
        });

        assertEquals(30, testProduct.getCurrentQuantity());
    }
}
