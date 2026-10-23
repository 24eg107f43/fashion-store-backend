package com.fashionstore.repository;
import com.fashionstore.model.StoreOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {
    List<StoreOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
