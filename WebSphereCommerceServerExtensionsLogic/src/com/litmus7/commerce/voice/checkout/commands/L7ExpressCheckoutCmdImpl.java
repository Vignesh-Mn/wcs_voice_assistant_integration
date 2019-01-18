package com.litmus7.commerce.voice.checkout.commands;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.transaction.RollbackException;

import org.apache.commons.lang3.StringUtils;

import com.ibm.commerce.command.CommandContext;
import com.ibm.commerce.command.CommandFactory;
import com.ibm.commerce.command.ControllerCommandImpl;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.edp.api.CommunicationException;
import com.ibm.commerce.edp.api.EDPPaymentInstruction;
import com.ibm.commerce.edp.api.InputException;
import com.ibm.commerce.edp.api.J2EEException;
import com.ibm.commerce.edp.commands.PIAddCmd;
import com.ibm.commerce.edp.commands.PIRemoveCmd;
import com.ibm.commerce.edp.commands.QueryPIsCmd;
import com.ibm.commerce.exception.ECException;
import com.ibm.commerce.foundation.logging.LoggingHelper;
import com.ibm.commerce.order.commands.OrderPrepareCmd;
import com.ibm.commerce.order.commands.OrderProcessCmd;
import com.ibm.commerce.order.utils.OrderConstants;
import com.ibm.commerce.server.TransactionManager;
import com.litmus7.commerce.voice.checkout.constants.L7VoiceCheckoutConstants;
import com.litmus7.commerce.voice.checkout.utils.L7VoiceCheckoutUtils;

/**
 * Custom command implementation to process with express checkout for voice
 * based integrations in checkout.
 * @author litmus7
 */
public class L7ExpressCheckoutCmdImpl extends ControllerCommandImpl implements L7ExpressCheckoutCmd {

	private static final String CLASSNAME = L7ExpressCheckoutCmdImpl.class.getName();

	private static final Logger LOGGER = LoggingHelper.getLogger(L7ExpressCheckoutCmdImpl.class);

	private String orderId = null;

	/**
	 * Override OOB setRequestProperties method to add required parameters in
	 * request.
	 */
	@Override
	public void setRequestProperties(TypedProperty reqProperties) throws ECException {
		super.setRequestProperties(reqProperties);
		orderId = reqProperties.getString(OrderConstants.EC_ORDER_ID, StringUtils.EMPTY);
		requestProperties.put("URL", "");
	}

	/**
	 * Business logic to call necessary commands to perform express checkout.
	 */
	@Override
	public void performExecute() {
		final String methodName = "performExecute";
		try {
			responseProperties = (null != responseProperties) ? responseProperties : new TypedProperty();
			
			executeOrderPrepare();

			executePIAdd();

			executeOrderProcess();

		} catch (ECException | RollbackException e) {
			LOGGER.info("Exception while doing express checkout for voice based integrations");
			responseProperties.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY, "Something went wrong while processing the order. Please try again");
			
		}
	}

	/**
	 * Executes Order processing by calling OOB OrderProcessCmd
	 * 
	 * @throws ECException
	 */
	private void executeOrderProcess() throws ECException {
		OrderProcessCmd orderProcessCmd = (OrderProcessCmd) CommandFactory
				.createCommand(OrderProcessCmd.class.getName(), commandContext.getStoreId());
		orderProcessCmd.setCommandContext(commandContext);
		orderProcessCmd.setRequestProperties(requestProperties);
		orderProcessCmd.execute();
		responseProperties.put(L7VoiceCheckoutConstants.API_RESPONSE_KEY,
				L7VoiceCheckoutConstants.EXP_CHECKOUT_SUCCESS_MSG.replace("%orderId%", orderId));
		LOGGER.info("Successfully completed Order Processing");
	}

	/**
	 * Execute OOB OrderPrepareCmd
	 * 
	 * @throws ECException
	 */
	private void executeOrderPrepare() throws ECException {
		OrderPrepareCmd orderPrepareCmd = (OrderPrepareCmd) CommandFactory
				.createCommand(OrderPrepareCmd.class.getName(), commandContext.getStoreId());
		orderPrepareCmd.setRequestProperties(requestProperties);
		orderPrepareCmd.setCommandContext(commandContext);
		orderPrepareCmd.execute();
		LOGGER.info("Successfully executed OrderPrepare : " + orderPrepareCmd.getResponseProperties());
	}

	/**
	 * Execute OOB PIAddcmd to create payment instructions for the order.
	 * 
	 * @throws ECException
	 * @throws RollbackException
	 */
	private void executePIAdd() throws ECException, RollbackException {
		checkAndRemoveExistingPIs();
		L7VoiceCheckoutUtils.createRequestPropertiesForPIAdd(requestProperties, commandContext, orderId);
		PIAddCmd piAddCmd = (PIAddCmd) CommandFactory.createCommand(PIAddCmd.class.getName(),
				commandContext.getStoreId());
		piAddCmd.setRequestProperties(requestProperties);
		piAddCmd.setCommandContext(commandContext);
		piAddCmd.execute();

		TransactionManager.commit();

		TransactionManager.begin();

		LOGGER.info("Successfully created Payment Instructions : " + piAddCmd.getResponseProperties());
	}

	/**
	 * Remove any existing PI's in order.
	 * @throws ECException
	 * @throws NumberFormatException 
	 * @throws InputException 
	 * @throws CommunicationException 
	 * @throws J2EEException 
	 */
	private void checkAndRemoveExistingPIs()
			throws ECException {
		QueryPIsCmd aQueryPIsCmd = (QueryPIsCmd) CommandFactory
				.createCommand("com.ibm.commerce.edp.commands.QueryPIsCmd", this.getStoreId());
		aQueryPIsCmd.setOrderId(Long.valueOf(orderId));
		aQueryPIsCmd.setCommandContext((CommandContext) this.getCommandContext().clone());
		aQueryPIsCmd.execute();
		ArrayList<EDPPaymentInstruction> piList = aQueryPIsCmd.getPIs();
		if (null != piList && !piList.isEmpty()) {
			Long[] piIds = piList.stream().map(k -> k.getId()).toArray(Long[]::new);
			PIRemoveCmd piRemoveCmd = (PIRemoveCmd) CommandFactory.createCommand(PIRemoveCmd.class.getName(),
					commandContext.getStoreId());
			piRemoveCmd.setCommandContext((CommandContext) this.getCommandContext().clone());
			piRemoveCmd.setRequestProperties(requestProperties);
			piRemoveCmd.setOrderId(Long.parseLong(orderId));
			piRemoveCmd.setPIIDs(piIds);
			piRemoveCmd.setURL(requestProperties.getString("URL", ""));
			piRemoveCmd.execute();
			LOGGER.info("Success removed existing payment instructions");
		}
	}
}
