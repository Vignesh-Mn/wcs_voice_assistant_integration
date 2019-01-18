package com.litmus7.commerce.voice.checkout.commands;

import com.ibm.commerce.command.ControllerCommand;

public interface L7ExpressCheckoutCmd extends ControllerCommand {

	public static final String NAME = L7ExpressCheckoutCmd.class.getName();

	final String defaultCommandClassName = L7ExpressCheckoutCmdImpl.class.getName();

}
