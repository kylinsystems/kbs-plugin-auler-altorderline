package alt.orderline.process;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env; 
 import alt.orderline.model.MALTOrderLine;

public class AltOrderLineModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		if (MALTOrderLine.Table_Name.equals(tableName))
			return MALTOrderLine.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		if (MALTOrderLine.Table_Name.equals(tableName))
			return new MALTOrderLine(Env.getCtx(), Record_ID, trxName); 
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if (MALTOrderLine.Table_Name.equals(tableName))
			return new MALTOrderLine(Env.getCtx(), rs, trxName); 
		return null;
	}
}
