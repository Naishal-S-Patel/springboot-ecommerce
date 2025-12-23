import { Alert, AlertTitle, Skeleton } from '@mui/material'
import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import React, { useEffect, useMemo, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import PaymentForm from './PaymentForm';
import { createStripePaymentSecret } from '../../store/actions';

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

const StripePayment = () => {
  const dispatch = useDispatch();
  const { clientSecret } = useSelector((state) => state.auth);
  const { cart } = useSelector((state) => state.carts);
  const { isLoading, errorMessage } = useSelector((state) => state.errors);
  const { user, selectedUserCheckoutAddress } = useSelector((state) => state.auth);

  const userEmailRef = useRef(user?.email);

  const calculatedTotal = useMemo(() => {
    return cart?.reduce(
      (acc, cur) => acc + Number(cur?.specialPrice) * Number(cur?.quantity),
      0
    ) || 0;
  }, [cart]);

  // Clear clientSecret when user changes or component unmounts
  useEffect(() => {
    if (userEmailRef.current && user?.email && userEmailRef.current !== user?.email) {
      // User changed, clear the old payment intent
      dispatch({ type: 'CLIENT_SECRET', payload: null });
    }
    userEmailRef.current = user?.email;

    return () => {
      // Clear clientSecret on unmount to prevent reuse
      dispatch({ type: 'CLIENT_SECRET', payload: null });
    };
  }, [user?.email, dispatch]);

  useEffect(() => {
    // Do not attempt Stripe if key missing or no items/amount
    if (!publishableKey || calculatedTotal <= 0 || !user || !selectedUserCheckoutAddress) return;
    if (!clientSecret) {
      const sendData = {
        amount: Math.round(Number(calculatedTotal) * 100),
        currency: "usd",
        email: user.email,
        name: `${user.username}`,
        address: selectedUserCheckoutAddress,
        description: `Order for ${user.email}`,
        metadata: {
          test: "1"
        }
      };
      dispatch(createStripePaymentSecret(sendData));
    }
  }, [clientSecret, calculatedTotal, dispatch, user, selectedUserCheckoutAddress]);

  if (!publishableKey) {
    return (
      <Alert severity="error" className='max-w-lg mx-auto'>
        <AlertTitle>Stripe not configured</AlertTitle>
        Missing VITE_STRIPE_PUBLISHABLE_KEY. Add it to your env and reload.
      </Alert>
    );
  }

  if (isLoading) {
    return (
      <div className='max-w-lg mx-auto'>
        <Skeleton />
      </div>
    )
  }

  if (errorMessage) {
    return (
      <Alert severity="error" className='max-w-lg mx-auto'>
        <AlertTitle>Payment Error</AlertTitle>
        {errorMessage}
      </Alert>
    );
  }

  return (
    <>
      {clientSecret && stripePromise && (
        <Elements stripe={stripePromise} options={{ clientSecret }}>
          <PaymentForm clientSecret={clientSecret} totalPrice={calculatedTotal} />
        </Elements>
      )}
    </>
  )
}

export default StripePayment