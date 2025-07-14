package com.acc.controller;
import com.acc.entity.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @GetMapping("/categories")
    public List<Category> getAllCategories() {
        return Arrays.asList(Category.values());
    }
}
