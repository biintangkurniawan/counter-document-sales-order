package com.reka.belajar.counterdoc;

import java.util.Properties;

import org.compiere.model.MOrder;

public class MOrderCounterWrapper extends MOrder {

	private static final long serialVersionUID = 1L;

	public MOrderCounterWrapper(Properties ctx, int C_Order_ID, String trxName) {
		super(ctx, C_Order_ID, trxName);
	}

	public MOrder createCounterDocPublic() {
		return super.createCounterDoc();
	}
}
