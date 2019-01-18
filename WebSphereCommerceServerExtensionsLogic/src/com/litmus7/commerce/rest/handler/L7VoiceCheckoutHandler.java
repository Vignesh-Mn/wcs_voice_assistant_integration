package com.litmus7.commerce.rest.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.wink.common.http.HttpStatus;

import com.ibm.commerce.browseradapter.HttpSessionContext;
import com.ibm.commerce.catalog.objects.CatalogEntryDescriptionAccessBean;
import com.ibm.commerce.component.contextservice.ActivityGUID;
import com.ibm.commerce.component.contextservice.ActivityToken;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.ejb.helpers.ECConstants;
import com.ibm.commerce.exception.ECException;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.foundation.rest.util.CommerceTokenHelper;
import com.ibm.commerce.member.facade.client.MemberFacadeClient;
import com.ibm.commerce.member.facade.client.PersonException;
import com.ibm.commerce.member.helpers.WCHttpClientHelper;
import com.ibm.commerce.member.helpers.WCHttpClientHelper.HttpInvocationResult;
import com.ibm.commerce.order.objects.OrderAccessBean;
import com.ibm.commerce.order.objects.OrderItemAccessBean;
import com.ibm.commerce.order.utils.OrderConstants;
import com.ibm.commerce.rest.classic.core.AbstractConfigBasedClassicHandler;
import com.ibm.commerce.rest.utils.GenericUtils;
import com.litmus7.commerce.voice.checkout.commands.L7ExpressCheckoutCmd;
import com.litmus7.commerce.voice.checkout.commands.L7VoiceOrderItemAddCmd;
import com.litmus7.commerce.voice.checkout.constants.L7VoiceCheckoutConstants;
import com.litmus7.commerce.voice.checkout.utils.L7VoiceCheckoutUtils;

/**
 * Custom REST handler to process express checkout flow.
 * @author litmus7
 */
@Path("store/{storeId}/voice_checkout")
public class L7VoiceCheckoutHandler extends AbstractConfigBasedClassicHandler {

	private static final String CLASSNAME = L7VoiceCheckoutHandler.class.getName();
	
	private static final Logger LOGGER = LoggingHelper.getLogger(L7VoiceCheckoutHandler.class);

	private static final String RESOURCE_NAME = "voice_checkout";

	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * Calls L7ExpressCheckoutCmd to proceed with express checkout flow
	 * @param storeId
	 * @return response.
	 */
	@POST
	@Path("express_checkout")
	@Produces({ APPLICATION_JSON, APPLICATION_XML, APPLICATION_XHTML_XML, APPLICATION_ATOM_XML })
	public Response doExpressCheckout(@PathParam(PARAMETER_STORE_ID) String storeId,
			@QueryParam(value = PARAMETER_RESPONSE_FORMAT) String responseFormat) {
		final String methodName = "doExpressCheckout";
		Response response = null;
		try {
			Map<String, Object> inputData = this.getMapFromRequest(request, responseFormat);
			Long userId = validateUserTokenAndCreateUserSession(storeId, inputData);
			if (!ECConstants.EC_GENERIC_USER_REFNUM.equals(userId.toString())) {
				String orderId = L7VoiceCheckoutUtils.getCurrentOrderIdForUser(userId, Integer.parseInt(storeId),
						inputData);
				inputData.put(OrderConstants.EC_ORDER_ID, orderId);
				response = StringUtils.isNotEmpty(orderId)
						? executeCommandFlow(storeId, responseFormat, inputData, L7ExpressCheckoutCmd.class.getName())
						: handleExceptionScenarios(responseFormat,
								"User account has an empty cart. Kindly add some products before proceeding");
			} else {
				LOGGER.info("User Authentication failed");
				response = handleExceptionScenarios(responseFormat,
						"Problem with authenticating the user. Please try after some time");
			}
		} catch (Exception e) {
			LOGGER.info("Exception caught in method : " + methodName + " is " + e.getMessage());
			response = handleExceptionScenarios(responseFormat,
					"Problem while doing expres checkout for the account. Please try after some time");
		}
		return response;
	}
	
	/**
	 * Resolve SKU and add item to order.
	 * @param storeId
	 * @param responseFormat
	 * @return response
	 */
	@POST
	@Path("item_add")
	@Produces({ APPLICATION_JSON, APPLICATION_XML, APPLICATION_XHTML_XML, APPLICATION_ATOM_XML })
	public Response addOrderItemWithVoice(@PathParam(PARAMETER_STORE_ID) String storeId,
			@QueryParam(value = PARAMETER_RESPONSE_FORMAT) String responseFormat) {
		final String methodName = "addOrderItemWithVoice";
		Response response = null;
		try {
			Map<String, Object> inputData = this.getMapFromRequest(request, responseFormat);
			Long userId = validateUserTokenAndCreateUserSession(storeId, inputData);
			response = (!ECConstants.EC_GENERIC_USER_REFNUM.equals(userId.toString()))
					? executeCommandFlow(storeId, responseFormat, inputData, L7VoiceOrderItemAddCmd.class.getName())
					: handleExceptionScenarios(responseFormat,
							"Problem with authenticating the user. Please try after some time");

		} catch (Exception e) {
			LOGGER.info("Exception caught in method : " + methodName + " is " + e.getMessage());
			response = handleExceptionScenarios(responseFormat,
					"Problem while doing adding item to cart. Please try after some time");
		}
		return response;
	}
	
	/**
	 * Wrapper method for OOB getCart method in CartHandler
	 * @param storeId
	 * @param responseFormat
	 * @param pageNumber
	 * @param pageSize
	 * @return cart details response
	 */
	@GET
	@Path("cart_details")
	@Produces({ APPLICATION_JSON, APPLICATION_XML, APPLICATION_XHTML_XML, APPLICATION_ATOM_XML })
	public Response getCartDetails(@PathParam(PARAMETER_STORE_ID) String storeId,
			@QueryParam(value = PARAMETER_RESPONSE_FORMAT) String responseFormat) {
		Response response = null;
		try {
			final String idToken = request.getHeader(L7VoiceCheckoutConstants.GOOGLE_ID_TOKEN);
			Map<String, Object> inputData = new HashMap<>();
			inputData.put(L7VoiceCheckoutConstants.GOOGLE_ID_TOKEN, idToken);
			Long userId = validateUserTokenAndCreateUserSession(storeId, inputData);
			OrderAccessBean orderAccessBean = L7VoiceCheckoutUtils.getOrderAccessBean(userId, Integer.valueOf(storeId));
			if (null != orderAccessBean) {
				StringBuilder reponseBuilder = new StringBuilder();
				int count = 1;
				for (OrderItemAccessBean orderItemAccBean : orderAccessBean.getOrderItems()) {
					CatalogEntryDescriptionAccessBean catentDescAccBean = new CatalogEntryDescriptionAccessBean();
					catentDescAccBean
							.setInitKey_catalogEntryReferenceNumber(orderItemAccBean.getCatalogEntryIdInEntityType());
					catentDescAccBean.setInitKey_language_id(-1);
					catentDescAccBean.instantiateEntity();
					reponseBuilder.append(count == 1
							? System.lineSeparator()
									+ (count++ + ") " + catentDescAccBean.getName() + System.lineSeparator())
							: (count++ + ") " + catentDescAccBean.getName()) + System.lineSeparator());
				}
				TypedProperty responseData = new TypedProperty();
				responseData.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY,
						L7VoiceCheckoutConstants.GET_CART_SUCCESS_MSG.replace("%orderDetails%",
								reponseBuilder.toString()));
				response = this.generateResponseFromHttpStatusCodeAndRespData(responseFormat, responseData,
						HttpStatus.OK);
			} else {
				response = handleExceptionScenarios(responseFormat,
						"No items to retrieve.User Account has an empty cart");
			}

		} catch (Exception e) {
			response = handleExceptionScenarios(responseFormat,
					"Exception while retrieving user order details. Please try later");
		}
		return response;
	}
		
	/**
	 * Establish user session within WCS from user logonId and password from request
	 * @param userInfo
	 * @throws PersonException
	 * @throws ECException 
	 */
	private Long createUserSession(final String storeId, Map<String, Object> userInfo)
			throws PersonException, ECException {
		MemberFacadeClient memberClient = new MemberFacadeClient(this.businessContext,
				this.activityTokenCallbackHandler);
		Map authInfo = L7VoiceCheckoutUtils.isNewUser(userInfo)
				? L7VoiceCheckoutUtils.ceateNewUser(storeId, userInfo) : userInfo;
		Map<String, String[]> userAuthInfo = GenericUtils.createMapOfStringArrayFromObjectMap(authInfo);
		authInfo = memberClient.authenticatePassword(userAuthInfo);
		Map commerceTokens = CommerceTokenHelper.generateCommerceTokens(authInfo);
		HttpSessionContext sessionContext = this.sessionInformation == null ? null
				: this.sessionInformation.getSessionContext();
		String[] ids = (String[]) authInfo.get("identityTokenID");
		String[] signatures = (String[]) authInfo.get("identityTokenSignature");
		String[] userIds = (String[]) authInfo.get("userId");
		ActivityToken activityToken = new ActivityToken(new ActivityGUID(new Long(ids[0])), signatures[0]);
		this.sessionInformation.setActivityToken(activityToken);
		return Long.valueOf(userIds[0]);
	}
	
	/**
	 * Handle success scenarios for express checkout
	 * @param storeId
	 * @param responseFormat
	 * @param inputData
	 * @param orderId
	 * @return Response
	 */
	private Response executeCommandFlow(final String storeId, final String responseFormat, Map<String, Object> inputData,
			final String cmdClassName) {
		Response result = null;
		TypedProperty requestProperties = new TypedProperty();
		requestProperties.putAll(inputData);
		result = this.executeControllerCommandWithContext(storeId, cmdClassName,
				requestProperties, responseFormat);
		return result;
	}
	
	/**
	 * Handle exception scenarios for express checkout.
	 * @param responseFormat
	 * @return Response
	 */
	private Response handleExceptionScenarios(final String responseFormat, final String message) {
		TypedProperty responseData = new TypedProperty();
		responseData.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY,message);
		return this.generateResponseFromHttpStatusCodeAndRespData(responseFormat, responseData, HttpStatus.OK);
	}
	
	/**
	 * Validate token obtained from google and create user session in WCS
	 * @param storeId
	 * @param inputData
	 * @return userId
	 * @throws ECException
	 * @throws PersonException
	 */
	private Long validateUserTokenAndCreateUserSession(final String storeId, Map<String, Object> inputData)
			throws ECException, PersonException {
		Long userId = Long.valueOf(ECConstants.EC_GENERIC_USER_REFNUM);
		boolean isValidToken = false;
		final String token = inputData.get("idToken").toString();
		HttpInvocationResult result = WCHttpClientHelper.execute(
				L7VoiceCheckoutConstants.GOOGLE_TOKEN_VRIFICATION_ENDPOINT.concat(token), "get", (Map) null,
				(Map) null);
		Map<String, Object> responseBody = result.getResponseBody();
		if (result.getStatusCode() == 200 && responseBody.get("error") == null) {
			if (L7VoiceCheckoutConstants.GOOGLE_AUTH_PROVIDER_URL.equals(responseBody.get("iss").toString())
					&& Boolean.valueOf(responseBody.get("email_verified").toString())) {
				inputData.put("logonId", responseBody.get("email").toString());
				inputData.put("oauthFlag", "true");
				userId = createUserSession(storeId, inputData);
			}
		}
		return userId;
	}
}

