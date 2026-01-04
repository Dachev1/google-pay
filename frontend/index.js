/**
 * Google Pay API Configuration for TEST Environment
 * This uses the 'example' gateway which returns test payment tokens
 */

// Define the Google Pay API version
const baseRequest = {
  apiVersion: 2,
  apiVersionMinor: 0
};

// Supported card networks
const allowedCardNetworks = ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"];

// Authentication methods
const allowedCardAuthMethods = ["PAN_ONLY", "CRYPTOGRAM_3DS"];

// Tokenization specification for payment gateway
const tokenizationSpecification = {
  type: 'PAYMENT_GATEWAY',
  parameters: {
    'gateway': 'example',  // 'example' is valid for TEST environment
    'gatewayMerchantId': 'exampleGatewayMerchantId'
  }
};

// Base card payment method
const baseCardPaymentMethod = {
  type: 'CARD',
  parameters: {
    allowedAuthMethods: allowedCardAuthMethods,
    allowedCardNetworks: allowedCardNetworks
  }
};

// Card payment method with tokenization
const cardPaymentMethod = Object.assign(
  {},
  baseCardPaymentMethod,
  {
    tokenizationSpecification: tokenizationSpecification
  }
);

// Payments client instance
let paymentsClient = null;

/**
 * Get or initialize the PaymentsClient
 */
function getGooglePaymentsClient() {
  if (paymentsClient === null) {
    paymentsClient = new google.payments.api.PaymentsClient({
      environment: 'TEST'  // Use 'TEST' for testing, 'PRODUCTION' for live
    });
  }
  return paymentsClient;
}

/**
 * Check if Google Pay is ready
 */
function getGoogleIsReadyToPayRequest() {
  return Object.assign({}, baseRequest, {
    allowedPaymentMethods: [baseCardPaymentMethod]
  });
}

/**
 * Get payment data request configuration
 */
function getGooglePaymentDataRequest() {
  const paymentDataRequest = Object.assign({}, baseRequest);
  paymentDataRequest.allowedPaymentMethods = [cardPaymentMethod];
  paymentDataRequest.transactionInfo = getGoogleTransactionInfo();
  paymentDataRequest.merchantInfo = {
    merchantName: 'Example Test Merchant'
    // merchantId not required for TEST environment
  };
  return paymentDataRequest;
}

/**
 * Get transaction information
 */
function getGoogleTransactionInfo() {
  return {
    countryCode: 'US',
    currencyCode: 'USD',
    totalPriceStatus: 'FINAL',
    totalPrice: '100.00'
  };
}

/**
 * Called when Google Pay JavaScript library is loaded
 */
function onGooglePayLoaded() {
  const paymentsClient = getGooglePaymentsClient();
  
  paymentsClient.isReadyToPay(getGoogleIsReadyToPayRequest())
    .then(function(response) {
      if (response.result) {
        addGooglePayButton();
      }
    })
    .catch(function(err) {
      console.error('Error checking Google Pay availability:', err);
    });
}

/**
 * Add Google Pay button to the page
 */
function addGooglePayButton() {
  const paymentsClient = getGooglePaymentsClient();
  const button = paymentsClient.createButton({
    onClick: onGooglePaymentButtonClicked,
    allowedPaymentMethods: [baseCardPaymentMethod]
  });
  document.getElementById('google-pay-button-container').appendChild(button);
}

/**
 * Handle button click
 */
function onGooglePaymentButtonClicked() {
  const paymentDataRequest = getGooglePaymentDataRequest();
  const paymentsClient = getGooglePaymentsClient();
  
  paymentsClient.loadPaymentData(paymentDataRequest)
    .then(function(paymentData) {
      processPayment(paymentData);
    })
    .catch(function(err) {
      console.error('Error loading payment data:', err);
    });
}

/**
 * Process the payment data
 */
function processPayment(paymentData) {
  console.log('Payment Data:', paymentData);
  
  // Extract the payment token
  const paymentToken = paymentData.paymentMethodData.tokenizationData.token;
  console.log('Payment Token:', paymentToken);
  
  // In a real application, you would send this token to your backend
  // Your backend would then forward it to your payment processor
  
  // Display some payment info
  const paymentMethodData = paymentData.paymentMethodData;
  const info = paymentMethodData.info || {};
  const description = paymentMethodData.description || 'Unknown card';
  
  console.log('Card Description:', description);
  console.log('Card Network:', info.cardNetwork);
  console.log('Card Details:', info.cardDetails);
  
  // Show success message
  showMessage('Payment Successful! ✓', 'success');
}

/**
 * Show message to user
 */
function showMessage(text, type) {
  const messageEl = document.getElementById('message');
  messageEl.textContent = text;
  messageEl.className = 'show ' + type;
  
  // Hide message after 3 seconds
  setTimeout(function() {
    messageEl.className = type;
  }, 3000);
}
