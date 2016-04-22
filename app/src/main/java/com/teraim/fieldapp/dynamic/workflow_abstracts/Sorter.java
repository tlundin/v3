package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.List;

public interface Sorter {
	
	public List<? extends Listable> sort(List<? extends Listable> list);
}
