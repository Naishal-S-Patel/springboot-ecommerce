import React from 'react'
import { useSelector } from 'react-redux'
import { Navigate, Outlet, useLocation } from 'react-router-dom';

const PrivateRoute = ({ publicPage = false, adminOnly = false }) => {
    const { user } = useSelector((state) => state.auth);
    const isAdmin = user && user?.roles?.includes("ROLE_ADMIN");
    const isSeller = user && user?.roles.includes("ROLE_SELLER");
    const location = useLocation();

    if (publicPage) {
        return user ? <Navigate to="/" /> : <Outlet />
    }

    // Admin area guard
    if (adminOnly) {
        if (!user) return <Navigate to="/login" replace />;

        if (isSeller && !isAdmin) {
            const sellerAllowedPaths = ["/admin/orders", "/admin/products"];
            const sellerAllowed = sellerAllowedPaths.some(path => 
                location.pathname.startsWith(path)
            );
            if (!sellerAllowed) {
                return <Navigate to="/" replace />
            }
        }

        if (!isAdmin && !isSeller) {
            return <Navigate to="/" replace />;
        }

        return <Outlet />;
    }
    
    // Default: any authenticated user can access
    return user ? <Outlet /> : <Navigate to="/login" replace />;
}

export default PrivateRoute