/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class FileStatistics {
	public static class Count {
		private Library library;
		private ComponentFactory factory;
		private int flatCount;
		private int recursiveCount;
		
		private Count(ComponentFactory factory) {
			this.library = null;
			this.factory = factory;
			this.flatCount = 0;
			this.recursiveCount = 0;
		}
		
		public Library getLibrary() {
			return library;
		}
		
		public ComponentFactory getFactory() {
			return factory;
		}
		
		public int getFlatCount() {
			return flatCount;
		}
		
		public int getRecursiveCount() {
			return recursiveCount;
		}
	}
	
	public static FileStatistics compute(LogisimFile file, Circuit circuit) {
		Set<Circuit> include = new HashSet<Circuit>();
		for (AddTool tool : file.getTools()) {
			ComponentFactory factory = tool.getFactory();
			if (factory instanceof Circuit) {
				include.add((Circuit) factory);
			}
		}
		
		Map<Circuit,Map<ComponentFactory,Count>> countMap;
		countMap = new HashMap<Circuit,Map<ComponentFactory,Count>>();
		doRecursiveCount(circuit, include, countMap);
		List<Count> countList = sortCounts(countMap.get(circuit), file);
		return new FileStatistics(countList);
	}
	
	private static Map<ComponentFactory,Count> doRecursiveCount(Circuit circuit,
			Set<Circuit> include,
			Map<Circuit,Map<ComponentFactory,Count>> countMap) {
		if (countMap.containsKey(circuit)) {
			return countMap.get(circuit);
		}

		Map<ComponentFactory,Count> counts = doSimpleCount(circuit);
		countMap.put(circuit, counts);
		for (Count count : counts.values()) {
			count.recursiveCount = count.flatCount;
		}
		for (Circuit sub : include) {
			if (counts.containsKey(sub)) {
				int multiplier = counts.get(sub).flatCount;
				Map<ComponentFactory,Count> subCount;
				subCount = doRecursiveCount(sub, include, countMap);
				for (Count subcount : subCount.values()) {
					ComponentFactory subfactory = subcount.factory;
					Count supercount = counts.get(subfactory);
					if (supercount == null) {
						supercount = new Count(subfactory);
						counts.put(subfactory, supercount);
					}
					supercount.flatCount += subcount.flatCount;
					supercount.recursiveCount += multiplier * subcount.recursiveCount;
				}
			}
		}
		
		return counts;
	}
	
	private static Map<ComponentFactory,Count> doSimpleCount(Circuit circuit) {
		Map<ComponentFactory,Count> counts;
		counts = new HashMap<ComponentFactory,Count>();
		for (Component comp : circuit.getNonWires()) {
			ComponentFactory factory = comp.getFactory();
			Count count = counts.get(factory);
			if (count == null) {
				count = new Count(factory);
				counts.put(factory, count);
			}
			count.flatCount++;
		}
		return counts;
	}
	
	private static List<Count> sortCounts(Map<ComponentFactory,Count> counts,
			LogisimFile file) {
		List<Count> ret = new ArrayList<Count>();
		for (AddTool tool : file.getTools()) {
			ComponentFactory factory = tool.getFactory();
			Count count = counts.get(factory);
			if (count != null) {
				count.library = file;
				ret.add(count);
			}
		}
		for (Library lib : file.getLibraries()) {
			for (Tool tool : lib.getTools()) {
				if (tool instanceof AddTool) {
					ComponentFactory factory = ((AddTool) tool).getFactory();
					Count count = counts.get(factory);
					if (count != null) {
						count.library = lib;
						ret.add(count);
					}
				}
			}
		}
		return ret;
	}
	
	private List<Count> counts;
	
	private FileStatistics(List<Count> counts) {
		this.counts = Collections.unmodifiableList(counts);
	}
	
	public List<Count> getCounts() {
		return counts;
	}
}