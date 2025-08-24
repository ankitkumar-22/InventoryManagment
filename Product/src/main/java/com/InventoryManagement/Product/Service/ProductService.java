package com.InventoryManagement.Product.Service;

import com.InventoryManagement.Product.DTO.ProductRequest;
import com.InventoryManagement.Product.Entity.ProductEntity;
import com.InventoryManagement.Product.Repository.ProductRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    public void createProduct(@RequestBody ProductRequest productRequest){
        ProductEntity product =ProductEntity.builder()
                .productId(productRequest.id())
                .productName(productRequest.name())
                .productDescription(productRequest.description())
                .build();
        productRepository.save(product);
    }
    public void deleteProduct(@RequestParam String id){
        if(!productRepository.existsById(id)){
            throw new RuntimeException("No Product with such ID exists");
        }
        productRepository.deleteById(id);
    }
}
