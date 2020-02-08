package alt.orderline.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MALTOrderLine extends X_C_ALT_OrderLine{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8542185746965314059L;

	public MALTOrderLine(Properties ctx, int C_ALT_OrderLine_ID, String trxName) {
		super(ctx, C_ALT_OrderLine_ID, trxName); 
	}
	public MALTOrderLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName); 
	}
	
}
