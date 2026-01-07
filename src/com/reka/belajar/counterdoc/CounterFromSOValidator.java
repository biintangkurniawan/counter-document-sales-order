package com.reka.belajar.counterdoc;

import org.compiere.model.MClient;
import org.compiere.model.MOrder;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;

public class CounterFromSOValidator implements ModelValidator {

	private int adClientId = 0; // 0 = semua client (lebih aman utk plugin)
	private static final CLogger log = CLogger.getCLogger(CounterFromSOValidator.class);

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		if (client != null) {
			adClientId = client.getAD_Client_ID();
			log.info("Initialize CounterFromSOValidator for AD_Client_ID=" + adClientId);
		} else {
			// dipanggil juga saat startup engine (kadang client null)
			log.info("Initialize CounterFromSOValidator (client = null)");
		}

		// Penting: register doc validate untuk Order
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

		// Kita hanya handle dokumen Order
		if (!(po instanceof MOrder)) {
			return null;
		}

		MOrder order = (MOrder) po;

		// Fokus: hanya Sales Order (SO)
		if (!order.isSOTrx()) {
			return null;
		}

		// Trigger saat AFTER_COMPLETE
		if (timing != ModelValidator.TIMING_AFTER_COMPLETE) {
			return null;
		}

		// Kalau sudah pernah punya counter order, skip (hindari double create)
		if (order.getRef_Order_ID() > 0) {
			log.fine("Skip: SO already has Ref_Order_ID=" + order.getRef_Order_ID()
					+ " for Order=" + order.getDocumentNo());
			return null;
		}

		try {
			// Panggil core logic pembuatan counter doc via wrapper subclass
			MOrderCounterWrapper wrapper = new MOrderCounterWrapper(order.getCtx(), order.getC_Order_ID(), order.get_TrxName());
			MOrder counter = wrapper.createCounterDocPublic(); // bisa return null kalau tidak eligible/config belum lengkap

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

			// Kalau kamu mau BLOCK proses complete ketika gagal, return message.
			// Kalau mau tetap lanjut complete, return null.
			return "Gagal membuat Counter Document untuk Sales Order. Error: " + e.getMessage();
		}

		return null;
	}
}
