package com.fashionstore.controller;

import com.fashionstore.model.Customer;
import com.fashionstore.model.Product;
import com.fashionstore.model.StoreOrder;
import com.fashionstore.repository.CustomerRepository;
import com.fashionstore.repository.ProductRepository;
import com.fashionstore.repository.StoreOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class StoreApiController {
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    public StoreApiController(ProductRepository products, CustomerRepository customers,
                              StoreOrderRepository orders, PasswordEncoder passwordEncoder) {
        this.products = products;
        this.customers = customers;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return products.findAll().stream().map(this::productDto).toList();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> me(HttpSession session) {
        Customer c = currentCustomer(session);
        return c == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Please log in."))
                         : ResponseEntity.ok(customerDto(c));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpSession session) {
        if (request == null || blank(request.fullName()) || blank(request.email())
                || request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest().body(error("Enter your name/email and a password with at least 6 characters."));
        }
        String name = request.fullName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (customers.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("That email is already registered. Please log in."));
        }
        Customer customer = customers.save(new Customer(name, email, passwordEncoder.encode(request.password())));
        session.setAttribute("customerId", customer.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerDto(customer));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request == null || blank(request.email()) || request.password() == null) {
            return ResponseEntity.badRequest().body(error("Enter your email and password."));
        }
        Optional<Customer> found = customers.findByEmailIgnoreCase(request.email().trim());
        if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Email or password is incorrect."));
        }
        session.setAttribute("customerId", found.get().getId());
        return ResponseEntity.ok(customerDto(found.get()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @GetMapping("/cart")
    public CartDto cart(HttpSession session) {
        return cartDto(session);
    }

    @PostMapping("/cart/items")
    public ResponseEntity<?> addToCart(@RequestBody AddItemRequest request, HttpSession session) {
        if (request == null || request.productId() == null) {
            return ResponseEntity.badRequest().body(error("productId is required."));
        }
        Optional<Product> found = products.findById(request.productId());
        if (found.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Product not found."));
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity < 1) return ResponseEntity.badRequest().body(error("Quantity must be at least 1."));
        Map<Long, Integer> cart = getCart(session);
        int current = cart.getOrDefault(request.productId(), 0);
        if (current + quantity > found.get().getStock()) {
            return ResponseEntity.badRequest().body(error("Sorry, there is not enough stock."));
        }
        cart.put(request.productId(), current + quantity);
        session.setAttribute("cart", cart);
        return ResponseEntity.ok(cartDto(session));
    }

    @PutMapping("/cart/items/{id}")
    public ResponseEntity<?> updateCart(@PathVariable Long id, @RequestBody QuantityRequest request, HttpSession session) {
        if (request == null || request.quantity() == null) {
            return ResponseEntity.badRequest().body(error("quantity is required."));
        }
        Map<Long, Integer> cart = getCart(session);
        int quantity = request.quantity();
        if (quantity <= 0) {
            cart.remove(id);
        } else {
            Optional<Product> p = products.findById(id);
            if (p.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Product not found."));
            if (quantity > p.get().getStock()) return ResponseEntity.badRequest().body(error("Quantity exceeds available stock."));
            cart.put(id, quantity);
        }
        session.setAttribute("cart", cart);
        return ResponseEntity.ok(cartDto(session));
    }

    @DeleteMapping("/cart/items/{id}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long id, HttpSession session) {
        getCart(session).remove(id);
        return ResponseEntity.ok(cartDto(session));
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request, HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Please log in before checkout."));
        if (request == null || blank(request.shippingAddress())) {
            return ResponseEntity.badRequest().body(error("Please enter a delivery address."));
        }
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) return ResponseEntity.badRequest().body(error("Your cart is empty."));

        StringBuilder summary = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        List<Product> chosen = new ArrayList<>();
        for (var entry : cart.entrySet()) {
            Product p = products.findById(entry.getKey()).orElse(null);
            if (p == null || entry.getValue() < 1 || entry.getValue() > p.getStock()) {
                return ResponseEntity.badRequest().body(error("A product is out of stock or has changed. Please review your cart."));
            }
            chosen.add(p);
            if (summary.length() > 0) summary.append("; ");
            summary.append(p.getName()).append(" x ").append(entry.getValue());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        summary.append(" | Delivery: ").append(request.shippingAddress().trim());
        for (Product p : chosen) {
            p.setStock(p.getStock() - cart.get(p.getId()));
            products.save(p);
        }
        StoreOrder order = orders.save(new StoreOrder(customer.getId(), customer.getFullName(),
                customer.getEmail(), summary.toString(), total));
        cart.clear();
        session.setAttribute("cart", cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDto(order));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orderHistory(HttpSession session) {
        Customer customer = currentCustomer(session);
        if (customer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("Please log in to view your orders."));
        return ResponseEntity.ok(orders.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::orderDto).toList());
    }

    private Customer currentCustomer(HttpSession session) {
        Object id = session.getAttribute("customerId");
        if (!(id instanceof Long)) return null;
        return customers.findById((Long) id).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object existing = session.getAttribute("cart");
        if (existing instanceof Map<?, ?>) return (Map<Long, Integer>) existing;
        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    private CartDto cartDto(HttpSession session) {
        List<CartItemDto> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entry : getCart(session).entrySet()) {
            Optional<Product> found = products.findById(entry.getKey());
            if (found.isPresent()) {
                Product p = found.get();
                BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
                lines.add(new CartItemDto(productDto(p), entry.getValue(), lineTotal));
                total = total.add(lineTotal);
            }
        }
        return new CartDto(lines, total);
    }

    private ProductDto productDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.getCategory());
    }

    private CustomerDto customerDto(Customer c) {
        return new CustomerDto(c.getId(), c.getFullName(), c.getEmail());
    }

    private OrderDto orderDto(StoreOrder o) {
        return new OrderDto(o.getId(), o.getItemsSummary(), o.getTotal(), o.getStatus(), o.getCreatedAt());
    }

    private Map<String, String> error(String message) { return Map.of("error", message); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    public record RegisterRequest(String fullName, String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record AddItemRequest(Long productId, Integer quantity) {}
    public record QuantityRequest(Integer quantity) {}
    public record CheckoutRequest(String shippingAddress) {}
    public record ProductDto(Long id, String name, String description, BigDecimal price, int stock, String category) {}
    public record CustomerDto(Long id, String fullName, String email) {}
    public record CartItemDto(ProductDto product, int quantity, BigDecimal lineTotal) {}
    public record CartDto(List<CartItemDto> items, BigDecimal total) {}
    public record OrderDto(Long id, String itemsSummary, BigDecimal total, String status, java.time.LocalDateTime createdAt) {}
}
