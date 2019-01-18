package com.litmus7.commerce.voice.checkout.commands;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Vector;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ibm.commerce.command.CommandFactory;
import com.ibm.commerce.command.ControllerCommandImpl;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.exception.ECException;
import com.ibm.commerce.order.utils.OrderConstants;
import com.ibm.commerce.orderitems.commands.OrderItemAddCmd;
import com.litmus7.commerce.voice.checkout.constants.L7VoiceCheckoutConstants;
import com.litmus7.commerce.voice.checkout.helper.L7VoiceCheckoutHelper;

public class L7VoiceOrderItemAddCmdImpl extends ControllerCommandImpl implements L7VoiceOrderItemAddCmd {

	private static final String CLASS_NAME = L7VoiceOrderItemAddCmdImpl.class.getName();

	private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

	private String productGroup;

	private String productBrand;

	private String productColor;

	private String productSize;

	@Override
	public void setRequestProperties(TypedProperty reqProp) throws ECException {
		super.setRequestProperties(reqProp);
		productGroup = reqProp.getString(L7VoiceCheckoutConstants.PRODUCT_CATGROUP, null);
		productBrand = reqProp.getString(L7VoiceCheckoutConstants.PRODUCT_BRAND_ATTR, null);
		productColor = reqProp.getString(L7VoiceCheckoutConstants.PRODUCT_COLOR_ATTR, null);
		productSize = reqProp.getString(L7VoiceCheckoutConstants.PRODUCT_SIZE_ATTR, null);
	}

	@Override
	public void performExecute() throws ECException {
		final String methodName = "performExecute";
		BigDecimal[] attrArr = new BigDecimal[2];
		Vector itemVector = null;
		responseProperties = null == responseProperties ? new TypedProperty() : responseProperties;
		try {
			Vector attrVector = L7VoiceCheckoutHelper.getInstance().executeParameterizedQuery(
					L7VoiceCheckoutConstants.GET_DEFINING_ATTR_FOR_CATGROUP, new Object[] { productGroup });

			for (int i = 0; i < attrVector.size(); i++) {
				Vector attrData = (Vector) attrVector.elementAt(i);
				if (L7VoiceCheckoutConstants.PRODUCT_SIZE_ATTR.equalsIgnoreCase(attrData.elementAt(1).toString())) {
					attrArr[1] = (BigDecimal) attrData.get(0);
				} else if (L7VoiceCheckoutConstants.PRODUCT_COLOR_ATTR
						.equalsIgnoreCase(attrData.elementAt(1).toString())) {
					attrArr[0] = (BigDecimal) attrData.get(0);
				}
			}

			itemVector = L7VoiceCheckoutHelper.getInstance()
					.executeParameterizedQuery(L7VoiceCheckoutConstants.GET_RESOLVED_CATENTRY_FROM_ATTR, new Object[] {
							attrArr[0], StringUtils.capitalize(productColor), attrArr[1], productSize, productBrand });
		} catch (SQLException | NamingException e) {
			LOGGER.info("Exception while adding items to cart" + e.getMessage());
			responseProperties.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY,
					L7VoiceCheckoutConstants.ITEM_ADD_ERR_MSG);
		}

		if (null == itemVector || itemVector.isEmpty()) {
			responseProperties.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY,
					L7VoiceCheckoutConstants.NO_ATTR_ITEM_MSG);
		} else {
			String itemToAdd = ((Vector) itemVector.elementAt(0)).elementAt(0).toString();
			requestProperties.put(OrderConstants.EC_CATENTRY_ID, itemToAdd);
			OrderItemAddCmd orderItemAddCmd = (OrderItemAddCmd) CommandFactory
					.createCommand(OrderItemAddCmd.class.getName(), commandContext.getStoreId());
			orderItemAddCmd.setRequestProperties(requestProperties);
			orderItemAddCmd.setCommandContext(commandContext);
			orderItemAddCmd.execute();

			responseProperties.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY, L7VoiceCheckoutConstants.ITEM_ADD_SUCCESS_MSG);
		}
	}
}
