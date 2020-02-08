/**
 * Licensed under the karma of sharing. As i have shared freely to you, so shall you share freely back.
 * If you shall try to cheat and find a loophole in this license, then karma will exact your share.
 * and your worldly gain shall come to naught and those who share shall inherit above you.
 */

package alt.orderline.process;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductBOM;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env; 
import org.osgi.service.event.Event;

import alt.orderline.model.MALTOrderLine;
 

/**
 * Alternative Output V1.0 for IDempiere 3.1 backward compatible to v2.0
 * OrderLines just refer to a single BOM defined in MProductBOM with HEADER# set in DocumentNote
 *  - the first orderline's bom parent will trigger the effect. No need for all other lines to be part of it
 *  - at least *HEADER* remark in Document Note of the parent BOM, or it won't take effect 
 *  - idea is at http://wiki.idempiere.org/en/Plugin:_Alternative_Output
 *  @author red1
 */
public class AltOrderLineEvent extends AbstractEventHandler{
	private static CLogger log = CLogger.getCLogger(AltOrderLineEvent.class);
	private String trxName = "";
	private PO po = null; 
	//these settings appear in Document Note of Parent.
	private boolean HEADER = false; //consolidated to parent name, no details unless ..
 	private boolean DETAIL_NO_PRICE = false;//add details no prices ELSE detail original price
	private boolean DETAIL_OWN_PRICE = false;//details with prices replaced by BOM children's description or orderline description marked with *PRICE*
	private boolean CONSOLIDATE = false; //consolidate same product to same line
	private boolean CONSOLIDATE_SAME_LINE = false; //consolidate same product, same qty to same line
	private boolean OUTPUT_SETTING;  //flag for parent header line at the end
	private MProductBOM bom = null;
	private StringBuilder HeaderString = new StringBuilder();
	private MProduct parent = null;
	private int firstAltID; 
	
	@Override
	protected void initialize() { 
	//register EventTopics and TableNames
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MOrder.Table_Name);
		log.info("<ALT-ORDERLINE> .. IS NOW INITIALIZED");
		}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		//testing that it works at login
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			LoginEventData eventData = getEventData(event);
			log.fine(" topic="+event.getTopic()+" AD_Client_ID="+eventData.getAD_Client_ID()
					+" AD_Org_ID="+eventData.getAD_Org_ID()+" AD_Role_ID="+eventData.getAD_Role_ID()
					+" AD_User_ID="+eventData.getAD_User_ID());
			}
		else if (type.equals(IEventTopics.DOC_AFTER_PREPARE))
		{
 			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			log.info(" topic="+event.getTopic()+" po="+po);
			if (po instanceof MOrder) {
				//HeaderString stores BOM Parent 'Product or Package' Name
				HeaderString = new StringBuilder(); 
				//INITIALISE all setting flags from BOM Parent
				OUTPUT_SETTING = HEADER = CONSOLIDATE = CONSOLIDATE_SAME_LINE = DETAIL_NO_PRICE = DETAIL_OWN_PRICE = false;
				MOrder order = (MOrder) po;
				
				//DELETE any old ALTOrderLines
				List<MALTOrderLine> olds = new Query(Env.getCtx(),MALTOrderLine.Table_Name,MALTOrderLine.COLUMNNAME_C_Order_ID+"=?",trxName)
				.setParameters(order.getC_Order_ID())
				.list();
				if (olds!=null){
					for (MALTOrderLine old:olds){
						old.deleteEx(false, trxName);
					}
				}
				 
				int lineno = 1; //details start at 2, 1 is reserved for header at end
				List<MOrderLine> orderlines = new Query(Env.getCtx(),MOrderLine.Table_Name,MOrder.COLUMNNAME_C_Order_ID+"=?", trxName)
				.setParameters(order.getC_Order_ID())
				.list();
				if (orderlines==null) return;
				//
				firstAltID = orderlines.get(0).get_ID();
				MALTOrderLine headerline = new MALTOrderLine(Env.getCtx(), 0, trxName);
				headerline.setLine(1);   
				headerline.setC_Order_ID(order.getC_Order_ID());
				headerline.setC_OrderLine_ID(firstAltID);
				headerline.setQtyOrdered(Env.ONE);
				headerline.saveEx(trxName);
				//
				//************************ START OF LOOP ******************
				for (MOrderLine orderline:orderlines){
					int counter = 0; //for CONSOLIDATE_SAME_LINE .. used in final price multiply
					
					//findSetting - Is it a BOM child?
					//ONE TIME SET, so not strict other siblings exist within BOM
					if (!findSetting(orderline)) //BREAK when DocumentNote with HEADER# not set
						break;
					MALTOrderLine altorderline	= null;
					
					//****************** CONSOLIDATION ***************
					if (CONSOLIDATE || CONSOLIDATE_SAME_LINE){
						// 
						MALTOrderLine checkline = new Query(Env.getCtx(),MALTOrderLine.Table_Name,MALTOrderLine.COLUMNNAME_M_Product_ID+"=?",trxName)
						.setParameters(orderline.getM_Product_ID())
						.first();
						if (checkline==null) { //no previous alt order line, do NEW
							lineno++;
							checkline = new MALTOrderLine(Env.getCtx(),0,trxName);
							checkline.setLine(lineno);
							checkline.setCounter(1);
							checkline.setM_Product_ID(orderline.getM_Product_ID());
							checkline.setName(orderline.getM_Product().getName());
							checkline.setQtyOrdered(orderline.getQtyOrdered());
						}else { //SAME LINE QTY, so just count
							if (CONSOLIDATE_SAME_LINE && checkline.getQtyOrdered().compareTo(orderline.getQtyOrdered())==0){
								counter = checkline.getCounter();
								counter++;
								checkline.setName(counter+" X "+orderline.getM_Product().getName());
								checkline.setCounter(counter);
							} else { //Not same Qty, so just add to it.
								
								BigDecimal sumup = checkline.getQtyOrdered();
								if (sumup==null)	
									sumup = Env.ZERO;
								sumup = sumup.add(orderline.getQtyOrdered());
								checkline.setQtyOrdered(sumup);
							}							
						}
						altorderline = checkline;
					//******************** NO CONSOLIDATION	*************
					} else { 
						lineno++;
						altorderline = new MALTOrderLine(Env.getCtx(),0,trxName);
						altorderline.setLine(lineno);
						altorderline.setM_Product_ID(orderline.getM_Product_ID());
						altorderline.setName(orderline.getM_Product().getName()); 
						altorderline.setQtyOrdered(orderline.getQtyOrdered());
						altorderline.setPriceEntered(Env.ZERO); //init DETAIL_NO_PRICE
					}
					//Look at other settings
					if (!DETAIL_NO_PRICE) {
  						
						if (DETAIL_OWN_PRICE) {//defined in bomproduct.description || orderline.description							
							String orderlinePrice = orderline.getDescription();
							if (orderlinePrice!=null){
								String[] orderlineSplit = orderlinePrice.split("#");
								if (orderlineSplit[0].contains("PRICE")) {
									//take from Orderline.Description PRICE# 
									if (orderlineSplit[1].matches("^[0-9]+$"))
										altorderline.setPriceEntered(new BigDecimal(orderlineSplit[1]));
 	 							} 
							} else if (bom.getDescription()!=null){
								//take from BOMProduct.
								if (bom.getDescription().matches("^[0-9]+$"))
									altorderline.setPriceEntered(new BigDecimal(bom.getDescription()));				
  							}
						} else {
							//original price
							altorderline.setPriceEntered(orderline.getPriceEntered());
 						}
					}
					//..DETAIL_NO_PRICE
					//NEW ALTORDERLINE IS SAVED
					altorderline.setC_Order_ID(order.getC_Order_ID());
					BigDecimal ctr = Env.ONE;
					if (counter>0)
						ctr = new BigDecimal(counter);
					altorderline.setLineNetAmt(altorderline.getQtyOrdered().multiply(altorderline.getPriceEntered()).multiply(ctr));
					altorderline.setC_OrderLine_ID(orderline.getC_OrderLine_ID());
					altorderline.saveEx(trxName);
				} //************************************ END OF LOOP - OrderLine
				if (OUTPUT_SETTING){
					if (HEADER){
						if (DETAIL_NO_PRICE) {
							headerline.setLineNetAmt(order.getGrandTotal());
						}
						headerline.setName(HeaderString.toString());
						headerline.saveEx(trxName);
					} 
				}
			}
		}
	}
	/**
	 * Done one time with OUTPUT_SETTING
	 * @param bom
	 * @return
	 */
	private boolean findSetting(MOrderLine orderline) { 
		if (OUTPUT_SETTING) //already set
			return true;
		MProductBOM checkbom = new Query(Env.getCtx(),MProductBOM.Table_Name,MProductBOM.COLUMNNAME_M_ProductBOM_ID+"=?",trxName)
		.setParameters(orderline.getM_Product_ID())
		.firstOnly();
		if (checkbom==null) 
			return false;//leave
		this.bom = checkbom;
		parent = new Query(Env.getCtx(),MProduct.Table_Name,MProduct.COLUMNNAME_M_Product_ID+"=?",trxName)
		.setParameters(checkbom.getM_Product_ID())
		.firstOnly();
		
		if (parent==null) 
			return false;
		String docNote = parent.getDocumentNote();
		if (docNote==null)
			return false;
		String[] array = docNote.split("#");
		if (array.length>0){
			for (String s:array){
				if (s.equals("HEADER")){
					HEADER = true;
					HeaderString.append(parent.getName());
				}					
				else if (s.equals("CONSOLIDATE"))
					CONSOLIDATE = true;
				else if (s.equals("CONSOLIDATE_SAME_LINE"))
					CONSOLIDATE_SAME_LINE = true; 
				else if (s.equals("DETAIL_NO_PRICE"))
					DETAIL_NO_PRICE = true;
				else if (s.equals("DETAIL_OWN_PRICE"))
					DETAIL_OWN_PRICE = true;
				}
			OUTPUT_SETTING = true;
			if (!HEADER){ //settings left out HEADER, so all is void.
				log.warning("*HEADER* in OUTPUT DOC EVENT MISSING - ABORTED");
				return false;
			}
			return true;
		} else {
			log.fine("OUTPUT EVENT has no Document Note setting in BOM Header (parent)");
			return false;
		}
	}

	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}
 
}
