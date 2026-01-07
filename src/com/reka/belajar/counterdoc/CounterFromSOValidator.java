package com.reka.belajar.counterdoc;

import org.compiere.model.MClient;
import org.compiere.model.MOrder;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;

public class CounterFromSOValidator implements ModelValidator {

	private int adClientId = 0;
	private static final CLogger log = CLogger.getCLogger(CounterFromSOValidator.class);

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		if (client != null) {
			adClientId = client.getAD_Client_ID();
			log.info("Initialize CounterFromSOValidator for AD_Client_ID=" + adClientId);
		} else {
			log.info("Initialize CounterFromSOValidator (client = null)");
		}

		engine.addDocValidate(MOrder.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID() {
		return adClientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		return null;
	}

	@Override
	public String modelChange(PO po, int type) {
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {

		if (!(po instanceof MOrder)) {
			return null;
		}

		MOrder order = (MOrder) po;

		if (!order.isSOTrx()) {
			return null;
		}

		if (timing != ModelValidator.TIMING_AFTER_COMPLETE) {
			return null;
		}

		if (order.getRef_Order_ID() > 0) {
			log.fine("Skip: SO already has Ref_Order_ID=" + order.getRef_Order_ID()
					+ " for Order=" + order.getDocumentNo());
			return null;
		}

		try {
			MOrderCounterWrapper wrapper = new MOrderCounterWrapper(order.getCtx(), order.getC_Order_ID(), order.get_TrxName());
			MOrder counter = wrapper.createCounterDocPublic();

			if (counter != null) {
				log.info("Counter Order created. SO=" + order.getDocumentNo()
						+ " -> Counter=" + counter.getDocumentNo()
						+ " (AD_Org_ID=" + counter.getAD_Org_ID() + ")");
			} else {
				log.fine("No counter order created for SO=" + order.getDocumentNo()
						+ " (maybe BP not linked to Org / Counter DocType not configured)");
			}

		} catch (Exception e) {
			log.severe("Failed to create counter order for SO=" + order.getDocumentNo()
					+ " error=" + e.getMessage());

			return "Gagal membuat Counter Document untuk Sales Order. Error: " + e.getMessage();
		}

		return null;
	}
}
