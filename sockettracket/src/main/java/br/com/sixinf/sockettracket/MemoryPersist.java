/**
 * 
 */
package br.com.sixinf.sockettracket;

import java.util.HashMap;
import java.util.Map;

/**
 * @author maicon
 *
 */
public class MemoryPersist {
	
	private Map<String, String> values = new HashMap<String, String>();
		
	private static MemoryPersist mem;
	
	public static MemoryPersist getInstance(){
		if (mem == null)
			mem = new MemoryPersist();
		return mem;
	}
	
	public String getValue(String key){
		return values.get(key);
	}
	
	public void put(String key, String value) {
		values.put(key, value);
	}

}
