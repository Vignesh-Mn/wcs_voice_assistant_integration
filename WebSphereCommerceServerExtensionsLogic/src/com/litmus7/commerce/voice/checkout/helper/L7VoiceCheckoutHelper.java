package com.litmus7.commerce.voice.checkout.helper;

import java.sql.SQLException;
import java.util.Vector;

import javax.naming.NamingException;

import com.ibm.commerce.base.objects.ServerJDBCHelperBean;

public class L7VoiceCheckoutHelper {
	
	private static final L7VoiceCheckoutHelper l7VoiceCheckoutHelper = new L7VoiceCheckoutHelper();
	
	private L7VoiceCheckoutHelper() {
		
	}
	
	public static L7VoiceCheckoutHelper getInstance() {
		return l7VoiceCheckoutHelper;
	}
	
	public Vector executeParameterizedQuery(final String sqlString, final Object[] params) throws SQLException, NamingException {
		ServerJDBCHelperBean jdbcHelperBean = new ServerJDBCHelperBean();
		return jdbcHelperBean.executeParameterizedQuery(sqlString, params);
	}

}
