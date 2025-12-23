package com.ecommerece.project.service;

import com.ecommerece.project.exceptions.APIException;
import com.ecommerece.project.exceptions.ResourceNotFoundException;
import com.ecommerece.project.model.Cart;
import com.ecommerece.project.model.Category;
import com.ecommerece.project.model.Product;
import com.ecommerece.project.model.User;
import com.ecommerece.project.payload.CartDTO;
import com.ecommerece.project.payload.ProductDTO;
import com.ecommerece.project.payload.ProductResponse;
import com.ecommerece.project.repositories.CartRepository;
import com.ecommerece.project.repositories.CategoryRepository;
import com.ecommerece.project.repositories.ProductRepository;
import com.ecommerece.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements  ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ModelMapper modelMapper;

    @Autowired
   private CartRepository cartRepository;

    @Autowired
     private CartService cartService;

    @Autowired
    private FileService fileService;

    @Value("${project.images}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository, ModelMapper modelMapper ){
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
    }
    @Override
    public ProductDTO saveProduct(ProductDTO productDTO, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).
                orElseThrow(() -> new ResourceNotFoundException("category", "categoryId", categoryId));
        Product product = modelMapper.map(productDTO, Product.class);
//        Product productFromDb=productRepository.findByProductName((product.getProductName());
        boolean ifProductNotPresent = true;
        List<Product> products = new ArrayList<>(category.getProducts());
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getProductName().equals(productDTO.getProductName())) {
                ifProductNotPresent = false;
                break;
            }
        }
        if (ifProductNotPresent) {
            product.setCategory(category);
            product.setImage("default.png");
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);
            product.setUser(authUtil.loggedInUser());
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);
        }
        else{
            throw new APIException("Product already exists");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        Specification<Product> spec=Specification.allOf();
        if(keyword!=null && !keyword.isEmpty()){
            spec=spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }
      if(category!=null && !category.isEmpty()){
            spec=spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product>  pageProduct = productRepository.findAll(spec,pageDetails);

//        List<Product> products=productRepository.findAll();
        List<Product> products = pageProduct.getContent();
        if(products.isEmpty()){
            throw new APIException("no products found");
        }
        List<ProductDTO> productDTOS=products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                }).toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProduct.getNumber());
        productResponse.setPageSize(pageProduct.getSize());
        productResponse.setTotalElements(pageProduct.getTotalElements());
        productResponse.setTotalPages(pageProduct.getTotalPages());
        productResponse.setLastPage(pageProduct.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category=categoryRepository.findById(categoryId).
                orElseThrow(()-> new ResourceNotFoundException("category","categoryId",categoryId));
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product>  pageProduct = productRepository.findByCategoryOrderByPriceAsc(category,pageDetails);
        List<Product> products = pageProduct.getContent();
        if(products.isEmpty()){
            throw new APIException("no products found in "+category.getCategoryName());
        }
//        List<Product> products=productRepository.findByCategoryOrderByPriceAsc(category);
        List<ProductDTO> productDTOS=products.stream().
                map(product -> {
                         ProductDTO productDTO=   modelMapper.map(product,ProductDTO.class);
                         productDTO.setImage(constructImageUrl(product.getImage()));
                         return productDTO;
                        }
                        ).toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProduct.getNumber());
        productResponse.setPageSize(pageProduct.getSize());
        productResponse.setTotalElements(pageProduct.getTotalElements());
        productResponse.setTotalPages(pageProduct.getTotalPages());
        productResponse.setLastPage(pageProduct.isLast());
        return productResponse;
    }

    private String constructImageUrl(String imageName){
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }


    @Override
    public ProductResponse getProductsByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product>  pageProduct = productRepository.findByProductNameLikeIgnoreCase("%"+keyword+"%",pageDetails);
        List<Product> products = pageProduct.getContent();

        if(products.isEmpty()){
            throw new APIException("no products found with keyword "+keyword);
        }
//        List<Product> products=productRepository.findByProductNameLikeIgnoreCase("%"+keyword+"%");
        List<ProductDTO> productDTOS=products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                }).toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProduct.getNumber());
        productResponse.setPageSize(pageProduct.getSize());
        productResponse.setTotalElements(pageProduct.getTotalElements());
        productResponse.setTotalPages(pageProduct.getTotalPages());
        productResponse.setLastPage(pageProduct.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {

//        get the product from the db
        Product productFromDb=productRepository.findById(productId).
                orElseThrow(()-> new ResourceNotFoundException("product","productId",productId));
//        update the product with user shared information one in req body
        Product product=modelMapper.map(productDTO,Product.class);
            productFromDb.setProductName(product.getProductName());
            productFromDb.setDescription(product.getDescription());
            productFromDb.setQuantity(product.getQuantity());
            productFromDb.setPrice(product.getPrice());
            productFromDb.setDiscount(product.getDiscount());
            productFromDb.setSpecialPrice(product.getSpecialPrice());

//        save to db
        Product savedProduct=productRepository.save(productFromDb);

        List<Cart> carts=cartRepository.findCartByProductId(productId);
        List<CartDTO> cartDTOs=carts.stream().map(
                cart->{
                    CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
                    List<ProductDTO> products=cart.getCartItems().stream().map(
                            p->modelMapper.map(p.getProduct(),ProductDTO.class)
                    ).toList();
                    cartDTO.setProducts(products);
                    return cartDTO;
                }
        ).toList();
        cartDTOs.forEach(cart->cartService.updateProductInCarts(cart.getCartId(),productId));

        return modelMapper.map(savedProduct,ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProductById(Long productId) {
        Product product=productRepository.findById(productId).
                orElseThrow(()-> new ResourceNotFoundException("product","productId",productId));
        List<Cart> carts=cartRepository.findCartByProductId(productId);
        carts.forEach(cart->cartService.deleteProductFromCart(cart.getCartId(),productId));

        productRepository.delete(product);
        return modelMapper.map(product,ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
//        get product from db
        Product productFromDb=productRepository.findById(productId).
                orElseThrow(()-> new ResourceNotFoundException("product","productId",productId));
//        upload image to server
//        get the file name of uploaded image
//        String path="images/";
//        / at end means image will be root directory
        String fileName=fileService.uploadImage(path,image);
//        updating the new file name to the product
        productFromDb.setImage(fileName);
//        save updated product
        Product updatedProduct=productRepository.save(productFromDb);
//        return dto after mapping product to dto
        return modelMapper.map(updatedProduct,ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        Page<Product>  pageProduct = productRepository.findAll(pageDetails);
//        List<Product> products=productRepository.findAll();
        List<Product> products = pageProduct.getContent();
        if(products.isEmpty()){
            throw new APIException("no products found");
        }
        List<ProductDTO> productDTOS=products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                }).toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProduct.getNumber());
        productResponse.setPageSize(pageProduct.getSize());
        productResponse.setTotalElements(pageProduct.getTotalElements());
        productResponse.setTotalPages(pageProduct.getTotalPages());
        productResponse.setLastPage(pageProduct.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        User user=authUtil.loggedInUser();
        Page<Product>  pageProduct = productRepository.findByUser(user,pageDetails);
//        List<Product> products=productRepository.findAll();
        List<Product> products = pageProduct.getContent();
        if(products.isEmpty()){
            throw new APIException("no products found");
        }
        List<ProductDTO> productDTOS=products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                }).toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProduct.getNumber());
        productResponse.setPageSize(pageProduct.getSize());
        productResponse.setTotalElements(pageProduct.getTotalElements());
        productResponse.setTotalPages(pageProduct.getTotalPages());
        productResponse.setLastPage(pageProduct.isLast());
        return productResponse;
    }


}
