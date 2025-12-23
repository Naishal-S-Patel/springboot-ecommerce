package com.ecommerece.project.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// here define attribute that actually u need to show on page not with db
// request object
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    @Schema(description = "Category Id for a particular Category",example = "101")
    private Long categoryId;
    @Schema(description = "Category Name ",example = "Iphone 16")
    @NotBlank
    @Size(min=5,message="category name should be at least 5 character long")
    private String categoryName;
}
