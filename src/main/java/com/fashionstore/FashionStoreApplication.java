package com.fashionstore;

import com.fashionstore.model.Product;
import com.fashionstore.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;

@SpringBootApplication
public class FashionStoreApplication {
    public static void main(String[] args) { SpringApplication.run(FashionStoreApplication.class, args); }

    @Bean
    CommandLineRunner seedProducts(ProductRepository products) {
        return args -> {
            if (products.count() == 0) {
                products.save(new Product("Classic T-Shirt", "Soft everyday cotton T-shirt.", new BigDecimal("499.00"), 20, "Men"));
                products.save(new Product("Casual Shirt", "Comfortable shirt for everyday style.", new BigDecimal("899.00"), 15, "Men"));
                products.save(new Product("Denim Jeans", "Classic blue denim jeans.", new BigDecimal("1299.00"), 12, "Women"));
                products.save(new Product("Summer Dress", "Light and easy summer dress.", new BigDecimal("1499.00"), 10, "Women"));
                products.save(new Product("Kids Hoodie", "Warm hoodie for kids.", new BigDecimal("799.00"), 18, "Kids"));
                products.save(new Product("Everyday Sneakers", "Simple casual sneakers.", new BigDecimal("1799.00"), 8, "Unisex"));
            }
        };
    }
}
