package com.ecommerece.project.service;

import com.ecommerece.project.exceptions.APIException;
import com.ecommerece.project.exceptions.ResourceNotFoundException;
import com.ecommerece.project.model.Category;
import com.ecommerece.project.payload.CategoryDTO;
import com.ecommerece.project.payload.CategoryResponse;
import com.ecommerece.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
//    private List<Category> categories=new ArrayList<>();
//    Long nextId=1L;
    private CategoryRepository categoryRepository;
    private ModelMapper modelMapper;
    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, ModelMapper modelMapper) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize,String sortBy,String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending():
                Sort.by(sortBy).descending();
//        return categories;
        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Category> categoryPage=categoryRepository.findAll(pageDetails);
        List<Category> categories=categoryPage.getContent();
        if(categories.isEmpty()){
            throw new APIException("no categories are present");
        }
//        .map category is mapped to category DTO class
        List<CategoryDTO> categoryDTOS=categories.stream().
                map(category -> modelMapper.map(category, CategoryDTO.class)).
                toList();
        CategoryResponse categoryResponse=new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements( categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    } 

    @Override
    public CategoryDTO addCategory(CategoryDTO categoryDTO) {
//        category.setCategoryId(nextId++);
//        categories.add(category);
//        Category savedCategory=categoryRepository.findCategoryByCategoryName(category.getCategoryName());
//        if(savedCategory!=null){
//            throw new APIException("Category with name "+category.getCategoryName()+"already exists");
//        }
//        convert dto to entity
        Category category=modelMapper.map(categoryDTO, Category.class);
        Category categoryFromDB=categoryRepository.findByCategoryName(category.getCategoryName());
        if(categoryFromDB!=null){
            throw new APIException("Category with name "+category.getCategoryName()+" already exists");
        }
        Category savedCategory= categoryRepository.save(category);
        CategoryDTO categoryDTO1=modelMapper.map(savedCategory, CategoryDTO.class);
        return categoryDTO1;
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("category","categoryId",categoryId));
            categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
//        List<Category> categories = categoryRepository.findAll();
//        Category category=categories.stream()
//                .filter(c ->c.getCategoryId().equals(categoryId))
//                .findFirst().orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, " Category Not Found"));
////        .get();
////        categories.remove(category);
//        categoryRepository.delete(category);
////        categoryRepository.deleteById(categoryId);


    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
//        List<Category> categories = categoryRepository.findAll();
//        Optional<Category> optionalCategory=categories.stream()
//                .filter(c ->c.getCategoryId().equals(categoryId)).findFirst();
//        if(optionalCategory.isPresent()){
//            Category existingCategory=optionalCategory.get();
//            existingCategory.setCategoryName(category.getCategoryName());
//            Category updatedCategory=categoryRepository.save(existingCategory);
//            return updatedCategory;
//        }
//        else{
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, " Category Not Found");
//        }

        Category savedCategory=categoryRepository.findById(categoryId).
                orElseThrow(() -> new ResourceNotFoundException("category","categoryId",categoryId));
        Category category=modelMapper.map(categoryDTO, Category.class);
        category.setCategoryId(categoryId);
        savedCategory=categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }
}
