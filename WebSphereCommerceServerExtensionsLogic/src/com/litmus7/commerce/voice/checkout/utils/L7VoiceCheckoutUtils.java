package com.litmus7.commerce.voice.checkout.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.commerce.command.CommandContext;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.edp.utils.Constants;
import com.ibm.commerce.exception.ECException;
import com.ibm.commerce.member.helpers.UserRegistrationHelper;
import com.ibm.commerce.order.calculation.CalculationHelper;
import com.ibm.commerce.order.objects.OrderAccessBean;
import com.ibm.commerce.order.utils.OrderConstants;
import com.ibm.commerce.server.ECConstants;
import com.ibm.commerce.user.objects.AddressAccessBean;
import com.ibm.commerce.user.objects.UserRegistryAccessBean;

/**
 * Utility class to perform various operations for express checkout
 * 
 * @author litmus7
 */

public class L7VoiceCheckoutUtils {

	public static String getCurrentOrderIdForUser(Long userId, int storeId, Map<String, Object> inputData) {
		String orderId = null;
		if (null != userId) {
			Enumeration<OrderAccessBean> orderEnum = new OrderAccessBean()
					.findByStatusAndMember(OrderConstants.ORDER_PENDING, userId);
			if (orderEnum.hasMoreElements()) {
				OrderAccessBean ordAcccBean = orderEnum.nextElement();
				orderId = ordAcccBean.getOrderId();
				inputData.put("orderChannel", ordAcccBean.getOrderChannelTypeId());
			}
		}
		return orderId;
	}

	public static OrderAccessBean getOrderAccessBean(String orderId) {
		OrderAccessBean orderAccessBean = new OrderAccessBean();
		orderAccessBean.setInitKey_orderId(orderId);
		orderAccessBean.instantiateEntity();
		return orderAccessBean;
	}

	public static OrderAccessBean getOrderAccessBean(final Long userId, int storeId) {
		OrderAccessBean ordAcccBean = null;
		if (null != userId) {
			Enumeration<OrderAccessBean> orderEnum = new OrderAccessBean()
					.findByStatusAndMember(OrderConstants.ORDER_PENDING, userId);
			if (orderEnum.hasMoreElements()) {
				ordAcccBean = orderEnum.nextElement();
			}
		}
		return ordAcccBean;
	}

	public static void createRequestPropertiesForPIAdd(TypedProperty requestProperties, CommandContext commandContext,
			String orderId) throws ECException {
		requestProperties.put(Constants.POLICY_ID, "11009");
		requestProperties.put("billing_address_id", getUsersDefaultAddressId(commandContext.getUserId()));
		requestProperties.put("expire_month", "12");
		requestProperties.put(ECConstants.EC_EDP_PAYMTHDID, "VISA");
		requestProperties.put("cc_brand", "VISA");
		requestProperties.put("expire_year", "2028");
		requestProperties.put(Constants.CREDIT_LINE_NUMBER, "4111111111111111");
		requestProperties.put(ECConstants.EC_EDP_PIAMOUNT,
				CalculationHelper.getInstance().getOrderTotalAmount(getOrderAccessBean(orderId), commandContext));
	}

	public static Long getUsersDefaultAddressId(Long userId) {
		Enumeration<AddressAccessBean> addressEnum = new AddressAccessBean().findPrimaryAddress("SB", userId);
		while (addressEnum.hasMoreElements()) {
			return addressEnum.nextElement().getAddressIdInEntityType();
		}
		return null;
	}

	public static AddressAccessBean getDefaultAddressAccBean(Long userId) {
		Enumeration<AddressAccessBean> addressEnum = new AddressAccessBean().findPrimaryAddress("SB", userId);
		if (addressEnum.hasMoreElements()) {
			return addressEnum.nextElement();
		}
		return null;
	}

	public static boolean isNewUser(Map requestParams) {
		String logonId = (String) requestParams.getOrDefault("logonId", null);
		UserRegistryAccessBean userRegAccBean = null;
		if (StringUtils.isNotEmpty(logonId)) {
			userRegAccBean = new UserRegistryAccessBean().findByUserLogonId(logonId);
		}
		return null == userRegAccBean;
	}

	public static Map ceateNewUser(final String storeId, Map<String, Object> userInfo) throws ECException {
		TypedProperty mappedUserInfo = new TypedProperty();
		mappedUserInfo.putAll(userInfo);
		return UserRegistrationHelper.createUser(storeId, mappedUserInfo);
	}

	public static Map convertUserDetailsToReqFormat(Map<String, Object> inputData) {
		Map userAuthenticatioInfo = new HashMap();
		userAuthenticatioInfo.put("logonId", trim(inputData.get("logonId")).split(" "));
		return userAuthenticatioInfo;
	}

	public static String trim(Object obj) {
		return null != obj ? ((String) obj).replaceAll("\\s", "") : null;
	}
}
