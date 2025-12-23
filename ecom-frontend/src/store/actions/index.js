import api from "../../api/api";

const getErrorMessage = (error, fallback = "Something went wrong") => {
    if (error?.response?.data?.message) return error.response.data.message;
    if (error?.response?.data?.error) return error.response.data.error;
    if (typeof error?.response?.data === "string") return error.response.data;
    return error?.message || fallback;
};

const mapCartDto = (cartDto = {}) => {
    const cart = (cartDto?.products || []).map((item) => ({
        productId: item.productId,
        productName: item.productName,
        image: item.image,
        description: item.description,
        quantity: item.quantity,
        price: item.price,
        discount: item.discount,
        specialPrice: item.specialPrice,
    }));

    return {
        cart,
        totalPrice: cartDto?.totalPrice || 0,
        cartId: cartDto?.cartId ?? null,
    };
};

const persistGuestCart = (cartItems) => {
    localStorage.setItem("cartItems", JSON.stringify(cartItems));
};

const refreshUserCart = async (dispatch) => {
    try {
        const { data } = await api.get("/carts/user/cart");
        const mapped = mapCartDto(data);
        dispatch({
            type: "GET_USER_CART_PRODUCTS",
            payload: mapped.cart,
            totalPrice: mapped.totalPrice,
            cartId: mapped.cartId,
        });
        return mapped;
    } catch (error) {
        dispatch({ type: "CLEAR_CART" });
        return { cart: [], totalPrice: 0, cartId: null };
    }
};

const mapPagination = (data = {}) => ({
    pageNumber: data.pageNumber ?? 0,
    pageSize: data.pageSize ?? 0,
    totalElements: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 0,
    lastPage: data.lastPage ?? false,
});

export const fetchProducts = (queryString = "") => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const url = queryString ? `/public/products?${queryString}` : "/public/products";
        const { data } = await api.get(url);

        dispatch({
            type: "FETCH_PRODUCTS",
            payload: data?.content || [],
            ...mapPagination(data),
        });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load products"),
        });
    }
};

export const dashboardProductsAction = (queryString = "", isAdmin = false) => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const prefix = isAdmin ? "/admin" : "/seller";
        const url = queryString
            ? `${prefix}/products?${queryString}`
            : `${prefix}/products`;
        const { data } = await api.get(url);

        dispatch({
            type: "FETCH_PRODUCTS",
            payload: data?.content || [],
            ...mapPagination(data),
        });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load products"),
        });
    }
};

export const fetchCategories = (queryString = "") => async (dispatch) => {
    try {
        dispatch({ type: "CATEGORY_LOADER" });
        const url = queryString ? `/public/categories?${queryString}` : "/public/categories";
        const { data } = await api.get(url);

        dispatch({
            type: "FETCH_CATEGORIES",
            payload: data?.content || [],
            ...mapPagination(data),
        });
        dispatch({ type: "CATEGORY_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load categories"),
        });
    }
};

export const addToCart = (cartItem, quantity = 1, toast) => async (dispatch, getState) => {
    const { auth: { user }, carts: { cart } } = getState();
    const safeQuantity = Number(quantity) || 1;

    if (!user) {
        const existing = cart?.find((item) => item.productId === cartItem.productId);
        const updatedCart = existing
            ? cart.map((item) =>
                item.productId === cartItem.productId
                    ? { ...item, quantity: item.quantity + safeQuantity }
                    : item
            )
            : [...(cart || []), { ...cartItem, quantity: safeQuantity }];

        dispatch({ type: "ADD_CART", payload: { ...cartItem, quantity: existing ? existing.quantity + safeQuantity : safeQuantity } });
        persistGuestCart(updatedCart);
        toast?.success("Added to cart");
        return;
    }

    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.post(`/carts/products/${cartItem.productId}/quantity/${safeQuantity}`);
        const mapped = mapCartDto(data);
        dispatch({
            type: "GET_USER_CART_PRODUCTS",
            payload: mapped.cart,
            totalPrice: mapped.totalPrice,
            cartId: mapped.cartId,
        });
        dispatch({ type: "IS_SUCCESS" });
        toast?.success("Added to cart");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to add to cart"),
        });
        toast?.error(getErrorMessage(error, "Unable to add to cart"));
    }
};

export const increaseCartQuantity = (cartItem, toast, currentQuantity = 1, setCurrentQuantity) => async (dispatch, getState) => {
    const { auth: { user }, carts: { cart } } = getState();
    const nextQuantity = Number(currentQuantity || 0) + 1;

    if (!user) {
        const updatedCart = (cart || []).map((item) =>
            item.productId === cartItem.productId
                ? { ...item, quantity: nextQuantity }
                : item
        );
        dispatch({ type: "ADD_CART", payload: { ...cartItem, quantity: nextQuantity } });
        persistGuestCart(updatedCart);
        setCurrentQuantity?.(nextQuantity);
        return;
    }

    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.put(`/cart/products/${cartItem.productId}/quantity/add`);
        const mapped = mapCartDto(data);
        dispatch({
            type: "GET_USER_CART_PRODUCTS",
            payload: mapped.cart,
            totalPrice: mapped.totalPrice,
            cartId: mapped.cartId,
        });
        dispatch({ type: "IS_SUCCESS" });
        setCurrentQuantity?.(nextQuantity);
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to update quantity"),
        });
        toast?.error(getErrorMessage(error, "Unable to update quantity"));
    }
};

export const decreaseCartQuantity = (cartItem, newQuantity) => async (dispatch, getState) => {
    const { auth: { user }, carts: { cart } } = getState();
    const safeQuantity = Number(newQuantity || 0);

    if (!user) {
        const updatedCart = safeQuantity <= 0
            ? (cart || []).filter((item) => item.productId !== cartItem.productId)
            : (cart || []).map((item) =>
                item.productId === cartItem.productId
                    ? { ...item, quantity: safeQuantity }
                    : item
            );

        if (safeQuantity <= 0) {
            dispatch({ type: "REMOVE_CART", payload: { productId: cartItem.productId } });
        } else {
            dispatch({ type: "ADD_CART", payload: { ...cartItem, quantity: safeQuantity } });
        }
        persistGuestCart(updatedCart);
        return;
    }

    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.put(`/cart/products/${cartItem.productId}/quantity/delete`);
        const mapped = mapCartDto(data);
        dispatch({
            type: "GET_USER_CART_PRODUCTS",
            payload: mapped.cart,
            totalPrice: mapped.totalPrice,
            cartId: mapped.cartId,
        });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to update quantity"),
        });
    }
};

export const removeFromCart = (cartItem, toast) => async (dispatch, getState) => {
    const { auth: { user }, carts: { cart, cartId } } = getState();

    if (!user) {
        const updatedCart = (cart || []).filter((item) => item.productId !== cartItem.productId);
        dispatch({ type: "REMOVE_CART", payload: { productId: cartItem.productId } });
        persistGuestCart(updatedCart);
        toast?.success("Removed from cart");
        return;
    }

    try {
        dispatch({ type: "IS_FETCHING" });
        const ensuredCartId = cartId || (await refreshUserCart(dispatch)).cartId;
        if (!ensuredCartId) {
            throw new Error("Cart not found for user");
        }
        await api.delete(`/carts/${ensuredCartId}/product/${cartItem.productId}`);
        await refreshUserCart(dispatch);
        dispatch({ type: "IS_SUCCESS" });
        toast?.success("Removed from cart");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to remove item"),
        });
        toast?.error(getErrorMessage(error, "Unable to remove item"));
    }
};

export const createUserCart = (cartItems = []) => async (dispatch, getState) => {
    const { auth: { user } } = getState();
    if (!user || !cartItems?.length) return;

    try {
        dispatch({ type: "IS_FETCHING" });
        await api.post("/carts/create", cartItems);
        await refreshUserCart(dispatch);
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to sync cart"),
        });
    }
};

export const addPaymentMethod = (method) => (dispatch) => {
    localStorage.setItem("PAYMENT_METHOD", method);
    dispatch({ type: "ADD_PAYMENT_METHOD", payload: method });
};

export const createStripePaymentSecret = (payload) => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.post("/order/stripe-client-secret", payload);
        dispatch({ type: "CLIENT_SECRET", payload: data });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to initialize payment"),
        });
    }
};

export const stripePaymentConfirmation = (payload, setErrorMessage, setLoading, toast) => async (dispatch, getState) => {
    const { payment: { paymentMethod } } = getState();
    const method = paymentMethod || localStorage.getItem("PAYMENT_METHOD") || "Stripe";

    try {
        setLoading?.(true);
        await api.post(`/order/users/payments/${method}`, { ...payload, paymentMethod: method });
        dispatch({ type: "CLEAR_CART" });
        dispatch({ type: "REMOVE_CHECKOUT_ADDRESS" });
        dispatch({ type: "CLIENT_SECRET", payload: null });
        localStorage.removeItem("cartItems");
        localStorage.removeItem("CHECKOUT_ADDRESS");
        localStorage.removeItem("PAYMENT_METHOD");
        toast?.success("Order placed successfully");
    } catch (error) {
        const message = getErrorMessage(error, "Payment confirmation failed");
        setErrorMessage?.(message);
        toast?.error(message);
    } finally {
        setLoading?.(false);
    }
};

export const getUserAddresses = () => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.get("/users/addresses");
        dispatch({ type: "USER_ADDRESS", payload: data });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load addresses"),
        });
    }
};

export const selectUserCheckoutAddress = (address) => (dispatch) => {
    if (address) {
        localStorage.setItem("CHECKOUT_ADDRESS", JSON.stringify(address));
    }
    dispatch({ type: "SELECT_CHECKOUT_ADDRESS", payload: address });
};

export const deleteUserAddress = (toast, addressId, setOpenDeleteModal) => async (dispatch, getState) => {
    if (!addressId) return;
    try {
        dispatch({ type: "BUTTON_LOADER" });
        await api.delete(`/addresses/${addressId}`);
        const { auth: { address } } = getState();
        const updated = (address || []).filter((item) => item.addressId !== addressId);
        dispatch({ type: "USER_ADDRESS", payload: updated });
        dispatch({ type: "IS_SUCCESS" });
        setOpenDeleteModal?.(false);
        toast?.success("Address deleted");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to delete address"),
        });
        toast?.error(getErrorMessage(error, "Unable to delete address"));
    }
};

export const addUpdateUserAddress = (data, toast, addressId, setOpenAddressModal) => async (dispatch) => {
    try {
        dispatch({ type: "BUTTON_LOADER" });
        const endpoint = addressId ? `/addresses/${addressId}` : "/addresses";
        const method = addressId ? api.put : api.post;
        const response = await method(endpoint, data);
        dispatch({ type: "IS_SUCCESS" });
        setOpenAddressModal?.(false);
        toast?.success(addressId ? "Address updated" : "Address added");
        dispatch(getUserAddresses());
        return response;
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to save address"),
        });
        toast?.error(getErrorMessage(error, "Unable to save address"));
    }
};

export const registerNewUser = (payload, toast, reset, navigate, setLoader) => async (dispatch) => {
    try {
        setLoader?.(true);
        await api.post("/auth/signup", { ...payload, role: ["user"] });
        toast?.success("Registration successful");
        reset?.();
        navigate?.("/login");
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to register"));
    } finally {
        setLoader?.(false);
    }
};

export const authenticateSignInUser = (payload, toast, reset, navigate, setLoader) => async (dispatch) => {
    try {
        setLoader?.(true);
        const { data } = await api.post("/auth/signin", payload);
        dispatch({ type: "LOGIN_USER", payload: data });
        localStorage.setItem("auth", JSON.stringify(data));

        const guestCart = localStorage.getItem("cartItems")
            ? JSON.parse(localStorage.getItem("cartItems"))
            : [];
        if (guestCart.length) {
            const cartItems = guestCart.map((item) => ({
                productId: item.productId,
                quantity: item.quantity,
            }));
            try {
                await api.post("/carts/create", cartItems);
                localStorage.removeItem("cartItems");
            } catch (cartError) {
                console.error("Unable to sync guest cart", cartError);
            }
        }

        await refreshUserCart(dispatch);
        toast?.success("Login successful");
        reset?.();
        navigate?.("/");
    } catch (error) {
        const message = getErrorMessage(error, "Unable to login");
        toast?.error(message);
    } finally {
        setLoader?.(false);
    }
};

export const logOutUser = (navigate) => async (dispatch) => {
    try {
        await api.post("/auth/signout");
    } catch (error) {
        // ignore logout failure
    }
    localStorage.removeItem("auth");
    localStorage.removeItem("cartItems");
    localStorage.removeItem("CHECKOUT_ADDRESS");
    localStorage.removeItem("PAYMENT_METHOD");
    dispatch({ type: "LOG_OUT" });
    dispatch({ type: "CLEAR_CART" });
    navigate?.("/");
};

export const addNewDashboardSeller = (payload, toast, reset, setOpen, setLoader) => async (dispatch) => {
    try {
        setLoader?.(true);
        await api.post("/auth/signup", { ...payload, role: ["seller"] });
        toast?.success("Seller added successfully");
        reset?.();
        setOpen?.(false);
        dispatch(getAllSellersDashboard());
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to add seller"));
    } finally {
        setLoader?.(false);
    }
};

export const getAllSellersDashboard = (queryString = "") => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const url = queryString ? `/auth/sellers?${queryString}` : "/auth/sellers";
        const { data } = await api.get(url);
        dispatch({
            type: "GET_SELLERS",
            payload: data?.content || [],
            ...mapPagination(data),
        });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load sellers"),
        });
    }
};

export const analyticsAction = () => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const { data } = await api.get("/admin/app/analytics");
        dispatch({ type: "FETCH_ANALYTICS", payload: data });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load analytics"),
        });
    }
};

export const getOrdersForDashboard = (queryString = "", isAdmin = false) => async (dispatch) => {
    try {
        dispatch({ type: "IS_FETCHING" });
        const prefix = isAdmin ? "/admin" : "/seller";
        const url = queryString ? `${prefix}/orders?${queryString}` : `${prefix}/orders`;
        const { data } = await api.get(url);
        dispatch({
            type: "GET_ADMIN_ORDERS",
            payload: data?.content || [],
            ...mapPagination(data),
        });
        dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to load orders"),
        });
    }
};

export const updateOrderStatusFromDashboard = (orderId, status, toast, setLoader, isAdmin = false) => async (dispatch, getState) => {
    if (!orderId) return;
    try {
        setLoader?.(true);
        const prefix = isAdmin ? "/admin" : "/seller";
        await api.put(`${prefix}/orders/${orderId}/status`, { status });

        const { order: { adminOrder, pagination } } = getState();
        const updatedOrders = (adminOrder || []).map((item) =>
            item.orderId === orderId ? { ...item, orderStatus: status } : item
        );
        dispatch({
            type: "GET_ADMIN_ORDERS",
            payload: updatedOrders,
            ...pagination,
        });
        toast?.success("Order status updated");
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to update order"));
    } finally {
        setLoader?.(false);
    }
};

export const addNewProductFromDashboard = (payload, toast, reset, setLoader, setOpen, isAdmin = false) => async (dispatch, getState) => {
    try {
        setLoader?.(true);
        const prefix = isAdmin ? "/admin" : "/seller";
        const { data } = await api.post(`${prefix}/categories/${payload.categoryId}/product`, payload);
        const { products: { products, pagination } } = getState();
        dispatch({
            type: "FETCH_PRODUCTS",
            payload: [data, ...(products || [])],
            ...pagination,
        });
        toast?.success("Product added successfully");
        reset?.();
        setOpen?.(false);
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to add product"));
    } finally {
        setLoader?.(false);
    }
};

export const updateProductFromDashboard = (payload, toast, reset, setLoader, setOpen, isAdmin = false) => async (dispatch, getState) => {
    try {
        setLoader?.(true);
        const prefix = isAdmin ? "/admin" : "/seller";
        const { data } = await api.put(`${prefix}/products/${payload.id}`, payload);
        const { products: { products, pagination } } = getState();
        const updated = (products || []).map((item) =>
            item.productId === data.productId ? data : item
        );
        dispatch({
            type: "FETCH_PRODUCTS",
            payload: updated,
            ...pagination,
        });
        toast?.success("Product updated successfully");
        reset?.();
        setOpen?.(false);
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to update product"));
    } finally {
        setLoader?.(false);
    }
};

export const updateProductImageFromDashboard = (formData, productId, toast, setLoader, setOpen, isAdmin = false) => async (dispatch, getState) => {
    if (!productId) return;
    try {
        setLoader?.(true);
        const prefix = isAdmin ? "/admin" : "/seller";
        const { data } = await api.put(`${prefix}/products/${productId}/image`, formData, {
            headers: { "Content-Type": "multipart/form-data" },
        });
        const { products: { products, pagination } } = getState();
        const updated = (products || []).map((item) =>
            item.productId === data.productId ? data : item
        );
        dispatch({
            type: "FETCH_PRODUCTS",
            payload: updated,
            ...pagination,
        });
        toast?.success("Image updated successfully");
        setOpen?.(false);
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to update image"));
    } finally {
        setLoader?.(false);
    }
};

export const deleteProduct = (setLoader, productId, toast, setOpenDeleteModal, isAdmin = false) => async (dispatch, getState) => {
    if (!productId) return;
    try {
        setLoader?.(true);
        const prefix = isAdmin ? "/admin" : "/seller";
        await api.delete(`${prefix}/products/${productId}`);
        const { products: { products, pagination } } = getState();
        const filtered = (products || []).filter((item) => item.productId !== productId);
        dispatch({
            type: "FETCH_PRODUCTS",
            payload: filtered,
            ...pagination,
        });
        toast?.success("Product deleted successfully");
        setOpenDeleteModal?.(false);
    } catch (error) {
        toast?.error(getErrorMessage(error, "Unable to delete product"));
    } finally {
        setLoader?.(false);
    }
};

export const deleteCategoryDashboardAction = (setOpenDeleteModal, categoryId, toast) => async (dispatch, getState) => {
    if (!categoryId) return;
    try {
        dispatch({ type: "CATEGORY_LOADER" });
        await api.delete(`/admin/categories/${categoryId}`);
        const { products: { categories, pagination } } = getState();
        const filtered = (categories || []).filter((item) => item.categoryId !== categoryId && item.id !== categoryId);
        dispatch({
            type: "FETCH_CATEGORIES",
            payload: filtered,
            ...pagination,
        });
        dispatch({ type: "CATEGORY_SUCCESS" });
        setOpenDeleteModal?.(false);
        toast?.success("Category deleted successfully");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to delete category"),
        });
        toast?.error(getErrorMessage(error, "Unable to delete category"));
    }
};

export const createCategoryDashboardAction = (payload, setOpen, reset, toast) => async (dispatch, getState) => {
    try {
        dispatch({ type: "CATEGORY_LOADER" });
        const { data } = await api.post("/admin/categories", payload);
        const { products: { categories, pagination } } = getState();
        dispatch({
            type: "FETCH_CATEGORIES",
            payload: [data, ...(categories || [])],
            ...pagination,
        });
        dispatch({ type: "CATEGORY_SUCCESS" });
        reset?.();
        setOpen?.(false);
        toast?.success("Category created successfully");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to create category"),
        });
        toast?.error(getErrorMessage(error, "Unable to create category"));
    }
};

export const updateCategoryDashboardAction = (payload, setOpen, categoryId, reset, toast) => async (dispatch, getState) => {
    if (!categoryId) return;
    try {
        dispatch({ type: "CATEGORY_LOADER" });
        const { data } = await api.put(`/admin/categories/${categoryId}`, payload);
        const { products: { categories, pagination } } = getState();
        const updated = (categories || []).map((item) =>
            item.categoryId === data.categoryId || item.id === data.categoryId ? data : item
        );
        dispatch({
            type: "FETCH_CATEGORIES",
            payload: updated,
            ...pagination,
        });
        dispatch({ type: "CATEGORY_SUCCESS" });
        reset?.();
        setOpen?.(false);
        toast?.success("Category updated successfully");
    } catch (error) {
        dispatch({
            type: "IS_ERROR",
            payload: getErrorMessage(error, "Unable to update category"),
        });
        toast?.error(getErrorMessage(error, "Unable to update category"));
    }
};
