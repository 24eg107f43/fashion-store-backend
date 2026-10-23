package com.fashionstore.controller;

import com.fashionstore.model.Customer;
import com.fashionstore.model.Product;
import com.fashionstore.model.StoreOrder;
import com.fashionstore.repository.CustomerRepository;
import com.fashionstore.repository.ProductRepository;
import com.fashionstore.repository.StoreOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class StoreController {
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    public StoreController(ProductRepository products, CustomerRepository customers,
                           StoreOrderRepository orders, PasswordEncoder passwordEncoder) {
        this.products=products; this.customers=customers; this.orders=orders; this.passwordEncoder=passwordEncoder;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("products", products.findAll());
        model.addAttribute("customer", currentCustomer(session));
        return "index";
    }

    @GetMapping("/signup")
    public String signup() { return "signup"; }

    @PostMapping("/signup")
    public String signupPost(@RequestParam String fullName, @RequestParam String email,
                             @RequestParam String password, RedirectAttributes ra, HttpSession session) {
        String cleanEmail=email.trim().toLowerCase(Locale.ROOT);
        if (fullName.trim().isEmpty() || cleanEmail.isEmpty() || password.length()<6) {
            ra.addFlashAttribute("error", "Enter your name/email and a password with at least 6 characters.");
            return "redirect:/signup";
        }
        if (customers.existsByEmailIgnoreCase(cleanEmail)) {
            ra.addFlashAttribute("error", "That email is already registered. Please log in.");
            return "redirect:/signup";
        }
        Customer customer=customers.save(new Customer(fullName.trim(), cleanEmail, passwordEncoder.encode(password)));
        session.setAttribute("customerId", customer.getId());
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @PostMapping("/login")
    public String loginPost(@RequestParam String email, @RequestParam String password,
                            RedirectAttributes ra, HttpSession session) {
        Optional<Customer> found=customers.findByEmailIgnoreCase(email.trim());
        if (found.isEmpty() || !passwordEncoder.matches(password, found.get().getPasswordHash())) {
            ra.addFlashAttribute("error", "Email or password is incorrect.");
            return "redirect:/login";
        }
        session.setAttribute("customerId", found.get().getId());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) { session.invalidate(); return "redirect:/"; }

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Optional<Product> product=products.findById(id);
        if (product.isEmpty()) return "redirect:/";
        Map<Long,Integer> cart=getCart(session);
        int current=cart.getOrDefault(id,0);
        if (current >= product.get().getStock()) ra.addFlashAttribute("error", "Sorry, there is not enough stock.");
        else cart.put(id,current+1);
        session.setAttribute("cart",cart);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        Map<Long,Integer> cart=getCart(session);
        List<CartLine> lines=new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;
        for (var entry:cart.entrySet()) {
            products.findById(entry.getKey()).ifPresent(p -> lines.add(new CartLine(p, entry.getValue())));
        }
        for (CartLine line:lines) total=total.add(line.product().getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        model.addAttribute("lines",lines); model.addAttribute("total",total);
        model.addAttribute("customer",currentCustomer(session));
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId, @RequestParam int quantity, HttpSession session) {
        Map<Long,Integer> cart=getCart(session);
        if (quantity<=0) cart.remove(productId);
        else {
            Optional<Product> p=products.findById(productId);
            if (p.isPresent()) cart.put(productId,Math.min(quantity,p.get().getStock()));
        }
        session.setAttribute("cart",cart);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        getCart(session).remove(productId); return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes ra) {
        if (getCart(session).isEmpty()) { ra.addFlashAttribute("error","Your cart is empty."); return "redirect:/cart"; }
        Customer customer=currentCustomer(session);
        if (customer==null) { ra.addFlashAttribute("error","Please log in or sign up before checkout."); return "redirect:/login"; }
        model.addAttribute("customer",customer);
        model.addAttribute("lines",cartLines(session));
        model.addAttribute("total",cartTotal(session));
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String shippingAddress, HttpSession session, RedirectAttributes ra) {
        Customer customer=currentCustomer(session);
        Map<Long,Integer> cart=getCart(session);
        if (customer==null) { ra.addFlashAttribute("error","Please log in before checkout."); return "redirect:/login"; }
        if (cart.isEmpty()) return "redirect:/cart";
        if (shippingAddress.trim().isEmpty()) { ra.addFlashAttribute("error","Please enter a delivery address."); return "redirect:/checkout"; }

        StringBuilder summary=new StringBuilder();
        BigDecimal total=BigDecimal.ZERO;
        for (var entry:cart.entrySet()) {
            Optional<Product> optional=products.findById(entry.getKey());
            if (optional.isEmpty() || entry.getValue()>optional.get().getStock()) {
                ra.addFlashAttribute("error","A product is out of stock or has changed. Please review your cart.");
                return "redirect:/cart";
            }
            Product p=optional.get();
            if (summary.length()>0) summary.append("; ");
            summary.append(p.getName()).append(" x ").append(entry.getValue());
            total=total.add(p.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        summary.append(" | Delivery: ").append(shippingAddress.trim());
        for (var entry:cart.entrySet()) {
            Product p=products.findById(entry.getKey()).orElseThrow();
            p.setStock(p.getStock()-entry.getValue());
            products.save(p);
        }
        orders.save(new StoreOrder(customer.getId(),customer.getFullName(),customer.getEmail(),summary.toString(),total));
        cart.clear();
        session.setAttribute("cart",cart);
        ra.addFlashAttribute("success","Your order has been placed successfully!");
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String orderHistory(HttpSession session, Model model, RedirectAttributes ra) {
        Customer customer=currentCustomer(session);
        if (customer==null) { ra.addFlashAttribute("error","Please log in to view your order history."); return "redirect:/login"; }
        model.addAttribute("orders",orders.findByCustomerIdOrderByCreatedAtDesc(customer.getId()));
        model.addAttribute("customer",customer);
        return "orders";
    }

    private Customer currentCustomer(HttpSession session) {
        Object id=session.getAttribute("customerId");
        if (!(id instanceof Long)) return null;
        return customers.findById((Long)id).orElse(null);
    }
    @SuppressWarnings("unchecked")
    private Map<Long,Integer> getCart(HttpSession session) {
        Object existing=session.getAttribute("cart");
        if (existing instanceof Map<?,?>) return (Map<Long,Integer>)existing;
        Map<Long,Integer> cart=new LinkedHashMap<>();
        session.setAttribute("cart",cart); return cart;
    }
    private List<CartLine> cartLines(HttpSession session) {
        List<CartLine> lines=new ArrayList<>();
        for (var entry:getCart(session).entrySet())
            products.findById(entry.getKey()).ifPresent(p -> lines.add(new CartLine(p,entry.getValue())));
        return lines;
    }
    private BigDecimal cartTotal(HttpSession session) {
        BigDecimal total=BigDecimal.ZERO;
        for (CartLine line:cartLines(session))
            total=total.add(line.product().getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        return total;
    }
    public record CartLine(Product product, int quantity) {}
}
