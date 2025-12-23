package com.ecommerece.project.controller;

import com.ecommerece.project.config.AppConstants;
import com.ecommerece.project.payload.CategoryDTO;
import com.ecommerece.project.payload.CategoryResponse;
import com.ecommerece.project.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private CategoryService categoryService;
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
//    @GetMapping("/echo")
//    public ResponseEntity<String> echoMessage(@RequestParam(name="message",
//            defaultValue ="hey",required=true ) String message){
//        return new ResponseEntity<>("echoed message "+message ,HttpStatus.OK);
//    }

    @Tag(name="Category APIs",description = "API for managing categories")
    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(name="pageNumber",defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber,
            @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE,required = false) Integer pageSize,
            @RequestParam(name="sortBy",defaultValue = AppConstants.SORT_CATEGORIES_BY,required = false) String sortBy,
            @RequestParam(name="sortOrder",defaultValue = AppConstants.SORT_DIR,required = false) String sortOrder,
            Sort sort) {
        return new ResponseEntity<>(categoryService.getAllCategories(pageNumber,pageSize,sortBy,sortOrder), HttpStatus.OK);
    }

    @Tag(name="Category APIs",description = "API for managing categories")
    @Operation(summary = "create category", description = "api for creating category")
    @ApiResponses( {
            @ApiResponse(responseCode = "201",description = "category is created successfully "),
            @ApiResponse(responseCode = "400",description = "Invalid input ",content = @Content),
            @ApiResponse(responseCode = "500",description = "Internal Server Error ",content = @Content),
    })
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDTO> addCategory(@Valid @RequestBody CategoryDTO categoryDTO){
        CategoryDTO savedCategoryDTO= categoryService.addCategory(categoryDTO);
        return new ResponseEntity<>(savedCategoryDTO,HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@Parameter(description = "category that u wish to delete")
            @PathVariable Long categoryId) {
            CategoryDTO categoryDTO= categoryService.deleteCategory(categoryId);
            return new ResponseEntity<>(categoryDTO, HttpStatus.OK);
//        catch (ResponseStatusException e) {
//            return new ResponseEntity<>(e.getMessage(),e.getStatusCode());
//        }
    }

    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long categoryId,  @Valid @RequestBody CategoryDTO categoryDTO){
            CategoryDTO savedCategoryDTO=categoryService.updateCategory(categoryDTO,categoryId);
            return new ResponseEntity<>(savedCategoryDTO,HttpStatus.OK);
//        catch(ResponseStatusException e){
//            return new ResponseEntity<>(e.getMessage(),e.getStatusCode());
//        }
    }
}
