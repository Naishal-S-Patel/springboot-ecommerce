package com.ecommerece.project.service;

import com.ecommerece.project.payload.CategoryDTO;
import com.ecommerece.project.payload.CategoryResponse;

public interface CategoryService {
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize,String sortBy,String sortOrder);
    public CategoryDTO addCategory(CategoryDTO categoryDTO);

   CategoryDTO deleteCategory(Long categoryId);

    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);
}
