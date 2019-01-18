package com.litmus7.commerce.voice.checkout.commands;

import com.ibm.commerce.command.ControllerCommand;

public interface L7VoiceOrderItemAddCmd extends ControllerCommand {
	
	public static final String NAME = L7VoiceOrderItemAddCmd.class.getName();
	
	final String defaultCommandClassName = L7VoiceOrderItemAddCmdImpl.class.getName();

}
