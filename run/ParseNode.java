package net.zerobone.knife.parser;

import java.lang.Object;
import java.util.ArrayList;

final class ParseNode {
	int actionId = 0;

	final int symbolId;

	Object payload = null;

	ArrayList<Object> children;

	ParseNode(int symbolId) {
		this.symbolId = symbolId;
		children = this.symbolId < 0 ? new ArrayList<>() : null;
	}

	void reduce() {
		Object v;
		switch (actionId - 1) {
			case 0:
				{

				}
				break;
			case 1:
				{

				}
				break;
			case 2:
				{

				}
				break;
			case 3:
				{

				}
				break;
			case 4:
				{

				}
				break;
			case 5:
				{

				}
				break;
			case 6:
				{

				}
				break;
			case 7:
				{

				}
				break;
			case 8:
				{

				}
				break;
			default:
				throw new IllegalStateException();
		}
		payload = v;
		children = null;
	}
}
