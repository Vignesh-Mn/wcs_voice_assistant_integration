package com.litmus7.commerce.voice.checkout.constants;

public final class L7VoiceCheckoutConstants {

	// Get defining ATTR_ID for specific catgroups
	public static final String GET_DEFINING_ATTR_FOR_CATGROUP = "SELECT ATTR_ID,NAME FROM ATTRDESC WHERE ATTR_ID IN (SELECT DISTINCT(ATTR_ID) FROM CATENTRYATTR WHERE CATENTRY_ID IN (SELECT CATENTRY_ID FROM CATGPENREL WHERE CATGROUP_ID IN (SELECT CATGROUP_ID FROM CATGRPDESC WHERE NAME = ?)) AND USAGE = 1) AND LANGUAGE_ID = -1";

	// Get required CATENTRY_ID from available attributes.
	public static final String GET_RESOLVED_CATENTRY_FROM_ATTR = "SELECT CATENTRY_ID FROM CATENTRY WHERE CATENTRY_ID IN (SELECT CATENTRY_ID FROM CATENTRYATTR WHERE CATENTRY_ID IN (SELECT CATENTRY_ID FROM CATENTRYATTR WHERE ATTRVAL_ID IN (SELECT ATTRVAL_ID FROM ATTRVALDESC WHERE ATTR_ID = ?"
			+ " AND LANGUAGE_ID = -1 and stringvalue = ?)) AND ATTRVAL_ID IN "
			+ "(SELECT ATTRVAL_ID FROM ATTRVALDESC WHERE ATTR_ID = ? AND LANGUAGE_ID = -1 and stringvalue = ?)) AND MFNAME = ?";

	// Verify validity of user token provided by google signin
	public static final String GOOGLE_TOKEN_VRIFICATION_ENDPOINT = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=";

	public static final String GOOGLE_AUTH_PROVIDER_URL = "https://accounts.google.com";

	public static final String GOOGLE_ID_TOKEN = "idToken";

	public static final String PRODUCT_COLOR_ATTR = "color";

	public static final String PRODUCT_SIZE_ATTR = "size";

	public static final String PRODUCT_BRAND_ATTR = "brand";

	public static final String PRODUCT_CATGROUP = "product";

	public static final String API_RESPONSE_KEY = "response";

	public static final String NO_ATTR_ITEM_MSG = "Item with specified attributes is not available. Please mention different attributes";

	public static final String ITEM_ADD_ERR_MSG = "There are problems in adding items to cart. Please try after some time";

	public static final String ITEM_ADD_SUCCESS_MSG = "<speak>Item successfully added to cart. <break time = \"500ms\"/>You can now view items in cart, <break time = \"500ms\"/> Add new items to cart , <break time = \"500ms\"/>Proceed to checkout or Continue Later. <break time = \"750ms\"/> What do you want to do ?</speak>";

	public static final String EXP_CHECKOUT_SUCCESS_MSG = "<speak>Successfully placed order. <break time = \"500ms\"/>Here is your order ID <break time = \"500ms\"/> <say-as interpret-as=\"digits\">%orderId%</say-as>. <break time = \"750ms\"/> Do you want to add new items to cart or continue later ?</speak>";

	public static final String GET_CART_SUCCESS_MSG = "<speak>Here are the items in your order <break time = \"500ms\"/> %orderDetails% <break time = \"500ms\"/>What do you want to do now ?</speak>";

}
